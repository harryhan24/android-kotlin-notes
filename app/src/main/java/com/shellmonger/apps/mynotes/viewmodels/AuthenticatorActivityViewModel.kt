package com.shellmonger.apps.mynotes.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.shellmonger.apps.mynotes.models.User
import com.shellmonger.apps.mynotes.services.IdentityHandler
import com.shellmonger.apps.mynotes.services.IdentityService

class AuthenticatorActivityViewModel(private val identityService: IdentityService) : ViewModel() {
    val currentUser: LiveData<User?> = identityService.currentUser
    val storedUsername: LiveData<String?> = identityService.storedUsername

    fun signIn(handler: IdentityHandler) = identityService.signIn(handler)
    fun forgotPassword(handler: IdentityHandler) = identityService.forgotPassword(handler)
    fun signUp(handler: IdentityHandler) = identityService.signUp(handler)
    fun updateStoredUsername(username: String?) = identityService.updateStoredUsername(username)
}