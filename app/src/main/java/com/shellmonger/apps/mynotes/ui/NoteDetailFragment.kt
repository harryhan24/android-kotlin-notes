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
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shellmonger.apps.mynotes.R
import com.shellmonger.apps.mynotes.extensions.afterTextChanged
import com.shellmonger.apps.mynotes.extensions.set
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.viewmodels.NoteDetailViewModel
import kotlinx.android.synthetic.main.fragment_note_detail.*
import kotlinx.android.synthetic.main.fragment_note_detail.view.*
import org.koin.android.architecture.ext.viewModel

class NoteDetailFragment : Fragment() {
    companion object {
        private val TAG = this::class.java.simpleName
        const val ARG_NOTE = "noteId"
    }

    private val viewModel by viewModel<NoteDetailViewModel>()
    private val observer = Observer<Note> {
        if (it != null) {
            detail_id_field.text = it.noteId
            detail_title_editor.text.set(it.title)
            detail_content_editor.text.set(it.content)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var isLoaded = false
        arguments?.let {
            if (it.containsKey(ARG_NOTE)) {
                viewModel.loadNote(it.getString(ARG_NOTE))
                isLoaded = true
            }
        }
        if (!isLoaded) viewModel.newNote()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_note_detail, container, false)
        rootView.detail_title_editor.afterTextChanged { saveNote() }
        rootView.detail_content_editor.afterTextChanged { saveNote() }
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.currentNote.removeObserver(observer)
        viewModel.currentNote.observe(this, observer)
    }

    private fun saveNote() {
        Log.d(TAG, "Note is being saved")
        val currentNote = Note(viewModel.currentNote.value?.noteId!!).apply {
            title = view?.detail_title_editor?.text.toString()
            content = view?.detail_content_editor?.text.toString()
        }
        viewModel.saveNote(currentNote)
    }
}
