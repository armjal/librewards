package com.example.librewards.ui.admin

import android.Manifest
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.librewards.R
import com.example.librewards.databinding.AdminFragmentHomeBinding
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.toastMessage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class AdminHomeFragment(override val icon: Int = R.drawable.home) : FragmentExtended() {
    private val viewModel: AdminHomeViewModel by viewModels {
        val adminSharedViewModel: AdminSharedViewModel by activityViewModels()
        AdminHomeViewModelFactory(adminSharedViewModel)
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
                    toastMessage(requireActivity(), getString(R.string.camera_permission_required))
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
        viewModel.studentRewardStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                StudentRewardStatus.Redeemed -> toastMessage(requireActivity(), getString(R.string.reward_redeemed))
                StudentRewardStatus.CantRedeem -> toastMessage(requireActivity(), getString(R.string.student_not_prepared))
                StudentRewardStatus.Error -> toastMessage(requireActivity(), getString(R.string.error_redeeming_reward))
                else -> {}
            }
        }
    }

    private fun observeStudentTimerStatus() {
        viewModel.studentTimerStatus.observe(viewLifecycleOwner) { status ->
            if (status == null) return@observe
            when (status) {
                StudentTimerStatus.Started -> toastMessage(requireActivity(), getString(R.string.timer_started))
                StudentTimerStatus.Stopped -> toastMessage(requireActivity(), getString(R.string.timer_stopped))
                StudentTimerStatus.Error -> toastMessage(requireActivity(), getString(R.string.error_starting_timer))
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
                    toastMessage(requireActivity(), getString(R.string.no_barcode_found))
                }
            }
            .addOnCanceledListener { toastMessage(requireActivity(), getString(R.string.scan_cancelled)) }
            .addOnFailureListener { e -> toastMessage(requireContext(), getString(R.string.scan_failed, e.message)) }
    }
}
