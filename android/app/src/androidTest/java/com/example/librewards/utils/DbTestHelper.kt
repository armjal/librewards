package com.example.librewards.utils

import com.example.librewards.data.models.User
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

object DbTestHelper {
    fun createTestUser(email: String, firstname: String = "Test", surname: String = "User", university: String = "University of Bristol") {
        val db = FirebaseDatabase.getInstance()
        val usersRef = db.reference.child("users")

        // Use the helper to generate ID consistent with app logic
        val userId = generateIdFromKey(email)

        val user = User(
            firstname = firstname,
            surname = surname,
            email = email,
            university = university,
        )

        val task = usersRef.child(userId).setValue(user)
        Tasks.await(task, 10, TimeUnit.SECONDS)
    }

    fun deleteTestUser(email: String) {
        val db = FirebaseDatabase.getInstance()
        val usersRef = db.reference.child("users")
        val userId = generateIdFromKey(email)

        val task = usersRef.child(userId).removeValue()

        Tasks.await(task, 10, TimeUnit.SECONDS)
    }
}
