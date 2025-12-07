package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.ProductEntry
import com.example.librewards.models.User
import com.example.librewards.repositories.ProductRepository
import com.example.librewards.repositories.UserRepository
import java.lang.Integer.parseInt

class RewardsViewModel(val userRepo: UserRepository, val productRepo: ProductRepository) : ViewModel() {
    val productEntries: LiveData<List<ProductEntry>> = productRepo.productEntriesLiveData

    init {
        productRepo.startListeningForProducts()
    }

    override fun onCleared() {
        super.onCleared()
        productRepo.stopListeningForProducts()
    }

    fun minusPoints(user: User, points: Int) {
        val finalPoints = parseInt(user.points) - points
        val updatedUser = user.copy(points = finalPoints.toString())
        userRepo.updateUser(updatedUser)
    }
}

class RewardsViewModelFactory(
    private val userRepo: UserRepository, private val productRepo: ProductRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        RewardsViewModel(userRepo, productRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
