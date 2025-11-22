package com.example.librewards.repositories

import com.example.librewards.models.ProductEntry
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference

class ProductRepository(val database: DatabaseReference) {


    fun addProductToDb(productEntry: ProductEntry) {
        val productRef = database.child(productEntry.id)
        productRef.setValue(productEntry.product)
    }

    fun updateProduct(productEntry: ProductEntry): Task<Void> {
        val productRef = database.child(productEntry.id)
        return productRef.updateChildren(productEntry.product.toMap())
    }

    fun deleteProduct(productEntry: ProductEntry) {
        val productRef = database.child(productEntry.id)
        productRef.removeValue()
    }
}