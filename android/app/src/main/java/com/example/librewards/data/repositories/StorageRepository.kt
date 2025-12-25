package com.example.librewards.data.repositories

import com.example.librewards.data.models.ImageFile
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class StorageRepository(val storageImagesRef: StorageReference) {
    lateinit var universityStorageRef: StorageReference

    fun setUniversityScope(university: String) {
        universityStorageRef = storageImagesRef.child(university)
    }

    fun uploadImage(imageFile: ImageFile): UploadTask {
        val imageRef = universityStorageRef.child("images/" + imageFile.name)
        return imageRef.putFile(imageFile.uri!!)
    }
}
