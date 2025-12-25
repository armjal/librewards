package com.example.librewards.ui.admin

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.data.repositories.ProductRepository
import com.example.librewards.data.repositories.StorageRepository
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.databinding.ActivityAdminBinding
import com.example.librewards.ui.auth.LoginActivity
import com.example.librewards.utils.getDbReference
import com.example.librewards.utils.setupWithFragments
import com.example.librewards.utils.startLibRewardsActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.storage

class AdminActivity : AppCompatActivity() {
    companion object {
        val TAG: String = AdminActivity::class.java.simpleName
    }

    private val viewModel: AdminSharedViewModel by viewModels {
        val userRepo = UserRepository(getDbReference("users"))
        val productRepo = ProductRepository(getDbReference("products"))
        val storageRepo = StorageRepository(Firebase.storage.reference)
        AdminSharedViewModelFactory(userRepo, productRepo, storageRepo)
    }

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtonListeners()
        setupFragmentsView()
        viewModel.initialiseStateOnUserRetrieval(Firebase.auth.currentUser?.email!!)
        observeUser()
    }

    private fun setupFragmentsView() {
        val fragmentsToCreate = listOf(AdminHomeFragment(), AdminRewardsFragment())
        binding.adminViewPager.setupWithFragments(this, binding.adminTabLayout, fragmentsToCreate)
    }

    private fun setupButtonListeners() {
        binding.adminProfileImage.setOnClickListener {
            viewModel.stopListeningToData()
            logoutApp()
        }
    }

    private fun observeUser() {
        viewModel.user.observe(this) {
            if (it == null) return@observe
            binding.adminUsername.text = "${it.firstname} ${it.surname}"
        }
    }

    private fun logoutApp() {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            auth.signOut()
            startLibRewardsActivity(LoginActivity::class.java, isLogOut = true)
        }
    }
}
