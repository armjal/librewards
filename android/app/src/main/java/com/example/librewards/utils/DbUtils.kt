package com.example.librewards.utils

import com.google.common.hash.Hashing
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.nio.charset.StandardCharsets

fun generateIdFromKey(key: String): String = Hashing.sipHash24().hashString(key, StandardCharsets.UTF_8)
    .toString()

fun getDbReference(collectionPath: String): DatabaseReference = FirebaseDatabase.getInstance().reference.child(collectionPath)
