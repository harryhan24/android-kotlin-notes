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
package com.shellmonger.apps.mynotes.viewmodels

import android.arch.lifecycle.ViewModel
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.repositories.NotesRepository

class NoteListViewModel(private val repository: NotesRepository) : ViewModel() {
    val notes = repository.notes

    fun deleteNote(item: Note, callback: () -> Unit) {
        repository.deleteNote(item, callback)
    }
}