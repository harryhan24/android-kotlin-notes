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
package com.shellmonger.apps.mynotes.services.mock

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.shellmonger.apps.mynotes.models.User
import com.shellmonger.apps.mynotes.services.IdentityHandler
import com.shellmonger.apps.mynotes.services.IdentityRequest
import com.shellmonger.apps.mynotes.services.IdentityService
import java.util.*

data class MockUser(val username: String) {
    var password: String = ""
    var mfaCode: String = ""
    var passwordReset: Boolean = false
    var attributes: MutableMap<String, String> = HashMap()
}

class MockIdentityService(context: Context): IdentityService {
    companion object {
        private val TAG = this::class.java.simpleName
        private val DO_NOTHING: (Map<String,String>?) -> Unit = { }
        private val PREFS_FILE: String = "${this::class.java.canonicalName}.PREFS"
        private const val USERNAME_PREF: String = "authenticator-username"
    }

    private val mutableCurrentUser: MutableLiveData<User?> = MutableLiveData()
    private val mutableStoredUsername: MutableLiveData<String?> = MutableLiveData()
    private val sharedPreferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val mockUserMap: MutableMap<String, MockUser> = HashMap()

    init {
        mutableCurrentUser.value = null
        mockUserMap[UUID.randomUUID().toString()] = MockUser("user@user.com").apply {
            password = "abcd1234"
            mfaCode = "123456"
            attributes = mutableMapOf("name" to "User 1", "phone_number" to "+17205551212")
        }
        mockUserMap[UUID.randomUUID().toString()] = MockUser("reset@password.com").apply {
            password = "abcd1234"
            mfaCode = "000000"
            passwordReset = true
            attributes = mutableMapOf("name" to "User 2", "phone_number" to "+14085551212")
        }
        mutableStoredUsername.value = sharedPreferences.getString(USERNAME_PREF, null)
    }

    override val currentUser: LiveData<User?> = mutableCurrentUser
    override val storedUsername: LiveData<String?> = mutableStoredUsername

    override fun signIn(handler: IdentityHandler) {
        handler(IdentityRequest.NEED_CREDENTIALS, null) {
            response -> signInWithCredentials(handler, response)
        }
    }

    override fun signOut(handler: IdentityHandler) {
        mutableCurrentUser.value = null
        handler(IdentityRequest.SUCCESS, emptyMap(), DO_NOTHING)
    }

    override fun forgotPassword(handler: IdentityHandler) {
        handler(IdentityRequest.NEED_CREDENTIALS, null) {
            response -> forgotPasswordWithCredentials(handler, response)
        }
    }

    override fun signUp(handler: IdentityHandler) {
        handler(IdentityRequest.NEED_SIGNUP, null) {
            response -> signUpWithCredentials(handler, response)
        }
    }

    override fun updateStoredUsername(username: String?) {
        with (sharedPreferences.edit()) {
            if (username == null) {
                remove(USERNAME_PREF)
            } else {
                putString(USERNAME_PREF, username)
            }
            apply()
        }
        mutableStoredUsername.value = username
    }

    private fun storeNewUserProfile(handler: IdentityHandler, mockUser: MockUser, parameters: Map<String, String>) {
        val user = User().apply {
            username = parameters["username"] ?: ""
            for (key in parameters.keys) userAttributes[key] = parameters[key] ?: ""
            for (entry in mockUser.attributes) userAttributes[entry.key] = entry.value
        }
        mutableCurrentUser.value = user
        handler(IdentityRequest.SUCCESS, parameters, DO_NOTHING)
    }

