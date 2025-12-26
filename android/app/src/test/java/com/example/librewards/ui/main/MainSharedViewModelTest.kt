package com.example.librewards.ui.main

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq

@ExperimentalCoroutinesApi
class MainSharedViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockUserRepo: UserRepository

    @Mock
    private lateinit var mockProductRepo: ProductRepository

    @Mock
    private lateinit var mockBitmap: Bitmap

    private lateinit var viewModel: MainSharedViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock default flows to avoid NPEs during initialization if any
        `when`(mockUserRepo.listenForUserField(anyString(), anyString())).thenReturn(flowOf("0"))

        viewModel = MainSharedViewModel(mockUserRepo, mockProductRepo)
    }

    @Test
    fun `startObservingUser updates state`() = runTest {
        val email = "test@example.com"
        val user = User("First", "Last", email, "Uni")

        `when`(mockUserRepo.getUser(anyString())).thenReturn(user)

        viewModel.startObservingUser(email)

        // Wait for coroutine
        testScheduler.advanceUntilIdle()

        val observedUser = viewModel.user.getOrAwaitValue()
        assertEquals(user, observedUser)

        verify(mockProductRepo).setUniversityScope("Uni")
    }

    @Test
    fun `userPoints updates when flow emits`() {
        val email = "test@example.com"
        val points = "100"

        `when`(mockUserRepo.listenForUserField(anyString(), eq("points")))
            .thenReturn(flowOf(points))

        viewModel.startObservingUser(email)

        val observedPoints = viewModel.userPoints.getOrAwaitValue()
        assertEquals(points, observedPoints)
    }

    @Test
    fun `addPoints updates repo`() {
        val email = "test@example.com"
        val currentPoints = "100"
        val pointsToAdd = 50

        `when`(mockUserRepo.listenForUserField(anyString(), eq("points")))
            .thenReturn(flowOf(currentPoints))

        viewModel.startObservingUser(email)
        viewModel.userPoints.getOrAwaitValue() // Ensure flow is collected

        viewModel.addPoints(pointsToAdd)

        verify(mockUserRepo).updateField(anyString(), eq("points"), eq("150"))
    }

    @Test
    fun `createQRCode generates bitmap`() {
        val email = "test@example.com"
        viewModel.startObservingUser(email)

        // Mock Bitmap.createBitmap
        val mockedBitmapStatic = mockStatic(Bitmap::class.java)
        try {
            mockedBitmapStatic.`when`<Bitmap> {
                Bitmap.createBitmap(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.any(Bitmap.Config::class.java),
                )
            }.thenReturn(mockBitmap)

            viewModel.createQRCode()

            val qrCode = viewModel.userQrCode.getOrAwaitValue()
            assertEquals(mockBitmap, qrCode.bitmap)
        } finally {
            mockedBitmapStatic.close()
        }
    }
}
