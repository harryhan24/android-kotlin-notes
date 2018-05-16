package com.shellmonger.apps.mynotes.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import com.shellmonger.apps.mynotes.R
import com.shellmonger.apps.mynotes.extensions.afterTextChanged
import com.shellmonger.apps.mynotes.extensions.isValidEmail
import com.shellmonger.apps.mynotes.extensions.set
import com.shellmonger.apps.mynotes.extensions.validate
import com.shellmonger.apps.mynotes.services.IdentityRequest
import com.shellmonger.apps.mynotes.viewmodels.AuthenticatorActivityViewModel
import kotlinx.android.synthetic.main.activity_authenticator.*
import org.jetbrains.anko.alert
import org.koin.android.architecture.ext.viewModel

class AuthenticatorActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val viewModel by viewModel<AuthenticatorActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticator)

        authenticator_username.validate("Valid email address required") { s-> isUsernameValid(s) }
        authenticator_username.afterTextChanged { checkLoginEnabled() }

        authenticator_password.validate("Minimum 6 characters required") { s -> isPasswordValid(s) }
        authenticator_password.afterTextChanged { checkLoginEnabled() }

        authenticator_sign_in_button.setOnClickListener { handleLogin() }
        authenticator_sign_up_button.setOnClickListener {
            startActivity(Intent(this@AuthenticatorActivity, SignUpActivity::class.java))
        }
        authenticator_forgot_password_button.setOnClickListener {
            startActivity(Intent(this@AuthenticatorActivity, ForgotPasswordActivity::class.java).apply {
                putExtra("login_username", authenticator_username.text.toString())
            })
        }

        checkLoginEnabled()
    }

    override fun onResume() {
        super.onResume()

        val storedUsername = viewModel.storedUsername.value ?: ""
        if (authenticator_username.text.isBlank() && storedUsername.isNotEmpty()) {
            authenticator_username.text.set(storedUsername)
            authenticator_password.text.clear()
            authenticator_password.requestFocus()
        }
    }

    private fun isUsernameValid(s: String) = s.isValidEmail()
    private fun isPasswordValid(s: String) = s.length >= 6

    private fun checkLoginEnabled() {
        authenticator_sign_in_button.isEnabled =
                isUsernameValid(authenticator_username.text.toString())
            &&  isPasswordValid(authenticator_password.text.toString())
    }

    @SuppressLint("InflateParams")
    private fun handleLogin() {
        viewModel.signIn {
            request, params, callback -> when(request) {
                IdentityRequest.NEED_CREDENTIALS -> {
                    callback(mapOf(
                            "username" to authenticator_username.text.toString(),
                            "password" to authenticator_password.text.toString()
                    ))
                }

                IdentityRequest.NEED_NEWPASSWORD -> {
                    val newPasswordDialog = layoutInflater.inflate(R.layout.dialog_new_password, null)
                    val passwordInput = newPasswordDialog.findViewById(R.id.new_password_input) as EditText
                    alert {
                        title = "Enter New Password"
                        customView = newPasswordDialog
                        positiveButton("OK") {
                            callback(mapOf("password" to passwordInput.text.toString()))
                        }
                    }.show()
                }

                IdentityRequest.NEED_MULTIFACTORCODE -> {
                    val mfaPromptDialog = layoutInflater.inflate(R.layout.dialog_mfa_prompt, null)
                    val mfaCodeInput = mfaPromptDialog.findViewById(R.id.mfa_prompt_input) as EditText
                    val mfaInstructions = mfaPromptDialog.findViewById(R.id.mfa_prompt_instructions) as TextView
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
                    viewModel.updateStoredUsername(authenticator_username.text.toString())
                    startActivity(Intent(this@AuthenticatorActivity, NoteListActivity::class.java))
                }

                IdentityRequest.FAILURE -> {
                    alert(params?.get("message") ?: "Error submitting credentials") {
                        title = "Login Denied"
                        positiveButton("Close") { /* Do nothing */ }
                    }.show()
                }

                else -> {
                    Log.d(TAG, "Unexpected IdentityHandler callback")
                    alert("We received an unexpected request from the backend service") {
                        title = "Unexpected request"
                        positiveButton("Close") { /* Do Nothing */ }
                    }
                }
            }
        }
    }
}