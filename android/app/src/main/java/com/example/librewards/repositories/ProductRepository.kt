package com.example.librewards.repositories

import com.example.librewards.hashFunction
import com.example.librewards.models.Product
import com.google.firebase.database.DatabaseReference

class ProductRepository(val database: DatabaseReference) {


    fun addProductToDb(product: Product) {
        val productRef = database.child(hashFunction(product.productName))
        productRef.setValue(product)
    }

    fun updateProduct(product: Product?) {}

    fun deleteProduct(id: Int) {}
}