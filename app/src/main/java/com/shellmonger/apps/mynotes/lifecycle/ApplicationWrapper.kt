package com.shellmonger.apps.mynotes.lifecycle

import android.app.Application
import com.shellmonger.apps.mynotes.repositories.mock.MockNotesRepository
import com.shellmonger.apps.mynotes.repositories.NotesRepository
import com.shellmonger.apps.mynotes.repositories.aws.AWSNotesDataSource
import com.shellmonger.apps.mynotes.repositories.aws.AWSNotesRepository
import com.shellmonger.apps.mynotes.services.AnalyticsService
import com.shellmonger.apps.mynotes.services.IdentityService
import com.shellmonger.apps.mynotes.services.aws.AWSAnalyticsService
import com.shellmonger.apps.mynotes.services.aws.AWSIdentityService
import com.shellmonger.apps.mynotes.services.mock.MockAnalyticsService
import com.shellmonger.apps.mynotes.services.mock.MockIdentityService
import com.shellmonger.apps.mynotes.viewmodels.AuthenticatorActivityViewModel
import com.shellmonger.apps.mynotes.viewmodels.NoteDetailViewModel
import com.shellmonger.apps.mynotes.viewmodels.NoteListViewModel
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
        private val services : Module = applicationContext {
            bean { AWSAnalyticsService(get()) as AnalyticsService }
            bean { AWSIdentityService(get()) as IdentityService }
        }

        private val repositories : Module = applicationContext {
            bean { AWSNotesRepository(get(), get(), get()) as NotesRepository }
        }
        private val viewModels : Module = applicationContext {
            viewModel { AuthenticatorActivityViewModel(get()) }
            viewModel { NoteDetailViewModel(get()) }
            viewModel { NoteListViewModel(get()) }
        }
    }
    /**
     * Lifecycle event method - called when the application is created.
     */
    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(services, repositories, viewModels))
    }
}