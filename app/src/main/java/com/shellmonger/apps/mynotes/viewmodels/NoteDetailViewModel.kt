package com.shellmonger.apps.mynotes.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.repositories.NotesRepository

class NoteDetailViewModel(private val repository: NotesRepository) : ViewModel() {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val mutableNoteId: MutableLiveData<String?> = MutableLiveData()
    val currentNote: LiveData<Note>


    init {
        currentNote = Transformations.switchMap(mutableNoteId, { loadNote(it) })
    }

    private fun loadNote(noteId: String?): LiveData<Note> {
        Log.d(TAG, "Loading note")
        val note: Note = if (noteId == null) Note() else repository.getNoteById(noteId) ?: Note()
        return MutableLiveData<Note>().apply { postValue(note) }
    }

    fun setNoteId(noteId: String) {
        Log.d(TAG, "NoteDetail of $noteId requested")
        mutableNoteId.postValue(noteId)
    }

    fun saveNote(item: Note) {
        Log.d(TAG, "Saving note ${item.noteId}")
        repository.saveNote(item)
        if (mutableNoteId.value != item.noteId) setNoteId(item.noteId)
    }
}