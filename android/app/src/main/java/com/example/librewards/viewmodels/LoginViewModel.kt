package com.example.librewards.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class LoginStatus {
    object Successful : LoginStatus()

    object Failed : LoginStatus()
}

class LoginViewModel(val auth: FirebaseAuth) : ViewModel() {
    companion object {
        val TAG: String = LoginViewModel::class.java.simpleName
    }

    fun login(email: String, password: String): Flow<LoginStatus> = callbackFlow {
        auth.signInWithEmailAndPassword(
            email, password,
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                trySend(LoginStatus.Successful)
                Log.d(TAG, "signInWithEmail:success")
            } else {
                trySend(LoginStatus.Failed)
                Log.e(TAG, "signInWithEmail:failure", task.exception)
            }
        }
        awaitClose {
        }
    }
}

class LoginViewModelFactory(
    private val auth: FirebaseAuth,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        LoginViewModel(auth) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
