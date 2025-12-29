package com.example.librewards.ui.main

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng

class CurrentLocation(var location: Location) {
    var latLng = LatLng(location.latitude, location.longitude)
}

class MapsViewModel(val fusedLocationClient: FusedLocationProviderClient) : ViewModel() {
    private var _chosenLocation = MutableLiveData<CurrentLocation?>(null)
    val chosenLocation: LiveData<CurrentLocation?> = _chosenLocation
    private var _hasChosenLocation = false
    private var _currentLocation = MutableLiveData<CurrentLocation>()
    val currentLocation: LiveData<CurrentLocation> = _currentLocation
    private var _distanceFromChosenLocation = MutableLiveData<Float>()
    val distance: LiveData<Float> get() = _distanceFromChosenLocation

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                if (location != null) {
                    if (_hasChosenLocation && _chosenLocation.value == null) {
                        _chosenLocation.value = CurrentLocation(location)
                    }

                    _chosenLocation.value?.let {
                        _distanceFromChosenLocation.value = it.location.distanceTo(location)
                    }

                    _currentLocation.value = CurrentLocation(location)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun listenToLocationChanges() {
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(20f)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    @SuppressLint("MissingPermission")
    fun setChosenLocation() {
        _hasChosenLocation = true
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null && _chosenLocation.value == null) {
                _chosenLocation.value = CurrentLocation(location)
                _distanceFromChosenLocation.value = 0f
            }
        }
    }

    fun reset() {
        _hasChosenLocation = false
        _chosenLocation.value = null
        _distanceFromChosenLocation.value = 0F
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

class MapsViewModelFactory(
    private val fusedLocationClient: FusedLocationProviderClient,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = if (modelClass.isAssignableFrom(MapsViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST")
        MapsViewModel(fusedLocationClient) as T
    } else {
        throw IllegalArgumentException("ViewModel Not Found")
    }
}
