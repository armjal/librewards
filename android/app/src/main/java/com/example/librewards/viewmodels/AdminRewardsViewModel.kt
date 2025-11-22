package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AdminRewardsViewModel(val productRepo: ProductRepository): ViewModel() {
    val productEntries: LiveData<List<ProductEntry>> = productRepo.productEntriesLiveData

    init {
        productRepo.startListeningForProducts()
    }

    override fun onCleared() {
        super.onCleared()
        productRepo.stopListeningForProducts()
    }

    fun updateProductEntry(productEntry: ProductEntry): Flow<UiEvent> = flow {
        try {
            productRepo.updateProduct(productEntry).await()
            emit(UiEvent.Success("Product successfully updated"))

        } catch (e : Exception) {
            emit(UiEvent.Failure("Failed to update product: ${e.message}"))
        }
    }

    fun deleteProductEntry(productId: String): Flow<UiEvent> = flow {
        try {
            productRepo.deleteProduct(productId).await()
            emit(UiEvent.Success("Product successfully deleted"))

        } catch (e : Exception) {
            emit(UiEvent.Failure("Failed to delete product: ${e.message}"))
        }
    }
}

class AdminRewardsViewModelFactory(private val repository: ProductRepository): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(AdminRewardsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            AdminRewardsViewModel(this.repository) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}
