package com.example.librewards.ui.admin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.ui.main.MainSharedViewModel.Companion.TAG
import com.example.librewards.utils.generateIdFromKey
import kotlinx.coroutines.launch

class AdminViewModel(val userRepo: UserRepository) : ViewModel() {
    private var _user = MutableLiveData<User>(null)
    val user: LiveData<User> = _user

    fun setUser(email: String) {
        val id = generateIdFromKey(email)
        viewModelScope.launch {
            try {
                _user.postValue(userRepo.getUser(id))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get user: ${e.message}")
            }
        }
    }
}

class AdminViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        AdminViewModel(userRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
