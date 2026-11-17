package com.bookverse.contacts.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptedDraftViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { models.clear(); Dispatchers.resetMain() }

    @Test
    fun clearsPlaintextOnLockAndRestoresOnlyAfterAuthenticationIncludingRecreation() = runTest(dispatcher) {
        val session = MutableStateFlow(false)
        val handle = SavedStateHandle()
        val cipher = FakeCipher()
        val draft = EncryptedDraftViewModel(handle, cipher, session).also { models.put("first", it) }
        advanceUntilIdle()
        assertNull(draft.fields.value)
        assertEquals(0, cipher.opens)
        session.value = true
        advanceUntilIdle()
        draft.initialize(mapOf("password" to "synthetic-only"))
        draft.field("password", draft.fields.value).value = "latest-edit"
        session.value = false
        advanceUntilIdle()
        assertNull(draft.fields.value)
        assertArrayEquals(byteArrayOf(42), handle.get<ByteArray>("encrypted_draft"))
        models.clear()
        val restored = EncryptedDraftViewModel(handle, cipher, session).also { models.put("restored", it) }
        advanceUntilIdle()
        assertNull(restored.fields.value)
        session.value = true
        advanceUntilIdle()
        assertEquals("latest-edit", restored.fields.value?.get("password"))
        restored.clear()
        session.value = false
        advanceUntilIdle()
        assertNull(handle.get<ByteArray>("encrypted_draft"))
    }

    @Test
    fun corruptDraftNeverBecomesVisible() = runTest(dispatcher) {
        val session = MutableStateFlow(true)
        val draft = EncryptedDraftViewModel(SavedStateHandle(mapOf("encrypted_draft" to byteArrayOf(0))), FakeCipher(), session)
        models.put("corrupt", draft)
        advanceUntilIdle()
        assertTrue(draft.failed.value)
        assertEquals(emptyMap<String, String>(), draft.fields.value)
    }

    @Test
    fun sealsFinalRecorderMetadataWithoutExposingLockedFieldsOrRevivingClearedDrafts() = runTest(dispatcher) {
        val session = MutableStateFlow(true)
        val handle = SavedStateHandle()
        val draft = EncryptedDraftViewModel(handle, FakeCipher(), session).also { models.put("recording", it) }
        advanceUntilIdle()
        draft.initialize(mapOf("path" to ""))
        session.value = false
        advanceUntilIdle()
        draft.sealFinalSnapshot(mapOf("path" to "synthetic.m4a", "duration" to "2000"))
        advanceUntilIdle()
        assertNull(draft.fields.value)
        session.value = true
        advanceUntilIdle()
        assertEquals("synthetic.m4a", draft.fields.value?.get("path"))
        draft.clear()
        draft.sealFinalSnapshot(mapOf("path" to "discarded.m4a"))
        advanceUntilIdle()
        assertNull(handle.get<ByteArray>("encrypted_draft"))
    }

    private class FakeCipher : DraftCipher {
        private var sealed: Map<String, String> = emptyMap()
        var opens = 0
        override suspend fun seal(fields: Map<String, String>): ByteArray {
            sealed = fields.toMap()
            return byteArrayOf(42)
        }
        override suspend fun open(blob: ByteArray): Map<String, String> {
            opens++
            check(blob.contentEquals(byteArrayOf(42)))
            return sealed
        }
    }
}
