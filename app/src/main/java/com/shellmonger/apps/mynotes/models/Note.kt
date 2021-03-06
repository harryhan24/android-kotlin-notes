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
package com.shellmonger.apps.mynotes.models

import android.support.v7.util.DiffUtil
import java.util.*

/**
 * Representation of a single note within the application.
 */
data class Note(val noteId: String = UUID.randomUUID().toString()) {
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note?, newItem: Note?): Boolean
                = oldItem != null && newItem != null && oldItem.noteId == newItem.noteId

            override fun areContentsTheSame(oldItem: Note?, newItem: Note?): Boolean
                = oldItem != null && newItem != null
                    && oldItem.noteId == newItem.noteId
                    && oldItem.title == newItem.title
                    && oldItem.content == newItem.content
        }
    }
    var title: String = ""
    var content: String = ""
}