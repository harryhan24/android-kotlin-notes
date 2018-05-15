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