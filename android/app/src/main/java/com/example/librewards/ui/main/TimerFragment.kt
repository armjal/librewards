package com.example.librewards.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer.OnChronometerTickListener
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.librewards.R
import com.example.librewards.databinding.FragmentTimerBinding
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.showPopup
import com.example.librewards.utils.toastMessage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.sothree.slidinguppanel.SlidingUpPanelLayout

class TimerFragment(
    override val icon: Int = R.drawable.timer,
) : FragmentExtended(), OnMapReadyCallback {
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val timerViewModel: TimerViewModel by viewModels {
        TimerViewModelFactory(mainSharedViewModel)
    }
    private val mapsViewModel: MapsViewModel by viewModels {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        MapsViewModelFactory(fusedLocationClient)
    }
    private var marker: Marker? = null
    private var mapCircle: Circle? = null
    private var googleMap: GoogleMap? = null
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val locationPermissionsLauncher = setLocationPermissionLauncher()

    companion object {
        private val TAG: String = TimerFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkLocationServicesPermissions()) {
            setupMap()
        } else {
            requestLocationServicesPermissions()
        }
        setupObservers()
        setupSlidePanelListener()
        setupChronometerDurationListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapsViewModel.listenToLocationChanges()
    }

    private fun setupObservers() {
        mainSharedViewModel.userPoints.observe(viewLifecycleOwner) { points ->
            binding.usersPoints.text = points
        }
        observerUserQRCode()
        observeTimerState()
        observeDistanceForTimer()
        observeMinutesSpentAtLibrary()
        observeLocationChanges()
        observeForMapDrawing()
    }

    private fun observerUserQRCode() {
        mainSharedViewModel.userQrCode.observe(viewLifecycleOwner) { qrCode ->
            binding.qrCode.setImageBitmap(qrCode.bitmap)
            binding.qrCodeNumber.text = qrCode.number
        }
    }

    private fun observeTimerState() {
        timerViewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                TimerState.Started -> {
                    Log.d(TAG, "Timer Started")
                    binding.stopwatch.base = SystemClock.elapsedRealtime()
                    binding.stopwatch.start()
                    mapsViewModel.setChosenLocation()
                }

                TimerState.Stopped, TimerState.Reset -> {
                    Log.d(TAG, "Timer Stopped/Reset")
                    resetTimerState()
                    timerViewModel.reset()
                }

                else -> {}
            }
        }
    }

    private fun observeDistanceForTimer() {
        val redSemiTransparent = "#4dff0000".toColorInt()
        val blueSemiTransparent = "#4d318ce7".toColorInt()
        mapsViewModel.distance.observe(viewLifecycleOwner) { distance ->
            if (distance == null || mapCircle == null) return@observe
            if (distance > 40) {
                Log.d(TAG, "observeDistanceForTimer: Student has gone out of the studying zone. Setting circle to red")
                mapCircle?.fillColor = redSemiTransparent
                timerViewModel.reset()
            } else {
                Log.d(TAG, "observeDistanceForTimer: Student is within the studying zone. Setting circle to blue")
                mapCircle?.fillColor = blueSemiTransparent
            }
        }
    }

    private fun observeMinutesSpentAtLibrary() {
        timerViewModel.timerSummary.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            val minuteText = resources.getQuantityString(R.plurals.minutes_plural, it.minutesSpent)

            showPopup(
                requireActivity(),
                getString(
                    R.string.congrats_message, it.minutesSpent, minuteText, it.pointsEarned, it.newTotalPoints,
                ),
            )
        }
    }

    private fun observeForMapDrawing() {
        mapsViewModel.chosenLocation.observe(viewLifecycleOwner) {
            if (it != null) {
                googleMap?.let { map ->
                    Log.d(TAG, "observeForMapDrawing: Drawing map circle, chosen location=$it")
                    drawMapCircle(it.latLng, map)
                }
            } else {
                Log.d(TAG, "observeForMapDrawing: chosen location is null, removing circle")
                googleMap?.stopAnimation()
                mapCircle?.remove()
                mapCircle = null
            }
        }
    }

    private fun observeLocationChanges() {
        mapsViewModel.currentLocation.observe(viewLifecycleOwner) {
            Log.d(TAG, "observeLocationChanges: $it")
            if (marker == null) {
                val markerOptions = MarkerOptions().position(it.latLng).title("I am here.")
                marker = googleMap?.addMarker(markerOptions)
            } else {
                marker?.position = it.latLng
            }
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it.latLng, 17F))
        }
    }

    private fun drawMapCircle(point: LatLng, googleMap: GoogleMap) {
        CircleOptions().let {
            it.center(point)
            it.radius(50.0)
            it.strokeColor(Color.BLACK)
            it.fillColor("#4d318ce7".toColorInt())
            it.strokeWidth(2f)
            mapCircle = googleMap.addCircle(it)
        }
    }

    private fun checkLocationServicesPermissions(): Boolean = ActivityCompat.checkSelfPermission(
        requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationServicesPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
        )
        locationPermissionsLauncher.launch(permissionsToRequest)
    }

    private fun setLocationPermissionLauncher(): ActivityResultLauncher<Array<String>?> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.any { it.value }) {
            Log.d(TAG, "Location permission granted.")
            setupMap()
        } else {
            toastMessage(requireActivity(), getString(R.string.location_permission_not_enabled))
        }
    }

    fun resetTimerState() {
        binding.stopwatch.let {
            it.onChronometerTickListener = null
            it.base = SystemClock.elapsedRealtime()
            it.stop()
        }
        mapsViewModel.reset()
    }

    fun setupChronometerDurationListener() {
        binding.stopwatch.onChronometerTickListener = OnChronometerTickListener {
            val twentyFourHoursMs = 86400000
            if ((SystemClock.elapsedRealtime() - binding.stopwatch.base) >= twentyFourHoursMs) {
                resetTimerState()
                showPopup(
                    requireActivity(), getString(R.string.no_stop_code_entered),
                )
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d(TAG, "Map is ready")
        googleMap = map
    }

    private fun setupSlidePanelListener() {
        binding.slidingPanel.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                mainSharedViewModel.onPanelSlide(slideOffset)
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?,
            ) {
            }
        })
    }

    @VisibleForTesting
    fun getMapCircle(): Circle? = mapCircle
}
