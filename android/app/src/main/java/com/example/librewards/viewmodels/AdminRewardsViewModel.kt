package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository

class AdminRewardsViewModel(val productRepo: ProductRepository): ViewModel() {
    val productEntries: LiveData<List<ProductEntry>> = productRepo.productEntriesLiveData

    init {
        productRepo.startListeningForProducts()
    }

    override fun onCleared() {
        super.onCleared()
        productRepo.stopListeningForProducts()
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
}