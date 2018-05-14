package com.shellmonger.apps.mynotes.repositories

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.shellmonger.apps.mynotes.models.Note

class MockNotesRepository : NotesRepository {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val _notes: MutableMap<String, Note> = HashMap()
    private val mutableNotes: MutableLiveData<List<Note>> = MutableLiveData()

    init {
        updateList()
    }

    private fun updateList() {
        mutableNotes.postValue(_notes.values.toList())
    }

    override val notes: LiveData<List<Note>> = mutableNotes

    override fun getNoteById(noteId: String): Note? = _notes[noteId]

    override fun saveNote(item: Note) {
        Log.d(TAG, "Saving note ${item.noteId}")
        _notes[item.noteId] = item
        updateList()
    }
}