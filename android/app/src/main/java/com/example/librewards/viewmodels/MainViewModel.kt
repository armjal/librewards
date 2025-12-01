package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository

class MainViewModel(val userRepo: UserRepository): ViewModel() {
    private var _updatedUser = MutableLiveData<User>()
    val updatedUser: LiveData<User> get() = _updatedUser

    fun updatePoints(points: String){
        val user = _updatedUser.value
        user?.points = points
        userRepo.updateUser(user!!)
    }

    fun getUser(email: String){
        userRepo.getUser(email).addOnSuccessListener {
            _updatedUser.value = it?.getValue(User::class.java)
        }
    }
}

class MainViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            MainViewModel(userRepo) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}