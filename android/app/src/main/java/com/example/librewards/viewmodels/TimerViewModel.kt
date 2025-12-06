package com.example.librewards.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository

class TimerViewModel(val userRepo: UserRepository) : ViewModel() {
    fun updateStudying(currentUser: User, studying: String) {
        val updatedUser = currentUser.copy(studying = studying)
        userRepo.updateUser(updatedUser)
    }
}

class TimerViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            TimerViewModel(userRepo) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}