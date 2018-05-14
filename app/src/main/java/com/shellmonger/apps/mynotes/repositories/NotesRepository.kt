package com.shellmonger.apps.mynotes.repositories

import android.arch.lifecycle.LiveData
import com.shellmonger.apps.mynotes.models.Note

interface NotesRepository {
    val notes: LiveData<List<Note>>

    fun getNoteById(noteId: String): Note?

    fun saveNote(item: Note)
}