package com.shellmonger.apps.mynotes.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.shellmonger.apps.mynotes.R
import kotlinx.android.synthetic.main.activity_note_detail.*

class NoteDetailActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.simpleName
        const val ARG_NOTE = "noteId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)
        setSupportActionBar(note_detail_toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true) }

        if (intent?.extras?.containsKey(ARG_NOTE) == true) {
            val noteId = intent.extras.getString(ARG_NOTE)
            loadFragment(noteId)
        } else {
            loadFragment(null)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean
        = when(item?.itemId) {
            android.R.id.home -> navigateUpTo(Intent(this, NoteListActivity::class.java))
            else -> super.onOptionsItemSelected(item)
        }

    private fun loadFragment(noteId: String?) {
        Log.d(TAG, "Loading fragment for note $noteId")
        val fragment = NoteDetailFragment()
        noteId?.let {
            fragment.arguments = Bundle().apply { putString(NoteDetailFragment.ARG_NOTE, noteId) }
        }
        supportFragmentManager
            ?.beginTransaction()
            ?.replace(R.id.note_detail_fragment, fragment)
            ?.commit()
    }
}
