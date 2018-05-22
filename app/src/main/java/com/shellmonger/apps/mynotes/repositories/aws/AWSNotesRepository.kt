/*
    Copyright 2018 Adrian Hall

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.shellmonger.apps.mynotes.repositories.aws

import android.content.Context
import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.repositories.NotesRepository
import com.shellmonger.apps.mynotes.services.AnalyticsService
import com.shellmonger.apps.mynotes.services.IdentityService

class AWSNotesRepository(
        context: Context,
        identityService: IdentityService,
        private val analyticsService: AnalyticsService) : NotesRepository {

    private val factory = AWSNotesDataSourceFactory(context, identityService)
    override val notes: LiveData<PagedList<Note>>

    init {
        analyticsService.recordEvent("START_NOTES_REPOSITORY")
        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setInitialLoadSizeHint(10)
                .setPageSize(10)
                .build()
        notes = LivePagedListBuilder<String,Note>(factory, pagedListConfig).build()
    }

    override fun getNoteById(noteId: String, callback: (Note?) -> Unit) {
        factory.dataSource!!.getNoteById(noteId, callback)
    }

    override fun saveNote(item: Note, callback: (Note) -> Unit) {
        analyticsService.recordEvent("SAVE_ITEM")
        factory.dataSource!!.saveItem(item, callback)
    }

    override fun deleteNote(item: Note, callback: () -> Unit) {
        analyticsService.recordEvent("DELETE_ITEM")
        factory.dataSource!!.deleteItem(item, callback)
    }
}