package com.example.librewards.repositories

import android.util.Log
import com.example.librewards.hashFunction
import com.example.librewards.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(val database: DatabaseReference) {
    companion object {
        val TAG: String = UserRepository::class.java.simpleName
    }

    fun addUser(user: User): Task<Void?> {
        val id = hashFunction(user.email)
        return database.child("users").child(id).setValue(user)
    }

    fun updateField(email: String, field: String, value: String) {
        val id = hashFunction(email)
        database.child("users").child(id).child(field).setValue(value)
    }

    suspend fun getUser(email: String): User? {
        val id = hashFunction(email)
        return try {
            val dbValue = database.child("users").child(id).get().await()
            dbValue.getValue(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user for email $email: ${e.message}")
            null
        }
    }

    fun listenForUserField(email: String, field: String): Flow<String?> = callbackFlow {
        val userId = hashFunction(email)
        val userRef = database.child("users").child(userId).child(field)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.value as String)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error for '$field' listener: ${error.message}")
                close(error.toException())
            }
        }

        userRef.addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Removing $field listener for $email")
            userRef.removeEventListener(listener)
        }
    }
}
