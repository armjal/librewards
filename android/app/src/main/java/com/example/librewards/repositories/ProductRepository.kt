package com.example.librewards.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.librewards.models.Product
import com.example.librewards.models.ProductEntry
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class ProductRepository(val database: DatabaseReference) {
    private val _productEntriesLiveData = MutableLiveData<List<ProductEntry>>()
    val productEntriesLiveData: LiveData<List<ProductEntry>> = _productEntriesLiveData
    private val productsValueEventListener: ValueEventListener

    init {
        productsValueEventListener = createProductsValueEventListener()
    }

    fun startListeningForProducts() {
        database.addValueEventListener(productsValueEventListener)
    }

    fun stopListeningForProducts() {
        database.removeEventListener(productsValueEventListener)
    }

    fun addProductToDb(productEntry: ProductEntry) {
        val productRef = database.child(productEntry.id)
        productRef.setValue(productEntry.product)
    }

    fun updateProduct(productEntry: ProductEntry): Task<Void> {
        val productRef = database.child(productEntry.id)
        return productRef.updateChildren(productEntry.product.toMap())
    }

    fun deleteProduct(productId: String): Task<Void> {
        val productRef = database.child(productId)
        return productRef.removeValue()
    }

    private fun createProductsValueEventListener(): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productEntries = mutableListOf<ProductEntry>()
                for (dataSnapshot in snapshot.children) {
                    val productEntry = ProductEntry()
                    productEntry.id = dataSnapshot.key!!
                    productEntry.product = dataSnapshot.getValue(Product::class.java)!!
                    productEntries.add(productEntry)
                    _productEntriesLiveData.postValue(productEntries)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
            }
        }
    }

    companion object {
        const val TAG = "ProductRepository"
    }
}