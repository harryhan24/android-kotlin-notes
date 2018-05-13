package com.shellmonger.apps.mynotes.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.shellmonger.apps.mynotes.R

class NoteDetailActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.simpleName
        const val ARG_NOTE = "noteId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        if (intent?.extras?.containsKey(ARG_NOTE) == true) {
            val noteId = intent.extras.getString(ARG_NOTE)
            loadFragment(noteId)
        } else {
            loadFragment(null)
        }
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
