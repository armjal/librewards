package com.example.librewards

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import com.example.librewards.qrcode.QRCodeGenerator
import com.example.librewards.repositories.UserRepository
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.calculatePointsFromTime
import com.example.librewards.utils.showPopup
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.StopwatchState
import com.example.librewards.viewmodels.StopwatchViewModel
import com.example.librewards.viewmodels.TimerViewModel
import com.example.librewards.viewmodels.TimerViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import com.sothree.slidinguppanel.SlidingUpPanelLayout


class TimerFragment(
    override val icon: Int = R.drawable.timer
) : FragmentExtended(), OnMapReadyCallback {
    private var userRepo = UserRepository(FirebaseDatabase.getInstance().reference)
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()

    private val timerViewModel: TimerViewModel by viewModels {
        TimerViewModelFactory(userRepo)
    }

    private val stopwatchViewModel: StopwatchViewModel by viewModels()
    private var marker: Marker? = null
    private lateinit var latLngLocTwo: LatLng
    private lateinit var latLngLocOne: LatLng
    private lateinit var circle: Circle
    private lateinit var mainActivity: MainActivity
    private lateinit var locationOne: Location
    private lateinit var locationTwo: Location
    private var distance: Float? = null
    lateinit var mapFragment: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val locationPermissionsLauncher = registerLocationPermissionLauncher()

    companion object {
        private val TAG: String = TimerFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        mainActivity = activity as MainActivity
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        handleStudyingChanges()
        handleStopwatchChanges()
        val qrGen = QRCodeGenerator()
        binding.qrCode.setImageBitmap(qrGen.createQR(hashFunction(mainActivity.email), 400, 400))
        binding.qrCodeNumber.text = hashFunction(mainActivity.email)
        mainSharedViewModel.updatedUser.observe(viewLifecycleOwner) { user ->
            binding.usersPoints.text = user.points
        }
        val touchableList: ArrayList<View?> = mainActivity.tabLayout.touchables
        binding.slidingPanel.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                mainActivity.profileImage.alpha = (1.3 - slideOffset).toFloat()
                mainActivity.logo.alpha = (1.3 - slideOffset).toFloat()
                mainActivity.appBarLayout.alpha = (1.3 - slideOffset).toFloat()
                mainActivity.tabLayout.alpha = (1.3 - slideOffset).toFloat()

                if (slideOffset > 0.9) {
                    mainActivity.profileImage.setOnClickListener(null)
                    touchableList[0]?.isEnabled = false
                    touchableList[1]?.isEnabled = false

                } else {
                    mainActivity.profileImage.setOnClickListener { }
                    touchableList[0]?.isEnabled = true
                    touchableList[1]?.isEnabled = true
                }

            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
            }

        })
    }

    private fun getLocationDistance() {
        val locManager = mainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationTwo = location
                distance = locationOne.distanceTo(locationTwo)
                Log.d(TAG, distance.toString())
                latLngLocTwo = LatLng(locationTwo.latitude, locationTwo.longitude)
                if (marker != null) {
                    marker!!.position = latLngLocTwo
                }
                if (distance!! > 40) {
                    circle.fillColor = "#4dff0000".toColorInt()
                } else {
                    circle.fillColor = "#4d318ce7".toColorInt()

                }

            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Status changed")
            }

            override fun onProviderEnabled(provider: String) {
            }

            override fun onProviderDisabled(provider: String) {
            }

        }
        if (checkLocationServicesPermissions()) {
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locListener)
        } else {
            requestLocationServicesPermissions()
        }
    }

    private fun drawCircle(point: LatLng, googleMap: GoogleMap) {
        // Instantiating CircleOptions to draw a circle around the marker
        CircleOptions().let {
            it.center(point)
            it.radius(50.0)
            it.strokeColor(Color.BLACK)
            it.fillColor("#4d318ce7".toColorInt())
            it.strokeWidth(2f)
            circle = googleMap.addCircle(it)
        }
    }

    private fun checkLocationServicesPermissions(): Boolean {
        //check if location permissions have been granted by user
        return ActivityCompat.checkSelfPermission(
            mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            mainActivity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    }

    private fun requestLocationServicesPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
        )
        locationPermissionsLauncher.launch(permissionsToRequest)
    }

    private fun registerLocationPermissionLauncher(): ActivityResultLauncher<Array<String>?> {
        return registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.getOrDefault(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    false
                ) || permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            ) {
                Log.d(TAG, "Location permission granted.")
                if (googleMap != null) {
                    onMapReady(googleMap!!)
                } else {
                    toastMessage(
                        requireActivity(),
                        "Location permission is required to use the timer feature."
                    )
                }
            }
        }
    }

    private fun handleStudyingChanges() {
        mainSharedViewModel.updatedUser.observe(viewLifecycleOwner) { user ->
            when (user?.studying) {
                "0" -> stopwatchViewModel.stop()
                "1" -> stopwatchViewModel.start()
            }
        }
    }

    private fun handleStopwatchChanges() {
        stopwatchViewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                StopwatchState.Started -> {
                    binding.stopwatch.base = SystemClock.elapsedRealtime()
                    binding.stopwatch.start()
                    validateUserWithinTimerBoundaries()
                }

                StopwatchState.Stopped -> {
                    setPointsFromTime(stopwatchViewModel.elapsedTime)
                    resetTimerState()
                    timerViewModel.updateStudying(mainSharedViewModel.updatedUser.value!!, "2")
                }

                else -> {
                    resetTimerState()
                }
            }
        }
    }

    fun resetTimerState() {
        binding.stopwatch.stop()
        binding.stopwatch.base = SystemClock.elapsedRealtime()
        googleMap?.stopAnimation()
        googleMap?.clear()
    }

    fun validateUserWithinTimerBoundaries() {
        getLocationDistance()
        binding.stopwatch.onChronometerTickListener = OnChronometerTickListener {
            //Checks if the stopwatch has gone over 24 hours. If so, the stopwatch resets back to its original state
            if (SystemClock.elapsedRealtime() - binding.stopwatch.base >= 800000) {
                resetTimerState()
                showPopup(
                    requireActivity(), getString(R.string.no_stop_code_entered)
                )
            }
            if (distance != null && distance!! > 50) {
                resetTimerState()
                timerViewModel.updateStudying(mainSharedViewModel.updatedUser.value!!, "2")
            }
        }
    }

    private fun setPointsFromTime(totalTime: Long) {
        val pointsEarned = calculatePointsFromTime(totalTime)
        val minutes = (totalTime / 1000 / 60).toInt()

        mainSharedViewModel.updatePoints(pointsEarned)

        val newPoints = pointsEarned + Integer.parseInt(binding.usersPoints.text.toString())

        if (minutes == 1) {
            showPopup(
                requireActivity(),
                getString(R.string.congrats_message, minutes, "minute", pointsEarned, newPoints)
            )
        } else {
            showPopup(
                requireActivity(),
                getString(R.string.congrats_message, minutes, "minutes", pointsEarned, newPoints)
            )
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        if (!checkLocationServicesPermissions()) {
            requestLocationServicesPermissions()
            return
        }
        val locManager = mainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocation = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (lastKnownLocation != null) {
            locationOne = lastKnownLocation // The crucial initialization
            latLngLocOne = LatLng(locationOne.latitude, locationOne.longitude)
            val markerOptions = MarkerOptions().position(latLngLocOne).title("I am here.")
            p0.animateCamera(CameraUpdateFactory.newLatLng(latLngLocOne))
            p0.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngLocOne, 17F))
            marker = p0.addMarker(markerOptions)!!
            drawCircle(latLngLocOne, p0)
        } else {
            toastMessage(
                requireActivity(),
                "Could not determine location. Please ensure location services are enabled."
            )
        }
    }
}
