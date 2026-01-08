package com.example.librewards.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AuthTestHelper {
    fun createUser(email: String, password: String): FirebaseUser? {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            auth.signOut()
        }

        val task = auth.createUserWithEmailAndPassword(email, password)
        val result = Tasks.await(task, 20, TimeUnit.SECONDS)
        return result.user
    }

    fun deleteAuth(email: String) {
        val path = "/$email/auth"
        val serverResponse = LocalServerUtils.delete(path)
        if (serverResponse.status != 200) {
            throw RuntimeException("Delete Auth: Local helper returned code ${serverResponse.status}")
        }
    }

    fun setUserAsAdmin(email: String) {
        try {
            val customToken = getCustomTokenFromLocalHelper(email)

            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                auth.signOut()
            }

            Tasks.await(auth.signInWithCustomToken(customToken), 30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to set admin via local server. Ensure :local-admin-server is running and emulator has internet.",
                e,
            )
        }
    }

    private fun getCustomTokenFromLocalHelper(email: String): String {
        val path = "/generate-token-for-admin?email=$email"
        val serverResponse = LocalServerUtils.post(path)
        if (serverResponse.status != 200) {
            throw RuntimeException("Get Custom Token: Local helper returned code ${serverResponse.status}")
        }
        return JSONObject(serverResponse.message).getString("customToken")
    }
}
