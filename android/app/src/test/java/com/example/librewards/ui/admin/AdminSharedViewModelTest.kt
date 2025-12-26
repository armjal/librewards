package com.example.librewards.ui.admin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.StorageRepository
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class AdminSharedViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockUserRepo: UserRepository

    @Mock
    private lateinit var mockProductRepo: ProductRepository

    @Mock
    private lateinit var mockStorageRepo: StorageRepository

    private lateinit var viewModel: AdminSharedViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = AdminSharedViewModel(mockUserRepo, mockProductRepo, mockStorageRepo)
    }

    @Test
    fun `initialiseStateOnUserRetrieval updates state and scopes`() = runTest {
        val email = "test@example.com"
        val user = User("First", "Last", email, "Uni")

        `when`(mockUserRepo.getUser(anyString())).thenReturn(user)

        viewModel.initialiseStateOnUserRetrieval(email)

        // Wait for coroutine
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val observedUser = viewModel.user.getOrAwaitValue()
        assertEquals(user, observedUser)

        verify(mockProductRepo).setUniversityScope("Uni")
        verify(mockStorageRepo).setUniversityScope("Uni")
    }

    @Test
    fun `stopListeningToData calls stop on productRepo`() {
        viewModel.stopListeningToData()
        verify(mockProductRepo).stopAllListeners()
    }
}
