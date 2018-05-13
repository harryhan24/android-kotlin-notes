package com.shellmonger.apps.mynotes.repositories

import android.util.Log
import com.shellmonger.apps.mynotes.models.Note

class MockNotesRepository : NotesRepository {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val notes: MutableMap<String, Note> = HashMap()

    override fun getNoteById(noteId: String): Note? = notes[noteId]

    override fun saveNote(item: Note) {
        Log.d(TAG, "Saving note ${item.noteId}")
        notes[item.noteId] = item
    }
}