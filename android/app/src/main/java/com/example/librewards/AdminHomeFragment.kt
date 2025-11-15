package com.example.librewards

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.librewards.databinding.AdminFragmentHomeBinding
import com.example.librewards.qrcode.IntentIntegratorExtended
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminHomeFragment : Fragment() {
    private val PERMISSION_ID = 2020
    private val SCAN_TIMER = 341
    private val SCAN_REWARD = 143
    private lateinit var database: DatabaseReference
    private var _binding: AdminFragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance().reference

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
        if (checkPermission()) {
            binding.scanTimerButton.setOnClickListener { scanButton(SCAN_TIMER) }
            binding.scanRewardButton.setOnClickListener { scanButton(SCAN_REWARD) }
        } else {
            requestPermission()
        }
        binding.startTimerButton.setOnClickListener { startStudentTimer(binding.enterQr.text.toString()) }
        binding.redeemRewardButton.setOnClickListener { redeemStudentReward(binding.enterQr.text.toString()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val scanTimerIntent = IntentIntegratorExtended.extendedParseActivityResult(
            requestCode,
            resultCode,
            data,
            SCAN_TIMER
        )
        val scanRewardIntent = IntentIntegratorExtended.extendedParseActivityResult(
            requestCode,
            resultCode,
            data,
            SCAN_REWARD
        )
        super.onActivityResult(requestCode, resultCode, data)
        if (scanTimerIntent != null) {
            if (scanTimerIntent.contents == null) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    context,
                    "You have successfully scanned ${scanTimerIntent.contents}",
                    Toast.LENGTH_LONG
                ).show()
                startStudentTimer(scanTimerIntent.contents)
            }
        }
        if (scanRewardIntent != null) {
            if (scanRewardIntent.contents == null) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    context,
                    "You have successfully scanned ${scanRewardIntent.contents}",
                    Toast.LENGTH_LONG
                ).show()
                redeemStudentReward(scanRewardIntent.contents)
            }
        }

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
                if (isStudying == "0" || isStudying == "2") {
                    refChild.setValue("1")
                } else if (isStudying == "1") {
                    refChild.setValue("0")
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

    private fun checkPermission(): Boolean {
        //check if location permissions have been granted by user
        return (ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.CAMERA
        )
                == PackageManager.PERMISSION_GRANTED)

    }

    private fun requestPermission() {
        //this function will allow us to tell the user to request the necessary permission if they are not granted
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
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
                Log.d(TAG, "You have the Permission")
            }
        }
    }

    private fun scanButton(requestCode: Int) {
        val intentIntegrator = IntentIntegratorExtended(requireActivity())
        intentIntegrator.extendedInitiateScan(requestCode)
    }

    companion object {
        val TAG: String = AdminHomeFragment::class.java.simpleName

    }
}
