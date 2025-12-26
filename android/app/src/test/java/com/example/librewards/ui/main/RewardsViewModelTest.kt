package com.example.librewards.ui.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class RewardsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockMainSharedViewModel: MainSharedViewModel

    @Mock
    private lateinit var mockProductRepo: ProductRepository

    private val redeemingRewardStatus = MutableLiveData<String>()
    private val userPoints = MutableLiveData<String>()

    private lateinit var viewModel: RewardsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        `when`(mockMainSharedViewModel.productRepo).thenReturn(mockProductRepo)
        `when`(mockProductRepo.listenForProducts()).thenReturn(flowOf(emptyList()))

        `when`(mockMainSharedViewModel.redeemingRewardStatus).thenReturn(redeemingRewardStatus)
        `when`(mockMainSharedViewModel.userPoints).thenReturn(userPoints)

        viewModel = RewardsViewModel(mockMainSharedViewModel)
    }

    @Test
    fun `handleRewardStatusChange updates status`() {
        // "0" -> ReadyToRedeem
        redeemingRewardStatus.value = "0"
        assertEquals(RewardsEvent.ReadyToRedeem, viewModel.rewardStatus.getOrAwaitValue())

        // "1" -> Redeemed
        redeemingRewardStatus.value = "1"
        assertEquals(RewardsEvent.Redeemed, viewModel.rewardStatus.getOrAwaitValue())
    }

    @Test
    fun `setRewardsStatus updates mainViewModel`() {
        viewModel.setRewardsStatus(RewardsEvent.ReadyToRedeem)
        verify(mockMainSharedViewModel).updateRedeemingReward("0")

        viewModel.setRewardsStatus(RewardsEvent.Redeemed)
        verify(mockMainSharedViewModel).updateRedeemingReward("1")

        viewModel.setRewardsStatus(RewardsEvent.Neutral)
        verify(mockMainSharedViewModel).updateRedeemingReward("2")
    }

    @Test
    fun `minusPoints insufficient funds`() {
        userPoints.value = "50"

        viewModel.minusPoints(100)

        assertEquals(RewardsEvent.InsufficientFunds, viewModel.rewardStatus.getOrAwaitValue())
    }

    @Test
    fun `minusPoints sufficient funds`() {
        userPoints.value = "100"

        viewModel.minusPoints(50)

        verify(mockMainSharedViewModel).updatePoints("50")
    }
}
