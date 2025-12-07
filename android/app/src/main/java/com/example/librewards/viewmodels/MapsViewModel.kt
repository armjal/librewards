package com.example.librewards.viewmodels

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

class MapsViewModel(val fusedLocationClient: FusedLocationProviderClient) : ViewModel() {
    private var _chosenLocation: Location? = null
    private var _hasChosenLocation = MutableLiveData(false)
    val hasChosenLocation: LiveData<Boolean> = _hasChosenLocation

    private var _currentLocation: Location? = null

    private var _currentLocationLatLng = MutableLiveData<LatLng>()
    val currentLatLng: LiveData<LatLng> get() = _currentLocationLatLng

    private var _distanceFromChosenLocation = MutableLiveData<Float>()
    val distance: LiveData<Float> get() = _distanceFromChosenLocation

    @SuppressLint("MissingPermission")
    fun listenToLocationChanges() {
        val locationRequest = LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(20f)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    if (location != null) {
                        if (_chosenLocation == null) {
                            _chosenLocation = location
                        }
                        _distanceFromChosenLocation.value = _chosenLocation!!.distanceTo(location)
                        _currentLocationLatLng.value = LatLng(location.latitude, location.longitude)
                        _currentLocation = location
                    }
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun setChosenLocation() {
        _chosenLocation = _currentLocation
        _hasChosenLocation.value = true
        _distanceFromChosenLocation.value = 0F
    }

    fun reset() {
        _hasChosenLocation.value = false
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
