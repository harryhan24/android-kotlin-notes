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
package com.shellmonger.apps.mynotes.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import com.shellmonger.apps.mynotes.R
import com.shellmonger.apps.mynotes.extensions.afterTextChanged
import com.shellmonger.apps.mynotes.extensions.isValidEmail
import com.shellmonger.apps.mynotes.extensions.validate
import com.shellmonger.apps.mynotes.services.IdentityRequest
import com.shellmonger.apps.mynotes.viewmodels.AuthenticatorActivityViewModel
import kotlinx.android.synthetic.main.activity_forgot_password.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.koin.android.architecture.ext.viewModel

class ForgotPasswordActivity : AppCompatActivity() {
    companion object {
        private val TAG: String = this::class.java.simpleName
    }

    /**
     * View model for this activity
     */
    private val model by viewModel<AuthenticatorActivityViewModel>()

    /**
     * Called when the activity is starting. This is where most initialization should go: calling
     * setContentView(int) to inflate the activity's UI, initializing any view models, etc.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Work out if we passed in a username from the login screen - if we did, then
        // use it.
        if (intent.hasExtra("login_username")) {
            forgot_username.text.clear()
            forgot_username.text.append(intent.getStringExtra("login_username"))
            if (forgot_username.text.isNotBlank()) forgot_password.requestFocus()
        }

        // We should be able to close this activity, in which case we go back
        // to the prior activity.
        forgot_cancel_button.setOnClickListener { this@ForgotPasswordActivity.finish() }

        // Hook up validator for email address and password.  In this case, we
        // do a minimal validation for the input as it will be checked by the
        // Amazon Cognito system as well.
        forgot_username.validate("Valid email address required") { s -> isUsernameValid(s) }

        // Now do the same for password.  We require a minimum length of 6 characters
        forgot_password.validate("Minimum 6 characters required") { s -> isPasswordValid(s) }

        // We only enable the login button when both the email address and password are both
        // valid.  To do this, we wire up an additional text listener on both to call the
        // checker
        forgot_username.afterTextChanged { checkSubmitEnabled() }
        forgot_password.afterTextChanged { checkSubmitEnabled() }

        // Wire up the form buttons
        forgot_password_button.setOnClickListener { handleForgotPassword() }

        // Call the checkSubmitEnabled to get into the right state
        checkSubmitEnabled()
    }

    /**
     * Checks the loginform_username and loginform_password.  If both of them are
     * valid, then enable the signin button
     */
    private fun checkSubmitEnabled() {
        forgot_password_button.isEnabled =
                isUsernameValid(forgot_username.text.toString())
                && isPasswordValid(forgot_password.text.toString())
    }

    /**
     * Checks to see if the username is valid
     */
    private fun isUsernameValid(s: String): Boolean = s.isValidEmail()

    /**
     * Checks to see if the password is valid
     */
    private fun isPasswordValid(s: String): Boolean = s.length >= 6

    /**
     * Handles the form submission event
     */
    @SuppressLint("InflateParams")
    private fun handleForgotPassword() {
        model.forgotPassword {
            request, params, callback -> when(request) {
            IdentityRequest.NEED_CREDENTIALS -> {
                Log.d(TAG, "NEED_CREDENTIALS")
                callback(mapOf("username" to forgot_username.text.toString(), "password" to forgot_password.text.toString()))
            }

            IdentityRequest.NEED_MULTIFACTORCODE -> {
                Log.d(TAG, "NEED_MULTIFACTORCODE")
                val mfaPromptDialog = layoutInflater.inflate(R.layout.dialog_mfa_prompt, null)
                val mfaCodeInput = mfaPromptDialog.find(R.id.mfa_prompt_input) as EditText
                val mfaInstructions = mfaPromptDialog.find(R.id.mfa_prompt_instructions) as TextView
                params?.let {
                    mfaInstructions.text = String.format(resources.getString(R.string.specific_mfa_instruction),
                            it["deliveryVia"] ?: "UNK", it["deliveryTo"] ?: "UNKNOWN")
                }
                alert {
                    title = "Multi-factor Code Required"
                    customView = mfaPromptDialog
                    positiveButton("OK") {
                        callback(mapOf("mfaCode" to mfaCodeInput.text.toString()))
                    }
                }.show()
            }

            IdentityRequest.SUCCESS -> {
                Log.d(TAG, "SUCCESS")
                model.updateStoredUsername(forgot_username.text.toString())
                this@ForgotPasswordActivity.finish()
            }

            IdentityRequest.FAILURE -> {
                Log.d(TAG, "FAILURE")
                alert(params?.get("message") ?: "Error submitting new credentials") {
                    title = "Password Reset Failed"
                    positiveButton("Close") { /* Do nothing */ }
                }.show()
            }

            else -> {
                Log.d(TAG, "Unexpected IdentityHandler callback")
                alert("We received an unexpected request from the backend service") {
                    title = "Unexpected request"
                    positiveButton("Close") { this@ForgotPasswordActivity.finish() }
                }
            }
        }
        }
    }
}