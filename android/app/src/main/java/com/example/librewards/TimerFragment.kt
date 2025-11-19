package com.example.librewards

import android.Manifest
import android.app.Dialog
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
import androidx.fragment.app.Fragment
import com.example.librewards.databinding.FragmentTimerBinding
import com.example.librewards.databinding.PopupLayoutBinding
import com.example.librewards.qrcode.QRCodeGenerator
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
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable


class TimerFragment : Fragment(), OnMapReadyCallback {
    private lateinit var markerOptions: MarkerOptions
    private var marker: Marker? = null
    private lateinit var latLngLocTwo: LatLng
    private lateinit var latLngLocOne: LatLng
    private lateinit var circle: Circle
    private var popup: Dialog? = null
    private var totalTime: Long? = null
    var listener: TimerListener? = null
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

    private var popupLayoutBinding: PopupLayoutBinding? = null

    //Interface that consists of a method that will update the points in "RewardsFragment"
    interface TimerListener {
        fun onPointsTimerSent(points: Int)
    }

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
        fh = FirebaseHandler()
        database = FirebaseDatabase.getInstance().reference

        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapFragment = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        addTimerEventListener()
        val qrGen = QRCodeGenerator()
        binding.qrCode.setImageBitmap(qrGen.createQR(fh.hashFunction(mainActivity.email), 400, 400))
        binding.qrCodeNumber.text = fh.hashFunction(mainActivity.email)

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

    override fun onStart() {
        super.onStart()
        addPointsListener(0)
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
                            showPopup("No stop code was entered for 24 hours. The timer has been reset")
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
                    //Listener to communicate with Rewards Fragment and give the points to display in there
                    listener?.onPointsTimerSent(Integer.parseInt(binding.usersPoints.text.toString()))
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
                    refChild.setValue(finalPoints.toString())
                    binding.usersPoints.text = finalPoints.toString()
                    listener?.onPointsTimerSent(finalPoints)
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

    //Method that converts the duration spent at the library into points
    private fun setPointsFromTime(totalTime: Long) {
        val minutes = (totalTime / 1000 / 60).toInt()
        val pointsEarned: Int = when (totalTime) {
            in 0..10000 -> 0
            in 10001..29999 -> 10
            in 30000..59999 -> 50
            in 60000..119999 -> 75
            in 120000..179999 -> 125
            in 180000..259999 -> 225
            in 260000..399999 -> 400
            else -> 700
        }

        addPointsListener(pointsEarned)

        val newPoints = pointsEarned + Integer.parseInt(binding.usersPoints.text.toString())

        if (minutes == 1) {
            showPopup("Well done, you spent $minutes minute at the library and have earned $pointsEarned points! Your new points balance is: $newPoints")
        } else {
            showPopup("Well done, you spent $minutes minutes at the library and have earned $pointsEarned points! Your new points balance is:  $newPoints")
        }
    }

    fun updatePoints(newPoints: Int) {
        binding.usersPoints.text = newPoints.toString()
    }

    //Method that creates a popup
    private fun showPopup(text: String?) {
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupLayoutBinding = PopupLayoutBinding.inflate(layoutInflater)
        popup?.setContentView(popupLayoutBinding!!.root)
        popupLayoutBinding!!.popupText.text = text
        popupLayoutBinding!!.closeBtn.setOnClickListener { popup?.dismiss() }
        popup?.show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is TimerListener) {
            context
        } else {
            throw RuntimeException(context.toString() + "must implement TimerListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
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
