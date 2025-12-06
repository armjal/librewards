package com.example.librewards.repositories

import com.example.librewards.models.ImageFile
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask

class StorageRepository(val storageImagesRef: StorageReference) {
    fun uploadImage(imageFile: ImageFile): UploadTask {
        val imageRef = storageImagesRef.child(imageFile.name)
        return imageRef.putFile(imageFile.uri!!)
    }
}
