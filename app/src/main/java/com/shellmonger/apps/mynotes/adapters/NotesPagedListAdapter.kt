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