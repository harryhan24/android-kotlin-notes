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
