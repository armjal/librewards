package com.example.librewards

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.viewModels
import com.example.librewards.databinding.AdminFragmentHomeBinding
import com.example.librewards.repositories.UserRepository
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.AdminHomeViewModel
import com.example.librewards.viewmodels.AdminHomeViewModelFactory
import com.example.librewards.viewmodels.StudentRewardStatus
import com.example.librewards.viewmodels.StudentTimerStatus
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class AdminHomeFragment(override val icon: Int = R.drawable.home) : FragmentExtended() {
    private val viewModel: AdminHomeViewModel by viewModels {
        val database = FirebaseDatabase.getInstance().reference
        AdminHomeViewModelFactory(UserRepository(database))
    }

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private var _binding: AdminFragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var scanner: GmsBarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    toastMessage(requireActivity(), "Camera permission is required to scan barcodes.")
                }
            }
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        scanner = GmsBarcodeScanning.getClient(requireContext(), options)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AdminFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeStudentRewardStatus()
        observeStudentTimerStatus()
        setupButtonListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupButtonListeners() {
        binding.scanTimerButton.setOnClickListener {
            startScanner(viewModel::toggleStudentTimer)
        }
        binding.scanRewardButton.setOnClickListener {
            startScanner(viewModel::redeemRewardForStudent)
        }

        binding.toggleTimerButton.setOnClickListener { viewModel.toggleStudentTimer(binding.enterQr.text.toString()) }
        binding.redeemRewardButton.setOnClickListener { viewModel.redeemRewardForStudent(binding.enterQr.text.toString()) }
    }

    private fun observeStudentRewardStatus() {
        viewModel.studentRewardStatus.observe(viewLifecycleOwner) {
            when (it) {
                StudentRewardStatus.Redeemed -> toastMessage(requireActivity(), "Reward redeemed for student")
                StudentRewardStatus.CantRedeem -> toastMessage(requireActivity(), "Student not prepared to redeem reward")
                StudentRewardStatus.Error -> toastMessage(requireActivity(), "Error redeeming reward")
                else -> {}
            }
        }
    }

    private fun observeStudentTimerStatus() {
        viewModel.studentTimerStatus.observe(viewLifecycleOwner) {
            if (it == null) return@observe
            when (it) {
                StudentTimerStatus.Started -> toastMessage(requireActivity(), "Student timer started")
                StudentTimerStatus.Stopped -> toastMessage(requireActivity(), "Student timer stopped")
                StudentTimerStatus.Error -> toastMessage(requireActivity(), "Error starting student timer")
            }
        }
    }

    private fun checkCameraPermission(): Boolean = checkSelfPermission(
        requireActivity(),
        Manifest.permission.CAMERA,
    ) == PERMISSION_GRANTED

    private fun startScanner(actionForStudent: (String) -> Unit) {
        if (!checkCameraPermission()) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue != null) {
                    actionForStudent(rawValue)
                } else {
                    toastMessage(requireActivity(), "No barcode found")
                }
            }
            .addOnCanceledListener { toastMessage(requireActivity(), "Scan cancelled") }
            .addOnFailureListener { e -> toastMessage(requireContext(), "Scan failed: ${e.message}") }
    }
}
