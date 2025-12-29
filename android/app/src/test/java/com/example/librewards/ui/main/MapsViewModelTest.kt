package com.example.librewards.ui.main

import android.location.Location
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.librewards.utils.MainDispatcherRule
import com.example.librewards.utils.TestUtils
import com.example.librewards.utils.getOrAwaitValue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class MapsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockFusedLocationClient: FusedLocationProviderClient

    @Mock
    private lateinit var mockLocation: Location

    private lateinit var viewModel: MapsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = MapsViewModel(mockFusedLocationClient)
    }

    @Test
    fun `listenToLocationChanges requests location updates`() {
        viewModel.listenToLocationChanges()

        verify(mockFusedLocationClient).requestLocationUpdates(
            ArgumentMatchers.any(LocationRequest::class.java),
            ArgumentMatchers.any(LocationCallback::class.java),
            ArgumentMatchers.any(),
        )
    }

    @Test
    fun `location callback updates current location and distance`() {
        // Trigger request to capture callback
        viewModel.listenToLocationChanges()

        val callbackCaptor = ArgumentCaptor.forClass(LocationCallback::class.java)
        verify(mockFusedLocationClient).requestLocationUpdates(
            ArgumentMatchers.any(LocationRequest::class.java),
            callbackCaptor.capture(),
            ArgumentMatchers.any(),
        )

        // Simulate location update
        `when`(mockLocation.latitude).thenReturn(10.0)
        `when`(mockLocation.longitude).thenReturn(20.0)
        val locationResult = LocationResult.create(listOf(mockLocation))

        callbackCaptor.value.onLocationResult(locationResult)

        // Verify current location updated
        val currentLocation: CurrentLocation = viewModel.currentLocation.getOrAwaitValue()
        assertEquals(10.0, currentLocation.latLng.latitude, 0.0)
        assertEquals(20.0, currentLocation.latLng.longitude, 0.0)
    }

    @Test
    fun `setChosenLocation marks location as chosen`() {
        // First simulate getting a location
        viewModel.listenToLocationChanges()
        val callbackCaptor = ArgumentCaptor.forClass(LocationCallback::class.java)
        verify(mockFusedLocationClient).requestLocationUpdates(
            ArgumentMatchers.any(LocationRequest::class.java),
            callbackCaptor.capture(),
            ArgumentMatchers.any(),
        )

        val locationResult = LocationResult.create(listOf(mockLocation))
        callbackCaptor.value.onLocationResult(locationResult)

        // Mock the Task returned by getCurrentLocation
        val mockTask = mock(Task::class.java) as Task<Location>
        `when`(mockFusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null))
            .thenReturn(mockTask)

        `when`(mockTask.result).thenReturn(mockLocation)
        TestUtils.mockTask(mockTask)

        // Set chosen location
        viewModel.setChosenLocation()

        assertEquals(mockLocation, viewModel.chosenLocation.getOrAwaitValue()?.location)
        assertEquals(0f, viewModel.distance.getOrAwaitValue(), 0.0f)
    }

    @Test
    fun `reset clears chosen location`() {
        viewModel.reset()
        assertNull(viewModel.chosenLocation.getOrAwaitValue())
        assertEquals(0f, viewModel.distance.getOrAwaitValue(), 0.0f)
    }
}
