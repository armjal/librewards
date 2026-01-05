package com.example.librewards.ui.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth

sealed class RegisterStatus() {
    object Registered : RegisterStatus()

    object RegisteredWithoutLogin : RegisterStatus()

    object Failed : RegisterStatus()
}

class RegisterViewModel(val auth: FirebaseAuth, val userRepo: UserRepository) : ViewModel() {
    companion object {
        val TAG: String = RegisterViewModel::class.java.simpleName
    }

    private val _registerStatus = MutableLiveData<RegisterStatus>()
    val registerStatus: LiveData<RegisterStatus> = _registerStatus

    fun signUp(user: User, password: String) {
        auth.createUserWithEmailAndPassword(user.email, password).addOnCompleteListener {
            if (it.isSuccessful && auth.currentUser != null) {
                _registerStatus.value = RegisterStatus.Registered
                userRepo.addUser(user)
                Log.d(TAG, "createUserWithEmail:success")
            } else if (it.isSuccessful) {
                Log.d(TAG, "createUserWithEmail:success. Not logged in.")
                _registerStatus.value = RegisterStatus.RegisteredWithoutLogin
            } else {
                _registerStatus.value = RegisterStatus.Failed
                Log.w(TAG, "createUserWithEmail:failure", it.exception)
            }
        }
    }
}

class RegisterViewModelFactory(
    private val auth: FirebaseAuth,
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        RegisterViewModel(auth, userRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
