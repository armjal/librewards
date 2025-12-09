package com.example.librewards.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository
import java.lang.Integer.parseInt

sealed class RewardsEvent() {
    object ReadyToRedeem : RewardsEvent()

    object Redeemed : RewardsEvent()

    object Neutral : RewardsEvent()
}

class RewardsViewModel(val mainSharedViewModel: MainSharedViewModel, val productRepo: ProductRepository) : ViewModel() {
    val productEntries: LiveData<List<ProductEntry>> = productRepo.productEntriesLiveData
    private var _rewardStatus = MutableLiveData<RewardsEvent>(RewardsEvent.Neutral)
    val rewardStatus: LiveData<RewardsEvent> get() = _rewardStatus

    init {
        productRepo.startListeningForProducts()
        mainSharedViewModel.redeemingRewardStatus.observeForever { status ->
            handleRewardStatusChange(status)
        }
    }

    fun handleRewardStatusChange(status: String) {
        when (status) {
            "0" -> _rewardStatus.value = RewardsEvent.ReadyToRedeem
            "1" -> _rewardStatus.value = RewardsEvent.Redeemed
        }
    }

    fun setRewardsStatus(status: RewardsEvent) {
        when (status) {
            RewardsEvent.ReadyToRedeem -> mainSharedViewModel.updateRedeemingReward("0")
            RewardsEvent.Redeemed -> mainSharedViewModel.updateRedeemingReward("1")
            RewardsEvent.Neutral -> mainSharedViewModel.updateRedeemingReward("2")
        }
    }

    fun minusPoints(points: Int) {
        val updatedPoints = parseInt(mainSharedViewModel.userPoints.value!!) - points
        mainSharedViewModel.updatePoints(updatedPoints.toString())
    }

    override fun onCleared() {
        super.onCleared()
        productRepo.stopListeningForProducts()
    }
}

class RewardsViewModelFactory(
    private val mainSharedViewModel: MainSharedViewModel, private val productRepo: ProductRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(RewardsViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        RewardsViewModel(mainSharedViewModel, productRepo) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
