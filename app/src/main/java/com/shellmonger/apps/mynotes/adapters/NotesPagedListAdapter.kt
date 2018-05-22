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
package com.shellmonger.apps.mynotes.adapters

import android.arch.paging.PagedListAdapter
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.shellmonger.apps.mynotes.R
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.ui.ListItemViewHolder

class NotesPagedListAdapter(val onClick: (Note) -> Unit) : PagedListAdapter<Note, ListItemViewHolder>(Note.DiffCallback) {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder
        = ListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_list_item, parent, false))

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        Log.d(TAG, "Binding view holder at position $position")
        holder.note = getItem(position)
        holder.view.setOnClickListener { onClick(holder.note!!) }
    }
}