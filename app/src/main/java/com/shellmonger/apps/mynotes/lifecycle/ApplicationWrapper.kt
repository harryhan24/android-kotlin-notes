package com.shellmonger.apps.mynotes.lifecycle

import android.app.Application
import com.shellmonger.apps.mynotes.repositories.MockNotesRepository
import com.shellmonger.apps.mynotes.repositories.NotesRepository
import com.shellmonger.apps.mynotes.viewmodels.NoteDetailViewModel
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext

/**
 * Wrapper around the application - used for initialization that needs to take place before
 * the activity starts.  Examples include dependency injection and long running services.
 */
class ApplicationWrapper : Application() {
    companion object {
        private val modules : Module = applicationContext {
            bean { MockNotesRepository() as NotesRepository }
            viewModel { NoteDetailViewModel(get()) }
        }
    }
    /**
     * Lifecycle event method - called when the application is created.
     */
    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(modules))
    }
}