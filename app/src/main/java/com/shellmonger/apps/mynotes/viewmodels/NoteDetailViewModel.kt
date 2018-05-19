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

    private val mCurrentNote: MutableLiveData<Note> = MutableLiveData()
    val currentNote: LiveData<Note> = mCurrentNote

    fun loadNote(noteId: String) {
        repository.getNoteById(noteId) {
            mCurrentNote.postValue(it)
        }
    }

    fun saveNote(item: Note) {
        Log.d(TAG, "Saving note ${item.noteId}")
        repository.saveNote(item) {
            /* Do nothing */
        }
    }
}