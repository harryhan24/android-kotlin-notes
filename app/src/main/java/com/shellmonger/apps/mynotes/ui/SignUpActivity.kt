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
import kotlinx.android.synthetic.main.activity_signup.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import org.koin.android.architecture.ext.viewModel

class SignUpActivity : AppCompatActivity() {
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

        setContentView(R.layout.activity_signup)

        // We should be able to close this activity, in which case we go back
        // to the prior activity.
        signup_cancel_button.setOnClickListener { this@SignUpActivity.finish() }

        // Hook up validator for the fields
        sign_up_username.validate("Valid email address required") { s -> isUsernameValid(s) }
        sign_up_password.validate("Minimum 6 characters required") { s -> isPasswordValid(s) }
        sign_up_phone.validate("Valid phone number required") { s -> isPhoneValid(s) }
        sign_up_name.validate("A name must be entered") { s -> isNameValid(s) }

        // We only enable the login button when both the email address and password are both
        // valid.  To do this, we wire up an additional text listener on both to call the
        // checker
        sign_up_username.afterTextChanged { checkSubmitEnabled() }
        sign_up_password.afterTextChanged { checkSubmitEnabled() }
        sign_up_phone.afterTextChanged { checkSubmitEnabled() }
        sign_up_name.afterTextChanged { checkSubmitEnabled() }

        // Wire up the form buttons
        sign_up_button.setOnClickListener { handleSignup() }

        // Call the checkSubmitEnabled to get into the right state
        checkSubmitEnabled()
    }

    /**
     * Checks the loginform_username and loginform_password.  If both of them are
     * valid, then enable the signin button
     */
    private fun checkSubmitEnabled() {
        sign_up_button.isEnabled =
                isUsernameValid(sign_up_username.text.toString())
                && isPasswordValid(sign_up_password.text.toString())
                && isPhoneValid(sign_up_phone.text.toString())
                && isNameValid(sign_up_name.text.toString())
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
     * Checks to see if the full name is valid
     */
    private fun isNameValid(s: String): Boolean = s.isNotBlank()

    /**
     * Checks to see if the phone number is valid
     */
    private fun isPhoneValid(s: String): Boolean = s.isNotBlank()

    /**
     * Handles the form submission event
     */
    @SuppressLint("InflateParams")
    private fun handleSignup() {
        model.signUp {
            request, params, callback -> when(request) {
            IdentityRequest.NEED_SIGNUP -> {
                val attrs: MutableMap<String, String> = HashMap()
                attrs["username"] = sign_up_username.text.toString()
                attrs["password"] = sign_up_password.text.toString()
                attrs["phone"] = sign_up_phone.text.toString()
                attrs["name"] = sign_up_name.text.toString()
                callback(attrs)
            }

            IdentityRequest.NEED_MULTIFACTORCODE -> {
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
                toast("Sign up Successful")
                model.updateStoredUsername(sign_up_username.text.toString())
                this@SignUpActivity.finish()
            }

            IdentityRequest.FAILURE -> {
                alert(params?.get("message") ?: "Error submitting new credentials") {
                    title = "Sign Up Failed"
                    positiveButton("Close") { /* Do nothing */ }
                }.show()
            }

            else -> {
                Log.d(TAG, "Unexpected IdentityHandler callback")
                alert("We received an unexpected request from the backend service") {
                    title = "Unexpected request"
                    positiveButton("Close") { this@SignUpActivity.finish() }
                }
            }
        }
        }
    }
}