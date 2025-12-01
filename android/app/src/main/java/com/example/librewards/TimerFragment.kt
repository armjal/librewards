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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import com.example.librewards.databinding.FragmentTimerBinding
import com.example.librewards.qrcode.QRCodeGenerator
import com.example.librewards.repositories.UserRepository
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.calculatePointsFromTime
import com.example.librewards.utils.showPopup
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.MainViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout


class TimerFragment(
    override val icon: Int = R.drawable.timer
) : FragmentExtended(), OnMapReadyCallback {
    private lateinit var userRepo: UserRepository

    private val mainSharedViewModel: MainSharedViewModel by activityViewModels {
        MainViewModelFactory(userRepo)
    }
    private lateinit var markerOptions: MarkerOptions
    private var marker: Marker? = null
    private lateinit var latLngLocTwo: LatLng
    private lateinit var latLngLocOne: LatLng
    private lateinit var circle: Circle
    private var totalTime: Long? = null
    private lateinit var fh: FirebaseHandler
    private lateinit var mainActivity: MainActivity
    private lateinit var adminActivity: AdminActivity
    private lateinit var locationOne: Location
    private lateinit var locationTwo: Location
    private var pointsListener: ValueEventListener? = null
    private lateinit var database: DatabaseReference
    private var counter: Int? = null
    private var distance: Float? = null
    lateinit var mapFragment: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private lateinit var circleOptions: CircleOptions
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PERMISSION_ID = 1010
        private val TAG: String = TimerFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            counter = requireArguments().getInt("param1")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        mainActivity = activity as MainActivity
        adminActivity = AdminActivity()
        database = FirebaseDatabase.getInstance().reference
        userRepo = UserRepository(database)
        fh = FirebaseHandler()

        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        addTimerEventListener()
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
        if (checkPermission()) {
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locListener)
        } else {
            requestPermission()
        }
    }

    private fun drawCircle(point: LatLng, googleMap: GoogleMap) {
        // Instantiating CircleOptions to draw a circle around the marker
        circleOptions = CircleOptions()
        // Specifying the center of the circle
        circleOptions.center(point)
        // Radius of the circle
        circleOptions.radius(50.0)
        // Border color of the circle
        circleOptions.strokeColor(Color.BLACK)
        // Fill color of the circle
        circleOptions.fillColor("#4d318ce7".toColorInt())
        // Border width of the circle
        circleOptions.strokeWidth(2f)
        // Adding the circle to the GoogleMap
        circle = googleMap.addCircle(circleOptions)
    }

    private fun checkPermission(): Boolean {
        //check if location permissions have been granted by user
        return ActivityCompat.checkSelfPermission(
            mainActivity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    mainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermission() {
        //this function will allow us to tell the user to request the necessary permission if they are not granted
        ActivityCompat.requestPermissions(
            mainActivity,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Debug:", "You have the Permission")
            }
        }
    }

    private fun addTimerEventListener() {
        val refChild = fh.getChild("users", mainActivity.email, "studying")
        var isStudying: String
        val timerListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                isStudying = dataSnapshot.value.toString()
                Log.d(TAG, isStudying)
                if (isStudying == "1") {
                    binding.stopwatch.base = SystemClock.elapsedRealtime()
                    binding.stopwatch.start()
                    getLocationDistance()
                    binding.stopwatch.onChronometerTickListener = OnChronometerTickListener {
                        //Checks if the stopwatch has gone over 24 hours. If so, the stopwatch resets back to its original state
                        if (SystemClock.elapsedRealtime() - binding.stopwatch.base >= 800000) {
                            binding.stopwatch.base = SystemClock.elapsedRealtime()
                            binding.stopwatch.stop()
                            showPopup(
                                requireActivity(),
                                "No stop code was entered for 24 hours. The timer has been reset"
                            )
                        }
                        if (distance != null && distance!! > 50) {
                            binding.stopwatch.base = SystemClock.elapsedRealtime()
                            binding.stopwatch.stop()
                            fh.getChild("users", mainActivity.email, "studying").setValue("2")
                        }
                    }

                } else if (isStudying == "0") {
                    totalTime = SystemClock.elapsedRealtime() - binding.stopwatch.base
                    setPointsFromTime(totalTime!!)
                    binding.stopwatch.base = SystemClock.elapsedRealtime()
                    binding.stopwatch.stop()
                    googleMap?.stopAnimation()
                    googleMap?.clear()
                    refChild.setValue("2")
                }

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        }
        refChild.addValueEventListener(timerListener)
    }

    private fun addPointsListener(addValue: Int): Int {
        val refChild = fh.getChild("users", mainActivity.email, "points")
        var dbPoints: String
        var finalPoints = 0
        pointsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dbPoints = dataSnapshot.value.toString()
                if (dbPoints != "null") {
                    finalPoints = Integer.parseInt(dbPoints) + addValue
                    binding.usersPoints.text = finalPoints.toString()
                    mainSharedViewModel.updatePoints(finalPoints.toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        }
        refChild.addListenerForSingleValueEvent(pointsListener as ValueEventListener)
        return finalPoints
    }

    private fun setPointsFromTime(totalTime: Long) {
        val pointsEarned = calculatePointsFromTime(totalTime)
        val minutes = (totalTime / 1000 / 60).toInt()

        addPointsListener(pointsEarned)

        val newPoints = pointsEarned + Integer.parseInt(binding.usersPoints.text.toString())

        if (minutes == 1) {
            showPopup(
                requireActivity(),
                getString(R.string.congrats_message, "minute", minutes, pointsEarned, newPoints)
            )
        } else {
            showPopup(
                requireActivity(),
                getString(R.string.congrats_message, "minutes", minutes, pointsEarned, newPoints)
            )
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        if (checkPermission()) {
            val locManager =
                mainActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastKnownLocation =
                locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastKnownLocation != null) {
                locationOne = lastKnownLocation // The crucial initialization
                latLngLocOne = LatLng(locationOne.latitude, locationOne.longitude)
                markerOptions = MarkerOptions().position(latLngLocOne).title("I am here.")
                p0.animateCamera(CameraUpdateFactory.newLatLng(latLngLocOne))
                p0.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngLocOne, 17F))
                marker = p0.addMarker(markerOptions)!!
                drawCircle(latLngLocOne, p0)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Could not determine location. Please ensure location services are enabled.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            requestPermission()
        }
    }
}
