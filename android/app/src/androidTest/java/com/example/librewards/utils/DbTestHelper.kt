package com.example.librewards.utils

import com.example.librewards.data.models.User
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

object DbTestHelper {
    fun createTestUser(
        email: String, firstname: String = "Test",
        surname: String = "User", university: String = "University of Bristol", points: String = "0",
    ) {
        val db = FirebaseDatabase.getInstance()
        val usersRef = db.reference.child("users")

        // Use the helper to generate ID consistent with app logic
        val userId = generateIdFromKey(email)

        val user = User(
            firstname = firstname,
            surname = surname,
            email = email,
            university = university,
            points = points,
        )

        val task = usersRef.child(userId).setValue(user)
        Tasks.await(task, 10, TimeUnit.SECONDS)
    }

    fun updateUserField(email: String, field: String, value: String) {
        val requestBody = JSONObject().apply {
            put("field", field)
            put("value", value)
        }
        updateUserFieldThroughLocalServer(generateIdFromKey(email), requestBody)
    }

    fun deleteTestUser(email: String) {
        val url = URL("http://10.0.2.2:8080/${generateIdFromKey(email)}/user")
        val serverResponse = LocalServerUtils.delete(url)
        if (serverResponse.status != 200) {
            throw RuntimeException("Local helper returned code ${serverResponse.status}")
        }
    }

    private fun updateUserFieldThroughLocalServer(uid: String, requestBody: JSONObject) {
        // Connect to local server running on host machine
        val url = URL("http://10.0.2.2:8080/$uid/update-user-field")
        val serverResponse = LocalServerUtils.post(url, requestBody)

        if (serverResponse.status != 200) {
            throw RuntimeException("Local helper returned code ${serverResponse.status}")
        }
    }
}
