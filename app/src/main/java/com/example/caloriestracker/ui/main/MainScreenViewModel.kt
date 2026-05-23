package com.example.caloriestracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caloriestracker.data.network.AiService
import com.example.caloriestracker.data.search.EmbeddingEngine
import com.example.caloriestracker.domain.model.JournalEntry
import com.example.caloriestracker.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab { Timeline, AiChat, Insights }

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val aiService: AiService,
    private val embeddingGenerator: EmbeddingEngine
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.Timeline)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Chat States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(isUser = false, text = "Hello! I am your AI journaling coach. Ask me any questions about your past entries, or request a summary!")
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainScreenUiState> = _searchQuery
        .flatMapLatest { query ->
            journalRepository.searchEntries(query)
                .map<List<JournalEntry>, MainScreenUiState> { MainScreenUiState.Success(it) }
        }
        .catch { emit(MainScreenUiState.Error(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainScreenUiState.Loading
        )

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.deleteEntry(entry)
        }
    }

    fun sendChatMessage(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return

        val userMsg = ChatMessage(isUser = true, text = trimmedQuery)
        _chatMessages.update { it + userMsg }
        _chatLoading.value = true

        viewModelScope.launch {
            try {
                // 1. Fetch relevant entries for context (RAG)
                val contextEntries = journalRepository.getSemanticContext(trimmedQuery, limit = 3)
                // 2. Generate reflection using Gemini
                val reflection = aiService.generateReflection(trimmedQuery, contextEntries)
                
                _chatMessages.update { it + ChatMessage(isUser = false, text = reflection) }
            } catch (e: Exception) {
                _chatMessages.update { it + ChatMessage(isUser = false, text = "Sorry, I encountered an error generating reflection: ${e.message}") }
            } finally {
                _chatLoading.value = false
            }
        }
    }

    // Mood Stats for Insights
    val moodStats: StateFlow<Map<String, Int>> = uiState
        .map { state ->
            if (state is MainScreenUiState.Success) {
                state.entries
                    .filter { it.mood != null }
                    .groupBy { it.mood!!.lowercase() }
                    .mapValues { it.value.size }
            } else {
                emptyMap()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val entries: List<JournalEntry>) : MainScreenUiState
}
