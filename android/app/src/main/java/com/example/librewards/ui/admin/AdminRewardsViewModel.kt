package com.example.librewards.ui.admin

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.librewards.data.models.ImageFile
import com.example.librewards.data.models.Product
import com.example.librewards.data.models.ProductEntry
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.StorageRepository
import com.example.librewards.ui.main.UiEvent
import com.example.librewards.utils.generateIdFromKey
import com.google.firebase.database.DatabaseException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRewardsViewModel(
    val productRepo: ProductRepository,
    val storageRepo: StorageRepository,
) : ViewModel() {
    val productEntries: LiveData<List<ProductEntry>> = productRepo.listenForProducts().asLiveData()

    fun addProductEntry(product: Product, imageFilePath: Uri?): Flow<UiEvent> = flow {
        val imageFile = ImageFile(name = generateIdFromKey(product.productName), uri = imageFilePath)
        val productEntry = ProductEntry(generateProductId(), product)

        try {
            val uploadedImageDownloadUrl = uploadImage(imageFile)
            emit(UiEvent.Success("Image successfully uploaded"))

            product.productImageUrl = uploadedImageDownloadUrl

            productRepo.addProductToDb(productEntry)
            emit(UiEvent.Success("Product successfully added"))
        } catch (e: StorageException) {
            Log.e(TAG, "Failed to upload image: ${e.message}")
            emit(UiEvent.Failure("Failed to upload image"))
        } catch (e: DatabaseException) {
            Log.e(TAG, "Failed to add product to the database: ${e.message}")
            emit(UiEvent.Failure("Failed to add product to the database"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add product: ${e.message}")
            emit(UiEvent.Failure("Failed to add product"))
        }
    }

    private suspend fun uploadImage(imageFile: ImageFile): String {
        val uploadedImageData = storageRepo.uploadImage(imageFile).await()
        return uploadedImageData.storage.downloadUrl.await().toString()
    }

    fun updateProductEntry(productEntry: ProductEntry): Flow<UiEvent> = flow {
        try {
            productRepo.updateProduct(productEntry)
            emit(UiEvent.Success("Product successfully updated"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update product: ${e.message}")
            emit(UiEvent.Failure("Failed to update product"))
        }
    }

    fun deleteProductEntry(productId: String): Flow<UiEvent> = flow {
        try {
            productRepo.deleteProduct(productId)
            emit(UiEvent.Success("Product successfully deleted"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete product: ${e.message}")
            emit(UiEvent.Failure("Failed to delete product"))
        }
    }

    companion object {
        private val TAG: String = AdminRewardsViewModel::class.java.simpleName
    }
}

private fun generateProductId(): String = "PROD-" + UUID.randomUUID().toString()

class AdminRewardsViewModelFactory(
    private val productRepo: ProductRepository,
    private val storageRepo: StorageRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(AdminRewardsViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        AdminRewardsViewModel(productRepo, storageRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
