package com.example.librewards.ui.admin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class AdminHomeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockAdminSharedViewModel: AdminSharedViewModel

    @Mock
    private lateinit var mockUserRepo: UserRepository

    private lateinit var viewModel: AdminHomeViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        `when`(mockAdminSharedViewModel.userRepo).thenReturn(mockUserRepo)

        viewModel = AdminHomeViewModel(mockAdminSharedViewModel)
    }

    @Test
    fun `redeemRewardForStudent updates status when not redeemed`() = runTest {
        val studentId = "123"
        val student = User("Name", "Last", "email", "Uni", redeemingReward = "0")

        `when`(mockUserRepo.getUser(studentId)).thenReturn(student)

        viewModel.redeemRewardForStudent(studentId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StudentRewardStatus.Redeemed, viewModel.studentRewardStatus.getOrAwaitValue())
        verify(mockUserRepo).updateField(studentId, "redeemingReward", "1")
    }

    @Test
    fun `redeemRewardForStudent sets CantRedeem status when already redeemed`() = runTest {
        val studentId = "123"
        val student = User("Name", "Last", "email", "Uni", redeemingReward = "1")

        `when`(mockUserRepo.getUser(studentId)).thenReturn(student)

        viewModel.redeemRewardForStudent(studentId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StudentRewardStatus.CantRedeem, viewModel.studentRewardStatus.getOrAwaitValue())
    }

    @Test
    fun `redeemRewardForStudent sets Error status when user null`() = runTest {
        val studentId = "123"

        `when`(mockUserRepo.getUser(studentId)).thenReturn(null)

        viewModel.redeemRewardForStudent(studentId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StudentRewardStatus.Error, viewModel.studentRewardStatus.getOrAwaitValue())
    }

    @Test
    fun `toggleStudentTimer starts timer when stopped`() = runTest {
        val studentId = "123"
        val student = User("Name", "Last", "email", "Uni", studying = "0")

        `when`(mockUserRepo.getUser(studentId)).thenReturn(student)

        viewModel.toggleStudentTimer(studentId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StudentTimerStatus.Started, viewModel.studentTimerStatus.getOrAwaitValue())
        verify(mockUserRepo).updateField(studentId, "studying", "1")
    }

    @Test
    fun `toggleStudentTimer stops timer when started`() = runTest {
        val studentId = "123"
        val student = User("Name", "Last", "email", "Uni", studying = "1")

        `when`(mockUserRepo.getUser(studentId)).thenReturn(student)

        viewModel.toggleStudentTimer(studentId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StudentTimerStatus.Stopped, viewModel.studentTimerStatus.getOrAwaitValue())
        verify(mockUserRepo).updateField(studentId, "studying", "0")
    }
}
