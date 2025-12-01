package com.example.librewards

import com.google.common.hash.Hashing
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.nio.charset.StandardCharsets

class FirebaseHandler {
    fun getChild(document: String, email: String, path: String): DatabaseReference {
        val database = FirebaseDatabase.getInstance().reference
        val id = Hashing.sipHash24().hashString(email, StandardCharsets.UTF_8)
            .toString()
        return database.child(document).child(id).child(path)
    }
}

fun hashFunction(email: String): String {
    return Hashing.sipHash24().hashString(email, StandardCharsets.UTF_8)
        .toString()
}