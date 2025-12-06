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

class UserRepository(val database: DatabaseReference) {
    companion object {
        val TAG: String = UserRepository::class.java.simpleName
    }
    fun addUser(user: User): Task<Void?> {
        val id = hashFunction(user.email)
        return database.child("users").child(id).setValue(user)
    }

    fun updateUser(user: User): Task<Void?> {
        val id = hashFunction(user.email)
        return database.child("users").child(id).updateChildren(user.toMap())
    }

    fun listenForUserUpdates(email: String): Flow<User?> = callbackFlow {
        val userId = hashFunction(email)
        val userRef = database.child("users").child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error for user listener: ${error.message}")
                close(error.toException())
            }
        }

        userRef.addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Removing user listener for $email")
            userRef.removeEventListener(listener)
        }
    }
}