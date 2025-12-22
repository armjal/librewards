package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository
import kotlinx.coroutines.launch

sealed class StudentRewardStatus {
    object NotRedeemed : StudentRewardStatus()

    object Redeemed : StudentRewardStatus()

    object CantRedeem : StudentRewardStatus()

    object Error : StudentRewardStatus()
}

class AdminHomeViewModel(val userRepo: UserRepository) : ViewModel() {
    private var _studentRewardStatus: MutableLiveData<StudentRewardStatus> = MutableLiveData(StudentRewardStatus.NotRedeemed)
    val studentRewardStatus: LiveData<StudentRewardStatus> = _studentRewardStatus

    fun redeemRewardForStudent(studentId: String) {
        var student: User?
        viewModelScope.launch {
            student = userRepo.getUser(studentId)
            if (student == null) {
                _studentRewardStatus.value = StudentRewardStatus.Error
                return@launch
            }
            when (student?.redeemingReward) {
                "0" -> {
                    userRepo.updateField(studentId, "redeemingReward", "1")
                    _studentRewardStatus.value = StudentRewardStatus.Redeemed
                }
                else -> {
                    _studentRewardStatus.value = StudentRewardStatus.CantRedeem
                }
            }
        }
    }
}

class AdminHomeViewModelFactory(
    private val userRepo: UserRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(AdminHomeViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        AdminHomeViewModel(userRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
