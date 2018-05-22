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
