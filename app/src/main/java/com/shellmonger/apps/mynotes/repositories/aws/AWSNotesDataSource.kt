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
package com.shellmonger.apps.mynotes.repositories.aws

import android.arch.paging.DataSource
import android.arch.paging.PageKeyedDataSource
import android.content.Context
import android.util.Log
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicCognitoUserPoolsAuthProvider
import com.amazonaws.regions.Regions
import com.apollographql.apollo.GraphQLCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.shellmonger.apps.mynotes.models.Note
import com.shellmonger.apps.mynotes.services.IdentityService
import com.shellmonger.apps.mynotes.services.aws.AWSIdentityService
import okhttp3.Interceptor
import okhttp3.OkHttpClient

class AWSNotesDataSource(context: Context, idService: IdentityService) : PageKeyedDataSource<String, Note>() {
    companion object {
        private val TAG = this::class.java.simpleName
        private const val MAX_PAGE_SIZE = 20
    }

    private val appSyncClient: AWSAppSyncClient

    init {
        val configProvider = AWSConfiguration(context)
        val appSyncConfig = configProvider.optJsonObject("AppSync")
        val cognitoUserPool = (idService as AWSIdentityService).userPool

        appSyncClient = AWSAppSyncClient.builder()
                .context(context)
                .region(Regions.fromName(appSyncConfig.getString("region")))
                .cognitoUserPoolsAuthProvider(BasicCognitoUserPoolsAuthProvider(cognitoUserPool))
                .serverUrl(appSyncConfig.getString("graphqlEndpoint"))
                .build()
    }

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<String, Note>) {
        Log.d(TAG, "loadInitial: requestedLoadSize=${params.requestedLoadSize}")
        val pageSize = minOf(MAX_PAGE_SIZE, params.requestedLoadSize)
        val query = AllNotesQuery.builder()
                .limit(pageSize)
                .build()

        val graphqlCallback = object : GraphQLCall.Callback<AllNotesQuery.Data>() {
            override fun onResponse(response: Response<AllNotesQuery.Data>) {
                if (response.hasErrors()) {
                    Log.d(TAG, "loadInitial::onResponse - has errors")
                } else {
                    Log.d(TAG, "loadInitial::onResponse - data received")
                    response.data()?.allNotes()?.let {
                        val results = mutableListOf<Note>()
                        for (rNote in it.notes()) {
                            results.add(Note(rNote.noteId()).apply { title = rNote.title() ?: "" })
                        }
                        callback.onResult(results, null, it.nextToken())
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.query(query)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(graphqlCallback)
    }

    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, Note>) {
        Log.d(TAG, "loadAfter: key=${params.key} requestedLoadSize=${params.requestedLoadSize}")
        val pageSize = minOf(MAX_PAGE_SIZE, params.requestedLoadSize)
        val query = AllNotesQuery.builder()
                .limit(pageSize)
                .nextToken(params.key)
                .build()

        val graphqlCallback = object : GraphQLCall.Callback<AllNotesQuery.Data>() {
            override fun onResponse(response: Response<AllNotesQuery.Data>) {
                if (response.hasErrors()) {
                    Log.d(TAG, "loadAfter::onResponse - has errors")
                } else {
                    Log.d(TAG, "loadAfter::onResponse - data received")
                    response.data()?.allNotes()?.let {
                        val results = mutableListOf<Note>()
                        for (rNote in it.notes()) {
                            results.add(Note(rNote.noteId()).apply { title = rNote.title() ?: "" })
                        }
                        callback.onResult(results, it.nextToken())
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.query(query)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(graphqlCallback)
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, Note>) {
        // We can't go backwards, so this should never happen.
        invalidate()
    }

    fun getNoteById(noteId: String, callback: (Note?) -> Unit) {
        Log.d(TAG, "getNoteById: noteId = $noteId")
        val query = GetNoteQuery.builder()
                .noteId(noteId)
                .build()

        val graphqlCallback = object : GraphQLCall.Callback<GetNoteQuery.Data>() {
            override fun onResponse(response: Response<GetNoteQuery.Data>) {
                if (response.hasErrors()) {
                    Log.d(TAG, "getNoteById::onResponse - has errors")
                } else {
                    Log.d(TAG, "getNoteById::onResponse - data received")
                    response.data()?.note?.let {
                        val note = Note(it.noteId()).apply {
                            title = it.title() ?: ""
                            content = it.content() ?: ""
                        }
                        val store = appSyncClient.store
                        callback(note)
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.query(query)
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(graphqlCallback)
    }

    fun saveItem(item: Note, callback: (Note) -> Unit) {
        Log.d(TAG, "Saving ite ${item.noteId}")
        val mutation = SaveNoteMutation.builder()
                .noteId(item.noteId)
                .title(if (item.title.isBlank()) " " else item.title)
                .content(if (item.content.isBlank()) " " else item.content)
                .build()

        val graphqlCallback = object : GraphQLCall.Callback<SaveNoteMutation.Data>() {
            override fun onResponse(response: Response<SaveNoteMutation.Data>) {
                if (response.hasErrors()) {
                    Log.d(TAG, "saveItem::onResponse - has errors")
                } else {
                    Log.d(TAG, "saveItem::onResponse - data received")
                    response.data()?.saveNote()?.let {
                        val note = Note(it.noteId()).apply {
                            title = it.title() ?: ""
                            content = it.content() ?: ""
                        }

                        invalidate()
                        runOnUiThread { callback(note) }
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.mutate(mutation)
                .enqueue(graphqlCallback)
    }

    fun deleteItem(item: Note, callback: () -> Unit) {
        Log.d(TAG, "Deleting item ${item.noteId}")
        val mutation = DeleteNoteMutation.builder()
                .noteId(item.noteId)
                .build()

        val graphqlCallback = object : GraphQLCall.Callback<DeleteNoteMutation.Data>() {
            override fun onResponse(response: Response<DeleteNoteMutation.Data>) {
                if (response.hasErrors()) {
                    Log.d(TAG, "saveItem::onResponse - has errors")
                } else {
                    Log.d(TAG, "deleteItem::onResponse - data received")
                    invalidate()
                    runOnUiThread { callback() }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.mutate(mutation)
                .enqueue(graphqlCallback)
    }
}

class AWSNotesDataSourceFactory(private val context: Context, private val identityService: IdentityService) : DataSource.Factory<String, Note>() {
    var dataSource: AWSNotesDataSource? = null

    override fun create(): DataSource<String, Note> {
        dataSource = AWSNotesDataSource(context, identityService)
        return dataSource as DataSource<String, Note>
    }
}
