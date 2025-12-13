package com.example.librewards.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

    private val _panelSlideOffset = MutableLiveData<Float>()
    val panelSlideOffset: LiveData<Float> = _panelSlideOffset

    private val _userEmailFlow = MutableStateFlow("")
    val userEmail: LiveData<String> = _userEmailFlow.asLiveData()

    val userPoints: LiveData<String> = _userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "points").map { it!! }
    }.asLiveData()

    val studyingStatus: LiveData<String> = _userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "studying").map { it!! }
    }.asLiveData()

    val redeemingRewardStatus: LiveData<String> = _userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "redeemingReward").map { it!! }
    }.asLiveData()

    fun onPanelSlide(offset: Float) {
        _panelSlideOffset.value = offset
    }

    fun startObservingUser(email: String) {
        Log.d(TAG, "Starting to observe user: $email")
        _userEmailFlow.value = email
    }

    fun addPoints(points: Int) {
        val updatedPoints = parseInt(userPoints.value!!) + points
        userRepo.updateField(_userEmailFlow.value, "points", updatedPoints.toString())
    }

    fun updatePoints(points: String) {
        userRepo.updateField(_userEmailFlow.value, "points", points)
    }

    fun updateStudying(studying: String) {
        userRepo.updateField(_userEmailFlow.value, "studying", studying)
    }

    fun updateRedeemingReward(points: String) {
        userRepo.updateField(_userEmailFlow.value, "redeemingReward", points)
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
