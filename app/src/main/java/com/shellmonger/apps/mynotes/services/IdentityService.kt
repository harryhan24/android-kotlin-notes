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