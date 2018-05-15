package com.shellmonger.apps.mynotes.repositories

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.util.Log
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.repositories.datasources.MockNotesDataSourceFactory

class MockNotesRepository : NotesRepository {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val factory = MockNotesDataSourceFactory()

    override val notes: LiveData<PagedList<Note>>

    init {
        Log.d(TAG, "Creating paged list livedata")
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(10)
                .setPageSize(10)
                .build()
        notes = LivePagedListBuilder<Int,Note>(factory, pagedListConfig).build()
    }

    override fun getNoteById(noteId: String): Note?
            = factory.dataSource.getNoteById(noteId)

    override fun saveNote(item: Note) {
        factory.dataSource.saveItem(item)
    }

}
