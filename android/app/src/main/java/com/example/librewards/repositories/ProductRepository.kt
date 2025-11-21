package com.example.librewards.repositories

import com.example.librewards.hashFunction
import com.example.librewards.models.Product
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference

class ProductRepository(val database: DatabaseReference) {


    fun addProductToDb(product: Product) {
        val productRef = database.child(hashFunction(product.productName))
        productRef.setValue(product)
    }

    fun updateProduct(product: Product): Task<Void> {
        val productRef = database.child(hashFunction(product.productName))
        return productRef.updateChildren(product.toMap())

    }


    fun deleteProduct(id: Int) {}
}