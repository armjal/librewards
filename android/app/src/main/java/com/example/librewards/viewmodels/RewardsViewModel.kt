package com.example.librewards.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository
import java.lang.Integer.parseInt

class RewardsViewModel(val userRepo: UserRepository) : ViewModel() {
    fun minusPoints(user: User, points: Int) {
        val finalPoints = parseInt(user.points) - points
        val updatedUser = user.copy(points = finalPoints.toString())
        userRepo.updateUser(updatedUser)
    }
}

class RewardsViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        RewardsViewModel(userRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
