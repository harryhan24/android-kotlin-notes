package com.shellmonger.apps.mynotes.ui

import android.support.v7.widget.RecyclerView
import android.view.View
import com.shellmonger.apps.mynotes.models.Note
import kotlinx.android.synthetic.main.note_list_item.view.*

class ListItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    var note: Note? = null
        set(value) {
            field = value
            view.list_item_id.text = value?.noteId
            view.list_item_title.text = value?.title
        }
}