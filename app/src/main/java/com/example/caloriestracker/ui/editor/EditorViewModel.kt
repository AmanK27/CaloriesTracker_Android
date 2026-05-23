package com.example.caloriestracker.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caloriestracker.domain.model.JournalEntry
import com.example.caloriestracker.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _entryState = MutableStateFlow<JournalEntry?>(null)
    val entryState: StateFlow<JournalEntry?> = _entryState.asStateFlow()

    // State flow specifically to trigger debounced autosaves
    private val _saveTrigger = MutableSharedFlow<JournalEntry>(replay = 0)

    init {
        viewModelScope.launch {
            _saveTrigger
                .debounce(1000) // Debounce autosave by 1 second to avoid excessive database writes
                .collect { entry ->
                    journalRepository.saveEntry(entry)
                }
        }
    }

    fun loadEntry(idString: String?) {
        if (idString == null) {
            // New Entry
            _entryState.value = JournalEntry()
        } else {
            // Loading existing Entry
            viewModelScope.launch {
                try {
                    val uuid = UUID.fromString(idString)
                    val entry = journalRepository.getEntryById(uuid).firstOrNull()
                    _entryState.value = entry ?: JournalEntry(id = uuid)
                } catch (e: Exception) {
                    _entryState.value = JournalEntry()
                }
            }
        }
    }

    fun updateTitle(title: String) {
        val current = _entryState.value ?: return
        val updated = current.copy(title = title, lastModified = System.currentTimeMillis())
        _entryState.value = updated
        triggerAutosave(updated)
    }

    fun updateContent(content: String) {
        val current = _entryState.value ?: return
        val updated = current.copy(content = content, lastModified = System.currentTimeMillis())
        _entryState.value = updated
        triggerAutosave(updated)
    }

    fun updateMood(mood: String?) {
        val current = _entryState.value ?: return
        val updated = current.copy(mood = mood, lastModified = System.currentTimeMillis())
        _entryState.value = updated
        triggerAutosave(updated)
    }

    fun addTag(tag: String) {
        val current = _entryState.value ?: return
        val cleanTag = tag.trim().lowercase()
        if (cleanTag.isNotEmpty() && !current.tags.contains(cleanTag)) {
            val updated = current.copy(
                tags = current.tags + cleanTag,
                lastModified = System.currentTimeMillis()
            )
            _entryState.value = updated
            triggerAutosave(updated)
        }
    }

    fun removeTag(tag: String) {
        val current = _entryState.value ?: return
        val updated = current.copy(
            tags = current.tags - tag,
            lastModified = System.currentTimeMillis()
        )
        _entryState.value = updated
        triggerAutosave(updated)
    }

    fun saveChangesImmediately() {
        val current = _entryState.value ?: return
        viewModelScope.launch {
            journalRepository.saveEntry(current)
        }
    }

    private fun triggerAutosave(entry: JournalEntry) {
        viewModelScope.launch {
            _saveTrigger.emit(entry)
        }
    }
}
