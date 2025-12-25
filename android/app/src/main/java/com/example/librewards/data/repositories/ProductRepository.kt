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

class ProductRepository(private val productDbRef: DatabaseReference) {
    private val activeListeners = mutableMapOf<DatabaseReference, ValueEventListener>()
    lateinit var universityRef: DatabaseReference

    fun setUniversityScope(university: String) {
        universityRef = productDbRef.child(university)
    }

    companion object {
        val TAG: String = ProductRepository::class.java.simpleName
    }

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

        universityRef.addValueEventListener(listener)
        activeListeners[universityRef] = listener

        awaitClose {
            Log.d(TAG, "Removing products listener")
            universityRef.removeEventListener(listener)
            activeListeners.remove(universityRef)
        }
    }

    suspend fun addProductToDb(productEntry: ProductEntry) {
        val productRef = universityRef.child(productEntry.id)
        productRef.setValue(productEntry.product).await()
    }

    suspend fun updateProduct(productEntry: ProductEntry) {
        val productRef = universityRef.child(productEntry.id)
        productRef.updateChildren(productEntry.product.toMap()).await()
    }

    suspend fun deleteProduct(productId: String) {
        val productRef = universityRef.child(productId)
        productRef.removeValue().await()
    }

    fun stopAllListeners() {
        Log.d(TAG, "Stopping all ${activeListeners.size} active listeners")
        activeListeners.forEach { (ref, listener) ->
            ref.removeEventListener(listener)
        }
        activeListeners.clear()
    }
}
