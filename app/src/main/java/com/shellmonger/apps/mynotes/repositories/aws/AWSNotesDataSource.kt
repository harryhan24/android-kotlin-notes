package com.shellmonger.apps.mynotes.repositories.aws

import android.arch.paging.DataSource
import android.arch.paging.PageKeyedDataSource
import android.content.Context
import android.util.Log
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
    private val okHttpClient: OkHttpClient

    init {
        val configProvider = AWSConfiguration(context)
        val appSyncConfig = configProvider.optJsonObject("AppSync")
        val cognitoUserPool = (idService as AWSIdentityService).userPool

        okHttpClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    Log.d(TAG, "In interceptor")
                    val request = chain.request()
                    val response = chain.proceed(request)
                    Log.d(TAG, "Response received")
                    if (!response.isSuccessful) {
                        Log.d(TAG, "Response was not successful")
                    }
                    return@Interceptor response
                })
                .build()


        appSyncClient = AWSAppSyncClient.builder()
                .context(context)
                .region(Regions.fromName(appSyncConfig.getString("region")))
                .cognitoUserPoolsAuthProvider(BasicCognitoUserPoolsAuthProvider(cognitoUserPool))
                .serverUrl(appSyncConfig.getString("graphqlEndpoint"))
                .okHttpClient(okHttpClient)
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
                .responseFetcher(AppSyncResponseFetchers.CACHE_FIRST)
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
                .responseFetcher(AppSyncResponseFetchers.CACHE_FIRST)
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
                        callback(note)
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.query(query)
                .responseFetcher(AppSyncResponseFetchers.CACHE_FIRST)
                .enqueue(graphqlCallback)
    }

    fun saveItem(item: Note, callback: (Note) -> Unit) {
        Log.d(TAG, "Saving ite ${item.noteId}")
        val mutation = SaveNoteMutation.builder()
                .noteId(item.noteId)
                .title(if (item.title == "") " " else item.title)
                .content(if (item.content == "") " " else item.content)
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
                        callback(note)
                    }
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.mutate(mutation).enqueue(graphqlCallback)
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
                    callback()
                }
            }

            override fun onFailure(e: ApolloException) {
                throw e
            }
        }

        appSyncClient.mutate(mutation).enqueue(graphqlCallback)
    }
}

class AWSNotesDataSourceFactory(context: Context, identityService: IdentityService) : DataSource.Factory<String, Note>() {
    val dataSource = AWSNotesDataSource(context, identityService)

    override fun create(): DataSource<String, Note> = dataSource
}
