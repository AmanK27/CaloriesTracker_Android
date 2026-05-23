package com.example.caloriestracker.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caloriestracker.domain.model.JournalEntry
import com.example.caloriestracker.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.deleteEntry(entry)
        }
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val entries: List<JournalEntry>) : MainScreenUiState
}
