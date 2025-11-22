package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.ImageFile
import com.example.librewards.models.Product
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository
import com.example.librewards.repositories.StorageRepository
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRewardsViewModel(
    val productRepo: ProductRepository,
    val storageRepo: StorageRepository
) : ViewModel() {
    val productEntries: LiveData<List<ProductEntry>> = productRepo.productEntriesLiveData

    init {
        productRepo.startListeningForProducts()
    }

    override fun onCleared() {
        super.onCleared()
        productRepo.stopListeningForProducts()
    }

    fun uploadImage(imageFile: ImageFile): Flow<UiEvent> = flow {
        try {
            val uploadedImageData = storageRepo.uploadImage(imageFile).await()
            imageFile.downloadUrl = uploadedImageData.storage.downloadUrl.await()

            emit(UiEvent.Success("Image successfully uploaded"))

        } catch (e: StorageException) {
            emit(UiEvent.Failure("Failed to upload image: ${e.message}"))
        }
    }

    fun addProductEntry(product: Product): Flow<UiEvent> = flow {
        val productEntry = ProductEntry(
            generateProductId(),
            product
        )
        try {
            productRepo.addProductToDb(productEntry).await()
            emit(UiEvent.Success("Product successfully added"))

        } catch (e: Exception) {
            emit(UiEvent.Failure("Failed to add product: ${e.message}"))
        }
    }

    fun updateProductEntry(productEntry: ProductEntry): Flow<UiEvent> = flow {
        try {
            productRepo.updateProduct(productEntry).await()
            emit(UiEvent.Success("Product successfully updated"))

        } catch (e: Exception) {
            emit(UiEvent.Failure("Failed to update product: ${e.message}"))
        }
    }

    fun deleteProductEntry(productId: String): Flow<UiEvent> = flow {
        try {
            productRepo.deleteProduct(productId).await()
            emit(UiEvent.Success("Product successfully deleted"))

        } catch (e: Exception) {
            emit(UiEvent.Failure("Failed to delete product: ${e.message}"))
        }
    }
}

private fun generateProductId(): String {
    return "PROD-" + UUID.randomUUID().toString()
}

class AdminRewardsViewModelFactory(
    private val productRepo: ProductRepository,
    private val storageRepo: StorageRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(AdminRewardsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            AdminRewardsViewModel(productRepo, storageRepo) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}
