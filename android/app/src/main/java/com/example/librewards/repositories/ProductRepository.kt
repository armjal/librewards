package com.example.librewards.repositories

import com.example.librewards.clients.Firebase
import com.example.librewards.models.Product

class ProductRepository(database: Firebase.RealtimeDatabase, storage: Firebase.Storage) {
    val database = Firebase.RealtimeDatabase()
    val storage = Firebase.Storage()


    fun addProduct(product: Product?) {
    }

    fun updateProduct(product: Product?) {}

    fun deleteProduct(id: Int) {}
}