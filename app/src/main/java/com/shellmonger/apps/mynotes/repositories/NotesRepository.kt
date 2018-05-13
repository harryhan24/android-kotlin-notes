package com.shellmonger.apps.mynotes.repositories

import com.shellmonger.apps.mynotes.models.Note

interface NotesRepository {
    fun getNoteById(noteId: String): Note?

    fun saveNote(item: Note)
}