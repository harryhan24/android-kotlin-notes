package com.shellmonger.apps.mynotes.extensions

import android.util.Patterns

/**
 * Returns true if the string is a valid email address
 */
fun String.isValidEmail(): Boolean = this.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()