    private fun signInWithCredentials(handler: IdentityHandler, nResponse: Map<String, String>?) {
        val parameters: MutableMap<String, String> = HashMap()

        try {
            val response = checkNotNull(nResponse) { "Invalid response from NEED_CREDENTIALS" }

            // Copy the response into the parameters
            for (key in response.keys) parameters[key] = response[key] ?: ""
            val username = parameters["username"] ?: ""
            val password = parameters["password"] ?: ""
            check(username.isNotEmpty()) { "Invalid Username" }
            check(password.isNotEmpty()) { "Invalid Password" }

            val mockUser = mockUserMap.entries.find { it.value.username == username }
            when {
                mockUser == null -> handleFailure(handler, "Username does not exist")
                mockUser.value.password != password -> handleFailure(handler, "Password incorrect")
                mockUser.value.passwordReset -> // Test the new password flow
                    handler(IdentityRequest.NEED_NEWPASSWORD, null) {
                        checkNotNull(it) { "Invalid response from NEED_NEWPASSWORD" }
                        val newpassword = parameters["password"] ?: ""
                        check(newpassword.isNotEmpty()) { "Invalid new password" }
                        mockUserMap[mockUser.key]?.passwordReset = false
                        mockUserMap[mockUser.key]?.password = newpassword
                        storeNewUserProfile(handler, mockUser.value, parameters)
                        return@handler
                    }
                else -> { // Test the MFA flow
                    Log.d(TAG, "MULTIFACTOR: Code = ${mockUser.value.mfaCode}")
                    handler(IdentityRequest.NEED_MULTIFACTORCODE, null) {
                        val mfaResponse = checkNotNull(it) { "Invalid response from NEED_MULTIFACTORCODE" }
                        val mfaCode = mfaResponse["mfaCode"] ?: ""
                        if (mockUser.value.mfaCode != mfaCode) {
                            handleFailure(handler, "MFA Code Incorrect")
                        } else {
                            storeNewUserProfile(handler, mockUser.value, parameters)
                            return@handler
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            handleFailure(handler, exception.message)
        }
    }

    private fun forgotPasswordWithCredentials(handler: IdentityHandler, nResponse: Map<String, String>?) {
        val parameters: MutableMap<String, String> = HashMap()

        try {
            val response = checkNotNull(nResponse) { "Invalid response from NEED_CREDENTIALS" }
            for (key in response.keys) parameters[key] = response[key] ?: ""
            val username = parameters["username"] ?: ""
            val password = parameters["password"] ?: ""
            check(username.isNotEmpty()) { "Invalid username" }
            check(password.isNotEmpty()) { "Invalid password" }

            val mockUser = mockUserMap.entries.find { it.value.username == username }
            if (mockUser == null) {
                handleFailure(handler, "Username does not exist")
            } else {
                handler(IdentityRequest.NEED_MULTIFACTORCODE, mapOf("deliveryVia" to "SMS", "deliveryTo" to "+1705551212")) { nMfaResponse ->
                    run {
                        val mfaResponse = checkNotNull(nMfaResponse) { "Invalid response to NEED_MULTIFACTORCODE" }
                        val mfaCode = mfaResponse["mfaCode"] ?: ""
                        if (mfaCode != mockUser.value.mfaCode) {
                            handleFailure(handler, "MFA Code does not match")
                        } else {
                            mockUserMap[mockUser.key]?.password = password
                            handler(IdentityRequest.SUCCESS, null, DO_NOTHING)
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            handleFailure(handler, exception.message)
        }
    }

    private fun signUpWithCredentials(handler: IdentityHandler, nResponse: Map<String, String>?) {
        try {
            val response = checkNotNull(nResponse) { "Invalid response from NEED_SIGNUP" }

            val emailaddr = response["username"] ?: ""
            val mPassword = response["password"] ?: ""
            val phone = response["phone"] ?: ""
            val name = response["name"] ?: ""
            check(emailaddr.isNotEmpty()) { "Email Address is empty" }
            check(mPassword.isNotEmpty()) { "Password is empty" }
            check(phone.isNotEmpty()) { "Phone is empty" }
            check(name.isNotEmpty()) { "Name is empty" }

            handler(IdentityRequest.NEED_MULTIFACTORCODE, mapOf("deliveryVia" to "SMS", "deliveryTo" to "144255")) { nMfaResponse ->
                run {
                    try {
                        val mfaResponse = checkNotNull(nMfaResponse) { "Invalid response from NEED_MULTIFACTORCODE" }
                        val rMFA = mfaResponse["mfaCode"] ?: ""
                        check(rMFA.length == 6) { "Invalid MFA Code len=${rMFA.length}" }
                        check(rMFA == "144255") { "Invalid Code Entered" }

                        mockUserMap[UUID.randomUUID().toString()] = MockUser(emailaddr).apply {
                            password = mPassword
                            mfaCode = rMFA
                            passwordReset = false
                            attributes = mutableMapOf("name" to name, "phone_number" to phone)
                        }
                        handler(IdentityRequest.SUCCESS, null, DO_NOTHING)
                    } catch (exception: Exception) {
                        handleFailure(handler, exception.message)
                    }
                }
            }
        } catch (exception: Exception) {
            handleFailure(handler, exception.message)
        }
    }

    private fun handleFailure(handler: IdentityHandler, message: String?) {
        handler(IdentityRequest.FAILURE, mapOf("message" to (message ?: "Unknown error")), DO_NOTHING)
    }
}