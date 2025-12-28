package com.example.librewards.utils

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
}
