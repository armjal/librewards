package com.example.librewards.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.librewards.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.lang.Integer.parseInt

class MainSharedViewModel(val userRepo: UserRepository) : ViewModel() {
    companion object {
        val TAG: String = MainSharedViewModel::class.java.simpleName
    }

    private val userEmailFlow = MutableStateFlow("")

    val userPoints: LiveData<String> = userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "points").map { it!! }
    }.asLiveData()

    val studyingStatus: LiveData<String> = userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "studying").map { it!! }
    }.asLiveData()

    val redeemingRewardStatus: LiveData<String> = userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "redeemingReward").map { it!! }
    }.asLiveData()

    fun startObservingUser(email: String) {
        Log.d(TAG, "Starting to observe user: $email")
        userEmailFlow.value = email
    }

    fun addPoints(points: Int) {
        val updatedPoints = parseInt(userPoints.value!!) + points
        userRepo.updateField(userEmailFlow.value, "points", updatedPoints.toString())
    }

    fun updatePoints(points: String) {
        userRepo.updateField(userEmailFlow.value, "points", points)
    }

    fun updateStudying(studying: String) {
        userRepo.updateField(userEmailFlow.value, "studying", studying)
    }

    fun updateRedeemingReward(points: String) {
        userRepo.updateField(userEmailFlow.value, "redeemingReward", points)
    }
}

class MainViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(MainSharedViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        MainSharedViewModel(userRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
