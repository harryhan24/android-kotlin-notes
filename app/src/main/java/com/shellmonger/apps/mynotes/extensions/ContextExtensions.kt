package com.shellmonger.apps.mynotes.extensions


import android.content.Context
import android.net.ConnectivityManager

/**
 * Returns the application connectivity manager
 */
fun Context.getConnectivityManager(): ConnectivityManager
        = this.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager