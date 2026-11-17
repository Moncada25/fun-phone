package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.data.qr.QRHistoryEntity
import com.bookverse.contacts.data.qr.QRHistoryStore
import com.bookverse.contacts.data.qr.QRType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QRHistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shouldCreateHistoryEntryThroughStore() = runTest(dispatcher) {
        val store = FakeQRHistoryStore()
        val viewModel = QRHistoryViewModel(store, dispatcher)

        viewModel.addQRHistory(
            content = "https://example.com",
            isUrl = true,
            type = QRType.SCANNED,
        )
        advanceUntilIdle()

        assertEquals("https://example.com", store.inserted?.content)
        assertEquals(true, store.inserted?.isUrl)
        assertEquals(QRType.SCANNED, store.inserted?.type)
    }

    @Test
    fun shouldForwardDeleteAndClearThroughStore() = runTest(dispatcher) {
        val store = FakeQRHistoryStore()
        val viewModel = QRHistoryViewModel(store, dispatcher)

        viewModel.deleteQRHistory(42)
        viewModel.clearAllHistory()
        advanceUntilIdle()

        assertEquals(42L, store.deletedId)
        assertTrue(store.cleared)
    }

    @Test
    fun shouldExposeContentStateFromHistoryStore() = runTest(dispatcher) {
        val store = FakeQRHistoryStore()
        val viewModel = QRHistoryViewModel(store, dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        val item = QRHistoryEntity(
            id = 9,
            content = "payload",
            isUrl = false,
            type = QRType.GENERATED,
        )

        store.history.value = listOf(item)
        advanceUntilIdle()

        assertEquals(QRHistoryUiState(isLoading = false, items = listOf(item)), viewModel.uiState.value)
    }

    @Test
    fun shouldEmitOneShotFailureEffectWithoutPersistingItInUiState() = runTest(dispatcher) {
        val store = FakeQRHistoryStore(failInsert = true)
        val viewModel = QRHistoryViewModel(store, dispatcher)
        val effect = async { viewModel.effects.first() }
        runCurrent()

        viewModel.addQRHistory("payload", false, QRType.GENERATED)
        advanceUntilIdle()

        assertEquals(QRHistoryEffect.MutationFailed, effect.await())
        assertEquals(QRHistoryUiState(), viewModel.uiState.value)
    }

    private class FakeQRHistoryStore(
        private val failInsert: Boolean = false,
    ) : QRHistoryStore {
        override val history = MutableStateFlow<List<QRHistoryEntity>>(emptyList())
        var inserted: QRHistoryEntity? = null
        var deletedId: Long? = null
        var cleared = false

        override suspend fun insert(entry: QRHistoryEntity) {
            if (failInsert) error("synthetic failure")
            inserted = entry
        }

        override suspend fun delete(id: Long) {
            deletedId = id
        }

        override suspend fun clear() {
            cleared = true
        }
    }
}
