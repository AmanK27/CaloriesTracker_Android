package com.example.caloriestracker.ui.main

import androidx.lifecycle.viewModelScope
import com.example.caloriestracker.domain.model.JournalEntry
import com.example.caloriestracker.domain.repository.JournalRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeGeminiApi = object : com.example.caloriestracker.data.network.GeminiApi {
        override suspend fun generateContent(apiKey: String, request: com.example.caloriestracker.data.network.GeminiRequest): com.example.caloriestracker.data.network.GeminiResponse {
            return com.example.caloriestracker.data.network.GeminiResponse()
        }
    }
    private val fakeAiService = com.example.caloriestracker.data.network.AiService(fakeGeminiApi, kotlinx.serialization.json.Json { })

    private val fakeEmbeddingEngine = object : com.example.caloriestracker.data.search.EmbeddingEngine {
        override fun generateEmbedding(text: String): FloatArray {
            return FloatArray(384).apply { this[0] = 1f }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoading() = runTest {
        val repository = FakeJournalRepository()
        val viewModel = MainScreenViewModel(repository, fakeAiService, fakeEmbeddingEngine)
        
        // Assert that the initial state of StateFlow is indeed Loading
        assertEquals(MainScreenUiState.Loading, viewModel.uiState.value)

        // Retrieve the first success emission
        val state = viewModel.uiState.first { it is MainScreenUiState.Success }
        assert(state is MainScreenUiState.Success)
        val successState = state as MainScreenUiState.Success
        assertEquals(0, successState.entries.size)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun uiState_onSearchQueryChanged_filtersEntries() = runTest {
        val repository = FakeJournalRepository()
        val entry1 = JournalEntry(title = "Healthy breakfast", content = "Had eggs and toast")
        val entry2 = JournalEntry(title = "Late night snack", content = "Chocolate ice cream")
        repository.saveEntry(entry1)
        repository.saveEntry(entry2)

        val viewModel = MainScreenViewModel(repository, fakeAiService, fakeEmbeddingEngine)
        
        val states = mutableListOf<MainScreenUiState>()
        // Start collection to activate StateFlow and keep tracking its emissions
        val job = launch(testDispatcher) {
            viewModel.uiState.collect { states.add(it) }
        }

        // Change query
        viewModel.onSearchQueryChanged("eggs")
        
        // Extract success states
        val successStates = states.filterIsInstance<MainScreenUiState.Success>()
        assert(successStates.isNotEmpty())
        
        // The latest success state should contain only the filtered entry
        val lastSuccess = successStates.last()
        assertEquals(1, lastSuccess.entries.size)
        assertEquals("Healthy breakfast", lastSuccess.entries.first().title)

        job.cancel()
        viewModel.viewModelScope.cancel()
    }

}

private class FakeJournalRepository : JournalRepository {
    private val entriesFlow = MutableStateFlow<Map<UUID, JournalEntry>>(emptyMap())

    override fun getEntries(): Flow<List<JournalEntry>> {
        return entriesFlow.map { it.values.toList().sortedByDescending { e -> e.timestamp } }
    }

    override fun getEntryById(id: UUID): Flow<JournalEntry?> {
        return entriesFlow.map { it[id] }
    }

    override suspend fun saveEntry(entry: JournalEntry) {
        val updated = entriesFlow.value.toMutableMap()
        updated[entry.id] = entry
        entriesFlow.value = updated
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        val updated = entriesFlow.value.toMutableMap()
        updated.remove(entry.id)
        entriesFlow.value = updated
    }

    override fun searchEntries(query: String): Flow<List<JournalEntry>> {
        return getEntries().map { list ->
            if (query.isEmpty()) list
            else list.filter {
                (it.title?.contains(query, ignoreCase = true) ?: false) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
    }

    override suspend fun getSemanticContext(query: String, limit: Int): List<JournalEntry> {
        return searchEntries(query).first().take(limit)
    }
}
