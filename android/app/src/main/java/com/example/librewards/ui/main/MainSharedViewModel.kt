package com.example.librewards.ui.main

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.utils.QRCodeGenerator
import com.example.librewards.utils.generateIdFromKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.Integer.parseInt

data class UserQRCode(
    val bitmap: Bitmap,
    val number: String,
)

class MainSharedViewModel(val userRepo: UserRepository, val productRepo: ProductRepository) : ViewModel() {
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
        userRepo.listenForUserField(email, "points").map { it ?: "0" }
    }.asLiveData()

    val studyingStatus: LiveData<String> = _userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "studying").map { it ?: "2" }
    }.asLiveData()

    val redeemingRewardStatus: LiveData<String> = _userEmailFlow.flatMapLatest { email ->
        userRepo.listenForUserField(email, "redeemingReward").map { it ?: "2" }
    }.asLiveData()

    fun onPanelSlide(offset: Float) {
        _panelSlideOffset.value = offset
    }

    fun startObservingUser(email: String) {
        Log.d(TAG, "Starting to observe user: $email")
        _userEmailFlow.value = email
        initialiseStateOnUserRetrieval(email)
    }

    fun createQRCode() {
        val qrCodeGenerator = QRCodeGenerator()
        val qrCodeNumber = generateIdFromKey(_userEmailFlow.value)
        val qrCodeBitmap = qrCodeGenerator.createQR(qrCodeNumber, 400, 400)
        _userQrCode.value = UserQRCode(qrCodeBitmap!!, qrCodeNumber)
    }

    fun addPoints(points: Int) {
        val currentPoints = userPoints.value?.let {
            try {
                parseInt(it)
            } catch (e: Exception) {
                0
            }
        } ?: 0
        val updatedPoints = currentPoints + points
        userRepo.updateField(generateIdFromKey(_userEmailFlow.value), "points", updatedPoints.toString())
    }

    fun updatePoints(points: String) {
        userRepo.updateField(generateIdFromKey(_userEmailFlow.value), "points", points)
    }

    fun updateStudying(studying: String) {
        userRepo.updateField(generateIdFromKey(_userEmailFlow.value), "studying", studying)
    }

    fun updateRedeemingReward(points: String) {
        userRepo.updateField(generateIdFromKey(_userEmailFlow.value), "redeemingReward", points)
    }

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
        }
    }

    fun stopListeningToData() {
        userRepo.stopAllListeners()
        productRepo.stopAllListeners()
    }
}

class MainViewModelFactory(
    private val userRepo: UserRepository,
    private val productRepo: ProductRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(MainSharedViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        MainSharedViewModel(userRepo, productRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
