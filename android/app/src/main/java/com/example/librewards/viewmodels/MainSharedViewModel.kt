package com.example.librewards.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.Integer.parseInt

class MainSharedViewModel(val userRepo: UserRepository) : ViewModel() {
    companion object {
        val TAG: String = MainSharedViewModel::class.java.simpleName
    }

    private var _updatedUser = MutableLiveData<User>()
    val updatedUser: LiveData<User> get() = _updatedUser

    fun startListeningForUserUpdates(email: String) {
        userRepo.listenForUserUpdates(email)
            .onEach { updatedUser ->
                _updatedUser.value = updatedUser!!

            }
            .catch { error ->
                Log.e(TAG, "Error observing user: ${error.message}")
            }.launchIn(viewModelScope)
    }

    fun updatePoints(points: Int) {
        val updatedPoints = parseInt(_updatedUser.value?.points!!) + points
        _updatedUser.value?.points = updatedPoints.toString()
        userRepo.updateUser(_updatedUser.value!!)
    }
}

class MainViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(MainSharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            MainSharedViewModel(userRepo) as T
        } else {
            throw IllegalArgumentException("ViewModel Not Found")
        }
    }
}
