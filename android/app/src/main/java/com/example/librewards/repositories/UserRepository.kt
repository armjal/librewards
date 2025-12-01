package com.example.librewards.repositories

import com.example.librewards.hashFunction
import com.example.librewards.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference

class UserRepository(val database: DatabaseReference) {
    fun addUser(user: User): Task<Void?> {
        val id = hashFunction(user.email)
        return database.child("users").child(id).setValue(user)
    }

    fun getUser(email: String): Task<DataSnapshot?> {
        val id = hashFunction(email)
        return database.child("users").child(id).get()
    }
}