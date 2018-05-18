package com.shellmonger.apps.mynotes.repositories.mock

import android.arch.paging.DataSource
import android.arch.paging.ItemKeyedDataSource
import android.util.Log
import com.shellmonger.apps.mynotes.models.Note

class MockNotesDataSource : ItemKeyedDataSource<Int, Note>() {
    companion object {
        private val TAG = this::class.java.simpleName

        const val MAX_PAGE_SIZE = 20
    }

    /**
     * The list of items in the data source
     */
    private val items: MutableList<Note> = ArrayList()

    init {
        // Create some fake data
        for (i in 0..200) {
            items.add(Note().apply { title = "title $i"; content = "content $i" })
        }
    }

    fun inRange(position: Int, start: Int, end: Int): Int {
        if (position < start) return start
        if (position > end) return end
        return position
    }

    /**
     * Load the initial items
     */
    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Note>) {
        Log.d(TAG, "loadInitial: key=${params.requestedInitialKey ?: "undefined"}, size=${params.requestedLoadSize}")
        val pageSize = minOf(MAX_PAGE_SIZE, params.requestedLoadSize)
        val firstItem = inRange(params.requestedInitialKey ?: 0,0, items.size)
        val lastItem = inRange(firstItem + pageSize, 0, items.size)
        Log.d(TAG, "loadInitial: firstItem = $firstItem, lastItem = $lastItem")
        val data = if (firstItem == lastItem) emptyList() else items.subList(firstItem, lastItem).toList()
        if (params.placeholdersEnabled) {
            callback.onResult(data, firstItem, items.size)
        } else {
            callback.onResult(data)
        }
    }

    /**
     * Load the next page
     */
    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Note>) {
        Log.d(TAG, "loadAfter: key=${params.key}, size=${params.requestedLoadSize}")
        val pageSize = minOf(MAX_PAGE_SIZE, params.requestedLoadSize)
        val firstItem = inRange(params.key + 1, 0, items.size)
        val lastItem = inRange(firstItem + pageSize, 0, items.size)
        Log.d(TAG, "loadAfter: firstItem = $firstItem, lastItem = $lastItem")
        val data = if (firstItem == lastItem) emptyList() else items.subList(firstItem, lastItem).toList()
        callback.onResult(data)
    }

    /**
     * Load the previous page
     */
    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Note>) {
        Log.d(TAG, "loadAfter: key=${params.key}, size=${params.requestedLoadSize}")
        val pageSize = minOf(MAX_PAGE_SIZE, params.requestedLoadSize)
        val lastItem = inRange(params.key - 1, 0, items.size)
        val firstItem = inRange(lastItem - pageSize, 0, items.size)
        Log.d(TAG, "loadBefore: firstItem = $firstItem, lastItem = $lastItem")
        val data = if (firstItem == lastItem) emptyList() else items.subList(firstItem, lastItem).toList()
        callback.onResult(data)
    }

    /**
     * Obtain an item based on the position
     */
    override fun getKey(item: Note): Int
            = items.indexOfFirst { it.noteId == item.noteId }

    /**
     * Obtain an item based on the ID
     */
    fun getNoteById(noteId: String, callback: (Note?) -> Unit) {
        callback(items.first { it.noteId == noteId })
    }

    /**
     * Save a new item to the list
     */
    fun saveItem(item: Note, callback: (Note) -> Unit) {
        Log.d(TAG, "Saving item ${item.noteId}")
        val index = getKey(item)
        if (index < 0) items.add(item) else items[index] = item
        invalidate()        // Tell the system the data has changed
        callback(item)      // Callback to tell the UI that it's been successfully saved
    }

    /**
     * delete an item from the list
     */
    fun deleteItem(item: Note, callback: () -> Unit) {
        val index = getKey(item)
        Log.d(TAG, "There are now ${items.size} items (index = $index)")
        if (index >= 0) {
            Log.d(TAG, "Deleting item ${item.noteId}")
            items.removeAt(index)
            Log.d(TAG, "There are now ${items.size} items")
            invalidate()
        } else {
            Log.d(TAG, "Item ${item.noteId} not found for deletion")
        }
        callback()
    }
}

class MockNotesDataSourceFactory : DataSource.Factory<Int,Note>() {
    val dataSource = MockNotesDataSource()
    override fun create(): DataSource<Int, Note> = dataSource
}