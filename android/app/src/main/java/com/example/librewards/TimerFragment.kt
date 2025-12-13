package com.example.librewards

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
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.librewards.databinding.FragmentTimerBinding
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.showPopup
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.MapsViewModel
import com.example.librewards.viewmodels.MapsViewModelFactory
import com.example.librewards.viewmodels.TimerState
import com.example.librewards.viewmodels.TimerViewModel
import com.example.librewards.viewmodels.TimerViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val timerViewModel: TimerViewModel by viewModels {
        TimerViewModelFactory(mainSharedViewModel)
    }
    private val mapsViewModel: MapsViewModel by viewModels {
        MapsViewModelFactory(fusedLocationClient)
    }
    private var marker: Marker? = null
    private lateinit var mapCircle: Circle
    private var googleMap: GoogleMap? = null
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val locationPermissionsLauncher = registerLocationPermissionLauncher()

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
        setupMap()
        setupObservers()
        setupSlidePanelListener()
        setupChronometerDurationListener()
    }

    private fun setupMap() {
        if (!checkLocationServicesPermissions()) {
            requestLocationServicesPermissions()
            return
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
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
                    binding.stopwatch.base = SystemClock.elapsedRealtime()
                    binding.stopwatch.start()
                    mapsViewModel.setChosenLocation()
                }

                TimerState.Stopped, TimerState.Reset -> {
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
            if (distance == null || !this::mapCircle.isInitialized) return@observe
            if (distance > 40) {
                mapCircle.fillColor = redSemiTransparent
                timerViewModel.reset()
            } else {
                mapCircle.fillColor = blueSemiTransparent
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
        mapsViewModel.hasChosenLocation.observe(viewLifecycleOwner) { hasChosenLocation ->
            if (hasChosenLocation) {
                drawMapCircle(mapsViewModel.currentLatLng.value!!, googleMap!!)
            } else {
                googleMap?.stopAnimation()
                if (this::mapCircle.isInitialized) {
                    mapCircle.remove()
                }
            }
        }
    }

    private fun observeLocationChanges() {
        mapsViewModel.currentLatLng.observe(viewLifecycleOwner) { latLng ->
            if (marker == null) {
                val markerOptions = MarkerOptions().position(latLng).title("I am here.")
                marker = googleMap?.addMarker(markerOptions)!!
            } else {
                marker?.position = latLng
            }
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17F))
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

    private fun registerLocationPermissionLauncher(): ActivityResultLauncher<Array<String>?> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.getOrDefault(
                Manifest.permission.ACCESS_FINE_LOCATION,
                false,
            ) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        ) {
            Log.d(TAG, "Location permission granted.")
            if (googleMap != null) {
                onMapReady(googleMap!!)
            } else {
                toastMessage(requireActivity(), getString(R.string.location_permission_not_enabled))
            }
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
            val twentyFourHoursMs = 864000
            if (SystemClock.elapsedRealtime() - binding.stopwatch.base >= twentyFourHoursMs) {
                resetTimerState()
                showPopup(
                    requireActivity(), getString(R.string.no_stop_code_entered),
                )
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
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
}
