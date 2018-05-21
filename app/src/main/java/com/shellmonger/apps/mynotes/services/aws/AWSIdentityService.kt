package com.shellmonger.apps.mynotes.services.aws

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.amazonaws.mobile.config.AWSConfiguration
import com.amazonaws.mobileconnectors.cognitoidentityprovider.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.*
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.*
import com.shellmonger.apps.mynotes.models.TokenType
import com.shellmonger.apps.mynotes.models.User
import com.shellmonger.apps.mynotes.services.IdentityHandler
import com.shellmonger.apps.mynotes.services.IdentityRequest
import com.shellmonger.apps.mynotes.services.IdentityService
import java.lang.Exception

class AWSIdentityService(context: Context) : IdentityService {
    companion object {
        private val TAG = this::class.java.simpleName
        private val DO_NOTHING: (Map<String,String>?) -> Unit = { /* Do Nothing */ }
        private val PREFS_FILE = "${this::class.java.canonicalName}.PREFS"
        private val USERNAME_PREF = "authenticator-username"
    }

    private val mutableCurrentUser: MutableLiveData<User> = MutableLiveData()
    private val mutableStoredUsername: MutableLiveData<String?> = MutableLiveData()
    private val sharedPreferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    val userPool: CognitoUserPool

    init {
        Log.d(TAG, "init")
        mutableCurrentUser.value = null // Initially, signed out

        val awsConfig = AWSConfiguration(context)
        userPool = CognitoUserPool(context, awsConfig)

        // Check to see if we have a current session
        userPool.currentUser.getSessionInBackground(object : AuthenticationHandler {
            override fun onSuccess(userSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                userSession?.let {
                    mutableCurrentUser.value = User().apply {
                        username = it.username
                        tokens[TokenType.ACCESS_TOKEN] = it.accessToken.jwtToken
                        tokens[TokenType.ID_TOKEN] = it.idToken.jwtToken
                        tokens[TokenType.REFRESH_TOKEN] = it.refreshToken.token
                    }
                }
            }

            override fun onFailure(exception: Exception?) { }
            override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) { }
            override fun authenticationChallenge(continuation: ChallengeContinuation?) { }
            override fun getMFACode(continuation: MultiFactorAuthenticationContinuation?) { }
        })

