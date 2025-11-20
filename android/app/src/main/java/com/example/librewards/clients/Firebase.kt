package com.example.librewards.clients

import android.net.Uri
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class Firebase {
    class RealtimeDatabase {
        val db: DatabaseReference = FirebaseDatabase.getInstance().reference

        fun write(path: String, data: Any) {
            db.child(path).setValue(data)
        }

        fun delete(path: String) {
            db.child(path).removeValue()
        }
    }

    class Storage {
        val storage = FirebaseStorage.getInstance().reference

        fun upload(path: String, uri: Uri) {
            storage.child(path).putFile(uri)
        }
    }
}