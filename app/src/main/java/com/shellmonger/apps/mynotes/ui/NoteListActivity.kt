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
package com.shellmonger.apps.mynotes.ui

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import com.shellmonger.apps.mynotes.R
import com.shellmonger.apps.mynotes.adapters.NotesPagedListAdapter
import com.shellmonger.apps.mynotes.viewmodels.NoteListViewModel
import kotlinx.android.synthetic.main.activity_note_list.*
import org.koin.android.architecture.ext.viewModel

class NoteListActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val viewModel by viewModel<NoteListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)
        setSupportActionBar(note_list_toolbar)

        note_list_add_note.setOnClickListener {
            Log.d(TAG, "addNote button pressed")
            launchNoteDetailActivity(this)
        }

        val adapter = NotesPagedListAdapter {
            launchNoteDetailActivity(this@NoteListActivity, it.noteId)
        }
        viewModel.notes.observe(this, Observer {
            it?.let { adapter.submitList(it) }
        })
        note_list_recyclerview.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        note_list_recyclerview.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteHandler(this) {
            val position = it.adapterPosition
            it.note?.let {
                viewModel.deleteNote(it) { adapter.notifyItemRemoved(position) }
            }
        })
        itemTouchHelper.attachToRecyclerView(note_list_recyclerview)
    }

    private fun launchNoteDetailActivity(context: Context, noteId: String? = null) {
        val intent = Intent(context, NoteDetailActivity::class.java)
        noteId?.let {
            val bundle = Bundle().apply { putString(NoteDetailActivity.ARG_NOTE, noteId) }
            intent.putExtras(bundle)
        }
        context.startActivity(intent)
    }
}
