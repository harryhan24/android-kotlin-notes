package com.shellmonger.apps.mynotes.repositories.mock

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.repositories.NotesRepository
import com.shellmonger.apps.mynotes.services.AnalyticsService

class MockNotesRepository(private val analyticsService: AnalyticsService) : NotesRepository {
    private val factory = MockNotesDataSourceFactory()
    override val notes: LiveData<PagedList<Note>>

    init {
        analyticsService.recordEvent("START_NOTES_REPOSITORY")
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(10)
                .setPageSize(10)
                .build()
        notes = LivePagedListBuilder<Int,Note>(factory, pagedListConfig).build()
    }

    override fun getNoteById(noteId: String, callback: (Note?) -> Unit) {
        factory.dataSource.getNoteById(noteId, callback)
    }

    override fun saveNote(item: Note, callback: (Note) -> Unit) {
        analyticsService.recordEvent("SAVE_ITEM")
        factory.dataSource.saveItem(item, callback)
    }

    override fun deleteNote(item: Note, callback: () -> Unit) {
        analyticsService.recordEvent("DELETE_ITEM")
        factory.dataSource.deleteItem(item, callback)
    }
}
