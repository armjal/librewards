package com.example.librewards.ui.admin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.StorageRepository
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.ui.main.MainSharedViewModel.Companion.TAG
import com.example.librewards.utils.generateIdFromKey
import kotlinx.coroutines.launch

class AdminSharedViewModel(
    val userRepo: UserRepository,
    val productRepo: ProductRepository,
    val storageRepo: StorageRepository,
) : ViewModel() {
    private var _user = MutableLiveData<User>(null)
    val user: LiveData<User> = _user

    fun initialiseStateOnUserRetrieval(email: String) {
        val id = generateIdFromKey(email)
        viewModelScope.launch {
            val user = userRepo.getUser(id)
            if (user == null) {
                Log.e(TAG, "Failed to get user for id $id")
                return@launch
            }
            _user.postValue(user)
            productRepo.setUniversityScope(user.university)
            storageRepo.setUniversityScope(user.university)
        }
    }

    fun stopListeningToData() {
        productRepo.stopAllListeners()
    }
}

class AdminSharedViewModelFactory(
    private val userRepo: UserRepository,
    private val productRepo: ProductRepository,
    private val storageRepo: StorageRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(AdminSharedViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        AdminSharedViewModel(userRepo, productRepo, storageRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
