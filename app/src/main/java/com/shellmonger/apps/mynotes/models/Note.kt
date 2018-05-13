package com.shellmonger.apps.mynotes.models

import java.util.*

/**
 * Representation of a single note within the application.
 */
data class Note(val noteId: String = UUID.randomUUID().toString()) {
    var title: String = ""
    var content: String = ""
}