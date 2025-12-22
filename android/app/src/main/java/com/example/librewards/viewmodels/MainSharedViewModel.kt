package com.example.librewards.viewmodels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.librewards.hashFunction
import com.example.librewards.models.User
import com.example.librewards.qrcode.QRCodeGenerator
import com.example.librewards.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.Integer.parseInt

data class UserQRCode(
    val bitmap: Bitmap,
    val number: String,
)

class MainSharedViewModel(val userRepo: UserRepository) : ViewModel() {
    companion object {
        val TAG: String = MainSharedViewModel::class.java.simpleName
    }

    private val _panelSlideOffset = MutableLiveData<Float>()
    val panelSlideOffset: LiveData<Float> = _panelSlideOffset

    private val _userEmailFlow = MutableStateFlow("")
    private var _userQrCode = MutableLiveData<UserQRCode>(null)
    val userQrCode: LiveData<UserQRCode> = _userQrCode

    private var _user = MutableLiveData<User>(null)
    val user: LiveData<User> = _user

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
        setUser(email)
    }

    fun createQRCode() {
        val qrCodeGenerator = QRCodeGenerator()
        val qrCodeNumber = hashFunction(_userEmailFlow.value)
        val qrCodeBitmap = qrCodeGenerator.createQR(qrCodeNumber, 400, 400)
        _userQrCode.value = UserQRCode(qrCodeBitmap!!, qrCodeNumber)
    }

    fun addPoints(points: Int) {
        val updatedPoints = parseInt(userPoints.value!!) + points
        userRepo.updateField(hashFunction(_userEmailFlow.value), "points", updatedPoints.toString())
    }

    fun updatePoints(points: String) {
        userRepo.updateField(hashFunction(_userEmailFlow.value), "points", points)
    }

    fun updateStudying(studying: String) {
        userRepo.updateField(hashFunction(_userEmailFlow.value), "studying", studying)
    }

    fun updateRedeemingReward(points: String) {
        userRepo.updateField(hashFunction(_userEmailFlow.value), "redeemingReward", points)
    }

    fun setUser(email: String) {
        val id = hashFunction(email)
        viewModelScope.launch {
            try {
                _user.postValue(userRepo.getUser(id))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get user: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainSharedViewModel is being cleared. Stopping all repository listeners.")
        userRepo.stopAllListeners()
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
