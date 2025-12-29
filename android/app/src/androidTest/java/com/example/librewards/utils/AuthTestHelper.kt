package com.example.librewards.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

object AuthTestHelper {
    fun createUser(email: String, password: String): FirebaseUser? {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            auth.signOut()
        }

        val task = auth.createUserWithEmailAndPassword(email, password)
        val result = Tasks.await(task, 10, TimeUnit.SECONDS)
        return result.user
    }

    fun deleteUser() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        if (user != null) {
            try {
                Tasks.await(user.delete(), 10, TimeUnit.SECONDS)
            } catch (e: Exception) {
                // Failed to delete, just sign out
            }
        }

        // Ensure we are signed out
        if (auth.currentUser != null) {
            auth.signOut()
        }
    }

    fun setUserAsAdmin(email: String) {
        try {
            val customToken = getCustomTokenFromLocalHelper(email)

            // Sign in with the custom token to apply the admin claim
            val auth = FirebaseAuth.getInstance()
            Tasks.await(auth.signInWithCustomToken(customToken), 10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw RuntimeException("Failed to set admin via local server. Ensure :local-admin-server is running.", e)
        }
    }

    private fun getCustomTokenFromLocalHelper(email: String): String {
        // Connect to local server running on host machine
        val url = URL("http://10.0.2.2:8080/generate-token-for-admin?email=$email")
        val serverResponse = LocalServerUtils.post(url)
        return JSONObject(serverResponse.message).getString("customToken")
    }
}
