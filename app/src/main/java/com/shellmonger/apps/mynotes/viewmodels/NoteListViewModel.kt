package com.shellmonger.apps.mynotes.viewmodels

import android.arch.lifecycle.ViewModel
import com.shellmonger.apps.mynotes.repositories.NotesRepository

class NoteListViewModel(repository: NotesRepository) : ViewModel() {
    val notes = repository.notes
}