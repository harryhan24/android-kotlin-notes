package com.shellmonger.apps.mynotes.repositories

import android.arch.lifecycle.LiveData
import android.arch.paging.PagedList
import com.shellmonger.apps.mynotes.models.Note

interface NotesRepository {
    val notes: LiveData<PagedList<Note>>

    fun getNoteById(noteId: String, callback: (Note?) -> Unit)

    fun saveNote(item: Note, callback: (Note) -> Unit)

    fun deleteNote(item: Note, callback: () -> Unit)
}