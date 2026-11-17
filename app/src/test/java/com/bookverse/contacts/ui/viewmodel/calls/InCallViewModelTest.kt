package com.bookverse.contacts.ui.viewmodel.calls

import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.service.DefaultCallRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InCallViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearRepository()
    }

    @After
    fun tearDown() {
        clearRepository()
        Dispatchers.resetMain()
    }

    private fun clearRepository() {
        DefaultCallRepository.calls.value.forEach { DefaultCallRepository.remove(it.id) }
    }

    private suspend fun awaitCollect() {
        yield()
        yield()
    }

    @Test
    fun state_callBecomesActive_startsTimer() = runBlocking {
        val callId = "test-call-1"
        val initialCall = makeCall(callId, CallState.DIALING)
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initialCall)
        awaitCollect()

        assertEquals(CallState.DIALING, vm.activeCall.value?.state)
        assertEquals(0, vm.durationSec.value)

        val activeCall = initialCall.copy(state = CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis() - 5000)
        DefaultCallRepository.upsert(callId, activeCall)
        awaitCollect()

        assertEquals(CallState.ACTIVE, vm.activeCall.value?.state)
        assertTrue(
            "durationSec debe incrementarse tras cambio a ACTIVE (actual=${vm.durationSec.value})",
            vm.durationSec.value >= 4
        )
    }

    @Test
    fun state_emitOnMuteChange_doesNotResetDuration() = runBlocking {
        val callId = "test-call-2"
        val connectTime = System.currentTimeMillis() - 10000
        val initial = makeCall(callId, CallState.ACTIVE, connectTimeMillis = connectTime)
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initial)
        awaitCollect()

        val firstDuration = vm.durationSec.value
        assertTrue("durationSec debe ser > 0 tras 10s (actual=$firstDuration)", firstDuration >= 9)

        DefaultCallRepository.setMuted(true)
        awaitCollect()

        val secondDuration = vm.durationSec.value
        assertTrue(
            "durationSec no debe resetearse al cambiar isMuted (antes=$firstDuration, después=$secondDuration)",
            secondDuration >= firstDuration
        )
    }

    @Test
    fun state_emitOnMuteChange_updatesMutedFlag() = runBlocking {
        val callId = "test-call-mute"
        val initial = makeCall(callId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis())
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initial)
        awaitCollect()

        assertEquals(false, vm.activeCall.value?.isMuted)

        DefaultCallRepository.setMuted(true)
        awaitCollect()

        assertEquals(true, vm.activeCall.value?.isMuted)
    }

    @Test
    fun state_emitOnStateChange_updatesActiveCall() = runBlocking {
        val callId = "test-call-state"
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, makeCall(callId, CallState.RINGING))
        awaitCollect()

        assertEquals(CallState.RINGING, vm.activeCall.value?.state)

        DefaultCallRepository.upsert(
            callId,
            makeCall(callId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis() - 3000)
        )
        awaitCollect()

        assertEquals(CallState.ACTIVE, vm.activeCall.value?.state)
    }

    @Test
    fun state_emitOnAudioRouteChange_doesNotResetDuration() = runBlocking {
        val callId = "test-call-3"
        val connectTime = System.currentTimeMillis() - 15000
        val initial = makeCall(callId, CallState.ACTIVE, connectTimeMillis = connectTime)
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initial)
        awaitCollect()

        val firstDuration = vm.durationSec.value
        assertTrue("durationSec debe ser >= 14 (actual=$firstDuration)", firstDuration >= 14)

        DefaultCallRepository.setAudioRoute(AudioRoute.SPEAKER)
        awaitCollect()

        val secondDuration = vm.durationSec.value
        assertTrue(secondDuration >= firstDuration)
    }

    @Test
    fun state_callRemoved_marksEndedAndClearsState() = runBlocking {
        val callId = "test-call-4"
        val initial = makeCall(callId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis() - 2000)
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initial)
        awaitCollect()

        assertNotNull(vm.activeCall.value)
        assertEquals(false, vm.uiState.value.callEnded)

        DefaultCallRepository.remove(callId)
        awaitCollect()

        assertEquals(true, vm.uiState.value.callEnded)
        assertNull(vm.activeCall.value)
        assertEquals(0, vm.durationSec.value)
    }

    @Test
    fun isAudioToggling_resetsToFalseAfterOperation() = runBlocking {
        val callId = "test-call-5"
        val initial = makeCall(callId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis())
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initial)
        awaitCollect()

        vm.toggleMute()
        awaitCollect()

        assertEquals(false, vm.isAudioToggling.value)
    }

    @Test
    fun toggleMute_doubleCall_serializes() = runBlocking {
        val callId = "test-call-6"
        val initial = makeCall(callId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis())
        val vm = InCallViewModel(callId, DefaultCallRepository)

        DefaultCallRepository.upsert(callId, initial)
        awaitCollect()

        vm.toggleMute()
        vm.toggleMute()
        awaitCollect()

        assertEquals(false, vm.isAudioToggling.value)
    }

    @Test
    fun reopen_vmCreatedAfterCallActive_doesNotMarkEnded() = runBlocking {
        // Reopening IN_CALL from the banner creates the ViewModel while the call is
        // already ACTIVE (reverse subscription order vs. a fresh outgoing call).
        val callId = "test-call-reopen"
        DefaultCallRepository.upsert(
            callId,
            makeCall(callId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis()),
        )
        awaitCollect()

        val vm = InCallViewModel(callId, DefaultCallRepository)
        awaitCollect()

        assertEquals(CallState.ACTIVE, vm.activeCall.value?.state)
        assertEquals(false, vm.uiState.value.callEnded)
    }

    @Test
    fun reopen_emissionsForOtherCalls_doNotEndThisScreen() = runBlocking {
        // Banner reopen races with repository emissions for other ids (hold/swap,
        // mute updates): they must not mark this screen as seen or ended.
        val targetId = "test-call-target"
        val otherId = "test-call-other"
        val vm = InCallViewModel(targetId, DefaultCallRepository)

        DefaultCallRepository.upsert(
            otherId,
            makeCall(otherId, CallState.ACTIVE, connectTimeMillis = System.currentTimeMillis()),
        )
        awaitCollect()
        DefaultCallRepository.setMuted(true)
        awaitCollect()

        assertEquals(false, vm.uiState.value.callEnded)
    }

    private fun makeCall(
        id: String,
        state: CallState,
        connectTimeMillis: Long? = null
    ) = UiCall(
        id = id,
        number = "+1234567890",
        state = state,
        isMuted = false,
        isOnHold = false,
        connectTimeMillis = connectTimeMillis,
        audioRoute = AudioRoute.EARPIECE
    )
}
