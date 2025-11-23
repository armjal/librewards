package com.example.librewards

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import com.example.librewards.databinding.AdminFragmentHomeBinding
import com.example.librewards.utils.FragmentExtended
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class AdminHomeFragment(override val title: String = TITLE, override val icon: Int = R.drawable.home) : FragmentExtended() {
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var database: DatabaseReference
    private var _binding: AdminFragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var currentScanIsForTimer: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    startScanner()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Camera permission is required to scan barcodes.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AdminFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scanTimerButton.setOnClickListener {
            currentScanIsForTimer = true
            scanQRCode()
        }
        binding.scanRewardButton.setOnClickListener {
            currentScanIsForTimer = false
            scanQRCode()
        }

        binding.startTimerButton.setOnClickListener { startStudentTimer(binding.enterQr.text.toString()) }
        binding.redeemRewardButton.setOnClickListener { redeemStudentReward(binding.enterQr.text.toString()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun redeemStudentReward(studentNumber: String) {
        var redeemingReward: String
        val refChild =
            database.child("users").child(studentNumber).child("redeemingReward")
        refChild.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                redeemingReward = dataSnapshot.value.toString()
                Log.d(TAG, redeemingReward)
                if (redeemingReward == "0") {
                    refChild.setValue("1")
                } else {
                    Toast.makeText(context, "Student ID is not recognised", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun startStudentTimer(studentNumber: String) {
        var isStudying: String
        val refChild =
            database.child("users").child(studentNumber).child("studying")
        refChild.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                isStudying = dataSnapshot.value.toString()
                Log.d("TAG", isStudying)
                when (isStudying) {
                    "0", "2" -> {
                        refChild.setValue("1")
                    }
                    "1" -> {
                        refChild.setValue("0")
                    }
                    else -> {
                        Toast.makeText(context, "Student ID is not recognised", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun checkCameraPermission(): Boolean {
        return checkSelfPermission(
            requireActivity(),
            Manifest.permission.CAMERA
        ) == PERMISSION_GRANTED
    }

    private fun scanQRCode() {
        if (checkCameraPermission()) {
            startScanner()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanner() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = GmsBarcodeScanning.getClient(requireContext(), options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null) {
                    if (currentScanIsForTimer) {
                        startStudentTimer(rawValue)
                    } else {
                        redeemStudentReward(rawValue)
                    }
                } else {
                    Toast.makeText(requireContext(), "No barcode found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnCanceledListener {
                Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Scan failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    companion object {
        private val TAG: String = AdminHomeFragment::class.java.simpleName
        private const val TITLE: String = "Home"
    }
}
