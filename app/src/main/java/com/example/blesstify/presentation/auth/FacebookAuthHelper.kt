package com.example.blesstify.presentation.auth

import android.app.Activity
import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

/**
 * Small helper for Facebook Login integration.
 *
 * Usage:
 * - Call [login] from UI with Activity.
 * - Forward Activity.onActivityResult to [onActivityResult].
 */
object FacebookAuthHelper {
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    fun login(
        activity: Activity,
        onToken: (token: String) -> Unit,
        onCancel: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    onToken(result.accessToken.token)
                }

                override fun onCancel() {
                    onCancel()
                }

                override fun onError(error: FacebookException) {
                    onError(error)
                }
            }
        )

        val loginManager = LoginManager.getInstance()

        // Force chooser-like behavior by avoiding direct app switch where possible.
        loginManager.logOut()
        loginManager.setLoginBehavior(LoginBehavior.WEB_ONLY)

        // Basic permission set. Add more if you need.
        loginManager.logInWithReadPermissions(activity, listOf("public_profile"))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
