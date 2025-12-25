package com.example.librewards.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.librewards.data.models.User
import kotlinx.coroutines.launch

sealed class StudentRewardStatus {
    object NotRedeemed : StudentRewardStatus()

    object Redeemed : StudentRewardStatus()

    object CantRedeem : StudentRewardStatus()

    object Error : StudentRewardStatus()
}

sealed class StudentTimerStatus {
    object Stopped : StudentTimerStatus()

    object Started : StudentTimerStatus()

    object Error : StudentTimerStatus()
}

class AdminHomeViewModel(val adminSharedViewModel: AdminSharedViewModel) : ViewModel() {
    private var _studentRewardStatus: MutableLiveData<StudentRewardStatus> = MutableLiveData(StudentRewardStatus.NotRedeemed)
    val studentRewardStatus: LiveData<StudentRewardStatus> = _studentRewardStatus

    private var _studentTimerStatus: MutableLiveData<StudentTimerStatus> = MutableLiveData(null)
    val studentTimerStatus: LiveData<StudentTimerStatus> = _studentTimerStatus

    fun redeemRewardForStudent(studentId: String) {
        var student: User?
        viewModelScope.launch {
            student = adminSharedViewModel.userRepo.getUser(studentId)
            if (student == null) {
                _studentRewardStatus.value = StudentRewardStatus.Error
                return@launch
            }
            when (student?.redeemingReward) {
                "0" -> {
                    adminSharedViewModel.userRepo.updateField(studentId, "redeemingReward", "1")
                    _studentRewardStatus.value = StudentRewardStatus.Redeemed
                }

                else -> {
                    _studentRewardStatus.value = StudentRewardStatus.CantRedeem
                }
            }
        }
    }

    fun toggleStudentTimer(studentNumber: String) {
        var student: User?
        viewModelScope.launch {
            student = adminSharedViewModel.userRepo.getUser(studentNumber)
            if (student == null) {
                _studentTimerStatus.value = StudentTimerStatus.Error
                return@launch
            }
            when (student?.studying) {
                "0", "2" -> {
                    adminSharedViewModel.userRepo.updateField(studentNumber, "studying", "1")
                    _studentTimerStatus.value = StudentTimerStatus.Started
                }

                "1" -> {
                    adminSharedViewModel.userRepo.updateField(studentNumber, "studying", "0")
                    _studentTimerStatus.value = StudentTimerStatus.Stopped
                }
            }
        }
    }
}

class AdminHomeViewModelFactory(
    private val adminSharedViewModel: AdminSharedViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(AdminHomeViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        AdminHomeViewModel(adminSharedViewModel) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
