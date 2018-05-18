package com.shellmonger.apps.mynotes.viewmodels

import android.arch.lifecycle.ViewModel
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.repositories.NotesRepository

class NoteListViewModel(private val repository: NotesRepository) : ViewModel() {
    val notes = repository.notes

    fun deleteNote(item: Note, callback: () -> Unit) {
        repository.deleteNote(item, callback)
    }
}