        mutableStoredUsername.value = sharedPreferences.getString(USERNAME_PREF, null)
    }

    override val currentUser: LiveData<User?> = mutableCurrentUser
    override val storedUsername: LiveData<String?> = mutableStoredUsername

    /**
     * Store the user session into the currentUser object
     */
    private fun storeUserSession(handler: IdentityHandler, userSession: CognitoUserSession) {
        Log.d(TAG, "storeUserSession")
        val user = User().apply {
            username = userSession.username
            tokens[TokenType.ACCESS_TOKEN] = userSession.accessToken.jwtToken
            tokens[TokenType.ID_TOKEN] = userSession.idToken.jwtToken
            tokens[TokenType.REFRESH_TOKEN] = userSession.refreshToken.token
        }
        userPool.currentUser.getDetailsInBackground(object : GetDetailsHandler {
            /**
             * This method is called on successfully fetching user attributes.
             * `attributesList` contains all attributes set for the user.
             *
             * @param cognitoUserDetails contains the users' details retrieved from the Cognito Service
             */
            override fun onSuccess(cognitoUserDetails: CognitoUserDetails?) = runOnUiThread {
                if (cognitoUserDetails != null) {
                    val userAttributes = cognitoUserDetails.attributes.attributes
                    for (entry in userAttributes) user.userAttributes[entry.key] = entry.value
                    mutableCurrentUser.value = user
                    handler(IdentityRequest.SUCCESS, null, DO_NOTHING)
                } else {
                    handleFailure(handler, "Success with no details")
                }
            }

            /**
             * This method is called upon encountering errors during this operation.
             * Probe `exception` for the cause of this exception.
             *
             * @param exception REQUIRED: Failure details.
             */
            override fun onFailure(exception: Exception?) = runOnUiThread {
                handleFailure(handler, "Unkown error while getting user details")
            }
        })
    }

    /**
     * Update the username that is stored in SharedPreferences
     */
    override fun updateStoredUsername(username: String?) {
        Log.d(TAG, "updateStoredUsername")
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

    /**
     * Sign in with a username / password
     */
    override fun signIn(handler: IdentityHandler) {
        Log.d(TAG, "signIn")
        try {
            userPool.currentUser.getSessionInBackground(object : AuthenticationHandler {
                /**
                 * This method is called to deliver valid tokens, when valid tokens were locally
                 * available (cached) or after successful completion of the authentication process.
                 * The `newDevice` will is an instance of [CognitoDevice] for this device, and this
                 * parameter will be not null during these cases:
                 * 1- If the user pool allows devices to be remembered and this is is a new device, that is
                 * first time authentication on this device.
                 * 2- When the cached device key is lost and, hence, the service identifies this as a new device.
                 *
                 * @param nullableUserSession [CognitoUserSession?]  Contains valid user tokens.
                 * @param newDevice           [CognitoDevice], will be null if this is not a new device.
                 */
                override fun onSuccess(nullableUserSession: CognitoUserSession?, newDevice: CognitoDevice?) {
                    val userSession = checkNotNull(nullableUserSession) { "user session is null" }
                    storeUserSession(handler, userSession)
                    runOnUiThread { handler(IdentityRequest.SUCCESS, null, DO_NOTHING) }
                }

                /**
                 * This method is called when a fatal exception was encountered during
                 * authentication. The current authentication process continue because of the error
                 * , hence a continuation is not available. Probe `exception` for details.
                 *
                 * @param exception is this Exception leading to authentication failure.
                 */
                override fun onFailure(exception: Exception?) {
                    handleFailure(handler, exception?.message)
                }

                /**
                 * Call out to the dev to get the credentials for a user.
                 *
                 * @param authenticationContinuation is a [AuthenticationContinuation] object that should
                 * be used to continue with the authentication process when
                 * the users' authentication details are available.
                 * @param userId                     Is the user-ID (username  or alias) used in authentication.
                 * This will be null if the user ID is not available.
                 */
                override fun getAuthenticationDetails(authenticationContinuation: AuthenticationContinuation?, userId: String?) {
                    val continuation = checkNotNull(authenticationContinuation) { "Invalid authentication continuation" }

                    runOnUiThread {
                        handler(IdentityRequest.NEED_CREDENTIALS, null) { nResponse ->
                            run {
                                val response = checkNotNull(nResponse) { "Invalid identity response" }
                                val username = (response["username"] ?: "").replace('@','_').replace('.','_')
                                val password = response["password"] ?: ""
                                check(username.isNotEmpty()) { "Username is empty" }
                                check(password.isNotEmpty()) { "Password is empty" }
                                continuation.setAuthenticationDetails(AuthenticationDetails(username, password, null))
                                continuation.continueTask()
                            }
                        }
                    }
                }

                /**
                 * Call out to the dev to respond to a challenge.
                 * The authentication process as presented the user with the a challenge, to successfully authenticate.
                 * This a generic challenge, that is not MFA or user password verification.
                 *
                 * @param nContinuation contains details about the challenge and allows dev to respond to the
                 * challenge.
                 */
                override fun authenticationChallenge(nContinuation: ChallengeContinuation?) {
                    val continuation = checkNotNull(nContinuation) { "Invalid challenge authentication" }
                    when (continuation.challengeName) {
                        "NEW_PASSWORD_REQUIRED" -> {
                            runOnUiThread {
                                handler(IdentityRequest.NEED_NEWPASSWORD, null) { nResponse ->
                                    run {
                                        val response = checkNotNull(nResponse) { "Invalid new password response" }
                                        continuation.parameters["NEW_PASSWORD"] = response["password"] ?: ""
                                        continuation.continueTask()
                                    }
                                }
                            }
                        }

                        else -> { handleFailure(handler, "Unknown authentication challenge") }
                    }
                }

                /**
                 * Call out to the dev to send MFA code.
                 * MFA code would have been sent via the deliveryMethod before this is invoked.
                 * This callback can be invoked in two scenarios -
                 * 1)  MFA verification is required and only one possible MFA delivery medium is
                 * available.
                 * 2)  MFA verification is required and a MFA delivery medium was successfully set.
                 * 3)  An MFA code sent earlier was incorrect and at-least one more attempt to send
                 * MFA code is available.
                 *
                 * @param nContinuation medium through which the MFA will be delivered
                 */
                override fun getMFACode(nContinuation: MultiFactorAuthenticationContinuation?) {
                    val continuation = checkNotNull(nContinuation) { "Invalid continuation token" }
                    runOnUiThread {
                        handler(IdentityRequest.NEED_MULTIFACTORCODE, null) { nResponse ->
                            run {
                                val response = checkNotNull(nResponse) { "Invalid MFA response" }
                                continuation.setMfaCode(response["mfaCode"] ?: "")
                                continuation.continueTask()
                            }
                        }
                    }
                }
            })
        } catch (exception: Exception) {
            handleFailure(handler, "Validation error")
        }
    }

    /**
     * Sign out of the system
     */
    override fun signOut(handler: IdentityHandler) {
        userPool.currentUser.signOut()
        mutableCurrentUser.value = null
        handler(IdentityRequest.SUCCESS, null, DO_NOTHING)
    }

    /**
     * Initiate the forgot password flow
     */
    override fun forgotPassword(handler: IdentityHandler)= runOnUiThread {
        handler(IdentityRequest.NEED_CREDENTIALS, null) { response -> fpHasCredentials(handler, response) }
    }

    /**
     * Initiate Flow: Sign up
     *
     * @param handler the Identity handler within the UI
     */
    override fun signUp(handler: IdentityHandler) = runOnUiThread {
        handler(IdentityRequest.NEED_SIGNUP, null) { response -> suHasInformation(handler, response) }
    }

    /**
     * Handle the response from the forgot password flow when we have credentials
     */
    private fun fpHasCredentials(handler: IdentityHandler, nResponse: Map<String,String>?) {
        try {
            // response validation
            val response: Map<String,String> = checkNotNull(nResponse) { "Invalid response when requesting new credentials" }
            check(response["username"]?.isNotEmpty() ?: false) { "Username must be specified" }
            check(response["password"]?.isNotEmpty() ?: false) { "New password must be specified" }

            // Call the forgotPassword flow on a background thread
            userPool.getUser(response["username"]).forgotPasswordInBackground(object : ForgotPasswordHandler {
                /**
                 * This is called after successfully setting new password for a user.
                 * The new password can new be used to authenticate this user.
                 */
                override fun onSuccess() {
                    runOnUiThread { handler(IdentityRequest.SUCCESS, null, DO_NOTHING) }
                }

                /**
                 * This is called for all fatal errors encountered during the password reset process
                 * Probe {@exception} for cause of this failure.
                 * @param exception REQUIRED: Contains failure details.
                 */
                override fun onFailure(exception: Exception?) {
                    handleFailure(handler, exception?.message ?: "Unknown error")
                }

                /**
                 * A code may be required to confirm and complete the password reset process
                 * Supply the new password and the confirmation code - which was sent through email/sms
                 * to the continuation
                 * @param continuation REQUIRED: Continuation to the next step.
                 */
                override fun getResetCode(continuation: ForgotPasswordContinuation?) {
                    runOnUiThread {
                        val delivery = checkNotNull(continuation?.parameters) { "Invalid continuation token" }
                        handler(IdentityRequest.NEED_MULTIFACTORCODE, mapOf("deliveryVia" to delivery.deliveryMedium, "deliveryTo" to delivery.destination)) {
                            nCodeResponse -> run {
                            val mfaResponse = checkNotNull(nCodeResponse) { "Invalid response when requesting MFA code" }
                            with (continuation!!) {
                                setPassword(response["password"]!!)
                                setVerificationCode(mfaResponse["mfaCode"]!!)
                                continueTask()
                            }
                        }
                        }
                    }
                }
            })
        } catch (exception: Exception) {
            handleFailure(handler, exception.message)
        }
    }

    /**
     * Handles the sign-up flow once we have information
     */
    private fun suHasInformation(handler: IdentityHandler, nResponse: Map<String, String>?) {
        try {
            val response = checkNotNull(nResponse) { "Invalid response from NEED_SIGNUP" }

            val emailaddr = response["username"] ?: ""
            val password = response["password"] ?: ""
            val phone = response["phone"] ?: ""
            val name = response["name"] ?: ""
            check(emailaddr.isNotEmpty()) { "Email Address is empty" }
            check(password.isNotEmpty()) { "Password is empty" }
            check(phone.isNotEmpty()) { "Phone is empty" }
            check(name.isNotEmpty()) { "Name is empty" }
            val username = emailaddr.replace('@', '_').replace('.', '_')

            // See https://docs.aws.amazon.com/cognito/latest/developerguide/user-pool-settings-attributes.html
            // for a list of user attributes
            val userAttributes = CognitoUserAttributes()
            userAttributes.addAttribute("phone_number", phone)
            userAttributes.addAttribute("email", emailaddr)
            userAttributes.addAttribute("name", name)
            userPool.signUpInBackground(username, password, userAttributes, null, object : SignUpHandler {
                /**
                 * This method is called successfully registering a new user.
                 * Confirming the user may be required to activate the users account.
                 *
                 * @param user [CognitoUser]
                 * @param state will be `true` is the user has been confirmed, otherwise `false`.
                 * @param nDetails REQUIRED: Indicates the medium and destination of the confirmation code.
                 */
                override fun onSuccess(user: CognitoUser?, state: Boolean, nDetails: CognitoUserCodeDeliveryDetails?) = runOnUiThread {
                    if (state) {
                        // We don't need to confirm our identity
                        handler(IdentityRequest.SUCCESS, null, DO_NOTHING)
                    } else {
                        // We've sent a code somewhere
                        val details = checkNotNull(nDetails) { "Invalid destination details" }
                        handler(IdentityRequest.NEED_MULTIFACTORCODE, mapOf("deliveryVia" to details.deliveryMedium, "deliveryTo" to details.destination)) { response ->
                            suHasConfirmationCode(handler, user!!, response)
                        }
                    }
                }

                /**
                 * This method is called when user registration has failed.
                 * Probe `exception` for cause of the failure.
                 *
                 * @param exception REQUIRED: Failure details.
                 */
                override fun onFailure(exception: Exception?) = runOnUiThread {
                    handleFailure(handler, exception?.message)
                }
            })
        } catch (exception: Exception) {
            handleFailure(handler, exception.message)
        }
    }

    /**
     * Handles the sign-up flow once we have the verification code
     */
    private fun suHasConfirmationCode(handler: IdentityHandler, user: CognitoUser, nResponse: Map<String, String>?) {
        try {
            val response = checkNotNull(nResponse) { "invalid MFA code response" }
            val mfaCode = response["mfaCode"] ?: ""
            user.confirmSignUpInBackground(mfaCode, false, object : GenericHandler {
                /**
                 * This callback method is invoked when the call has successfully completed.
                 */
                override fun onSuccess() = runOnUiThread { handler(IdentityRequest.SUCCESS, null, DO_NOTHING) }

                /**
                 * This callback method is invoked when call has failed. Probe `exception` for cause.
                 *
                 * @param exception REQUIRED: Failure details.
                 */
                override fun onFailure(exception: Exception?) = handleFailure(handler, exception?.message)
            })
        } catch (exception: Exception) {
            handleFailure(handler, exception.message)
        }
    }

    /**
     * Handles failure cases
     */
    private fun handleFailure(handler: IdentityHandler, message: String?) {
        runOnUiThread { handler(IdentityRequest.FAILURE, mapOf("message" to (message ?: "Unknown error")), DO_NOTHING) }
    }

}