package com.example.librewards.data.repositories

import android.util.Log
import com.example.librewards.data.models.User
import com.example.librewards.utils.generateIdFromKey
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(val usersDbRef: DatabaseReference) {
    private val activeListeners = mutableMapOf<DatabaseReference, ValueEventListener>()

    companion object {
        val TAG: String = UserRepository::class.java.simpleName
    }

    fun addUser(user: User): Task<Void?> {
        val id = generateIdFromKey(user.email)
        return usersDbRef.child(id).setValue(user)
    }

    fun updateField(userId: String, field: String, value: String) {
        usersDbRef.child(userId).child(field).setValue(value)
    }

    suspend fun getUser(userId: String): User? = try {
        val dbValue = usersDbRef.child(userId).get().await()
        dbValue.getValue(User::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get user for id $userId: ${e.message}", e)
        null
    }

    fun listenForUserField(email: String, field: String): Flow<String?> = callbackFlow {
        val userId = generateIdFromKey(email)
        val userRef = usersDbRef.child(userId).child(field)

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
        activeListeners[userRef] = listener

        awaitClose {
            Log.d(TAG, "Removing $field listener for $email")
            userRef.removeEventListener(listener)
            activeListeners.remove(userRef)
        }
    }

    fun stopAllListeners() {
        Log.d(TAG, "Stopping all ${activeListeners.size} active listeners")
        activeListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
    }
}
