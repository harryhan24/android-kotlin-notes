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
        it?.let {
            detail_id_field.text = it.noteId
            detail_title_editor.text.set(it.title)
            detail_content_editor.text.set(it.content)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_NOTE)) viewModel.loadNote(it.getString(ARG_NOTE))
        }
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
        val currentNote = (viewModel.currentNote.value ?: Note()).apply {
            title = view?.detail_title_editor?.text.toString()
            content = view?.detail_content_editor?.text.toString()
        }
        viewModel.saveNote(currentNote)
    }
}
