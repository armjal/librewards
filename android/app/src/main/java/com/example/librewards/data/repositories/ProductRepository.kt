package com.example.librewards.data.repositories

import android.util.Log
import com.example.librewards.data.models.Product
import com.example.librewards.data.models.ProductEntry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository(private val database: DatabaseReference) {
    fun listenForProducts(): Flow<List<ProductEntry>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productEntries = mutableListOf<ProductEntry>()
                for (dataSnapshot in snapshot.children) {
                    val product = dataSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        val productEntry = ProductEntry()
                        productEntry.id = dataSnapshot.key!!
                        productEntry.product = product
                        productEntries.add(productEntry)
                    }
                }
                trySend(productEntries)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                close(error.toException())
            }
        }

        database.addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Removing products listener")
            database.removeEventListener(listener)
        }
    }

    suspend fun addProductToDb(productEntry: ProductEntry) {
        val productRef = database.child(productEntry.id)
        productRef.setValue(productEntry.product).await()
    }

    suspend fun updateProduct(productEntry: ProductEntry) {
        val productRef = database.child(productEntry.id)
        productRef.updateChildren(productEntry.product.toMap()).await()
    }

    suspend fun deleteProduct(productId: String) {
        val productRef = database.child(productId)
        productRef.removeValue().await()
    }

    companion object {
        const val TAG = "ProductRepository"
    }
}
