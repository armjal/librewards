package com.example.librewards.ui.main

import android.os.SystemClock
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import utils.MainDispatcherRule
import utils.getOrAwaitValue

@ExperimentalCoroutinesApi
class TimerViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockMainSharedViewModel: MainSharedViewModel

    private val studyingStatus = MutableLiveData<String>()
    private val userPoints = MutableLiveData<String>()

    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        `when`(mockMainSharedViewModel.studyingStatus).thenReturn(studyingStatus)
        `when`(mockMainSharedViewModel.userPoints).thenReturn(userPoints)

        viewModel = TimerViewModel(mockMainSharedViewModel)
    }

    @Test
    fun `start updates state and mainViewModel`() {
        val mockedSystemClock = mockStatic(SystemClock::class.java)
        try {
            val startTime = 1000L
            mockedSystemClock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(startTime)

            viewModel.start()

            assertEquals(TimerState.Started, viewModel.state.getOrAwaitValue())
            verify(mockMainSharedViewModel).updateStudying("1")
            assertEquals(startTime, viewModel.startTime)
        } finally {
            mockedSystemClock.close()
        }
    }

    @Test
    fun `stop updates state, adds points and generates summary`() {
        val mockedSystemClock = mockStatic(SystemClock::class.java)
        try {
            val startTime = 1000L
            val endTime = 61000L + 1000L // 61 seconds later

            // Set up initial state
            mockedSystemClock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(startTime)
            viewModel.start()

            // Prepare for stop
            mockedSystemClock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(endTime)
            userPoints.value = "100"

            viewModel.stop()

            assertEquals(TimerState.Stopped, viewModel.state.getOrAwaitValue())
            verify(mockMainSharedViewModel).updateStudying("0")

            // 61 seconds -> > 60000 -> 75 points (based on PointsUtils ranges in previous files)
            // in 60000..119999 -> 75
            verify(mockMainSharedViewModel).addPoints(75)

            val summary = viewModel.timerSummary.getOrAwaitValue()
            assertEquals(1, summary.minutesSpent) // 61000 / 1000 / 60 = 1
            assertEquals(75, summary.pointsEarned)
            assertEquals(175, summary.newTotalPoints)
        } finally {
            mockedSystemClock.close()
        }
    }

    @Test
    fun `reset updates state`() {
        viewModel.reset()

        assertEquals(TimerState.Reset, viewModel.state.getOrAwaitValue())
        verify(mockMainSharedViewModel).updateStudying("2")
        assertEquals(0L, viewModel.elapsedTime)
        assertEquals(0L, viewModel.startTime)
    }

    @Test
    fun `studyingStatus observer updates state`() {
        // "1" -> Start
        val mockedSystemClock = mockStatic(SystemClock::class.java)
        try {
            mockedSystemClock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(1000L)

            studyingStatus.value = "1"
            assertEquals(TimerState.Started, viewModel.state.getOrAwaitValue())

            // "0" -> Stop
            userPoints.value = "0"
            studyingStatus.value = "0"
            assertEquals(TimerState.Stopped, viewModel.state.getOrAwaitValue())
        } finally {
            mockedSystemClock.close()
        }
    }
}
