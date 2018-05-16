package com.shellmonger.apps.mynotes.services

import android.arch.lifecycle.LiveData
import com.shellmonger.apps.mynotes.models.User

enum class IdentityRequest {
    NEED_SIGNUP,
    NEED_CREDENTIALS,
    NEED_NEWPASSWORD,
    NEED_MULTIFACTORCODE,
    SUCCESS,
    FAILURE
}

typealias IdentityResponse = (Map<String,String>?) -> Unit
typealias IdentityHandler = (IdentityRequest, Map<String,String>?, IdentityResponse) -> Unit

interface IdentityService {
    val currentUser: LiveData<User?>
    val storedUsername: LiveData<String?>

    fun signIn(handler: IdentityHandler)
    fun signOut(handler: IdentityHandler)
    fun forgotPassword(handler: IdentityHandler)
    fun signUp(handler: IdentityHandler)
    fun updateStoredUsername(username: String?)
}