package com.shellmonger.apps.mynotes.adapters

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.shellmonger.apps.mynotes.R
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.ui.ListItemViewHolder

class NoteListAdapter(val onClick: (Note) -> Unit) : RecyclerView.Adapter<ListItemViewHolder>() {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private var items: List<Note> = emptyList()

    fun loadItems(newItems: List<Note>) {
        Log.d(TAG,"loading new items - nItems = ${newItems.size}")
        items = newItems
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        Log.d(TAG, "Creating view holder")
        return ListItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.note_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        Log.d(TAG, "Loading note ${items[position].noteId} into position $position")
        holder.note = items[position]
        holder.view.setOnClickListener { onClick(items[position]) }
    }
}