package com.example.librewards

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.ActivityMainBinding
import com.example.librewards.repositories.UserRepository
import com.example.librewards.utils.setupWithFragments
import com.example.librewards.utils.startLibRewardsActivity
import com.example.librewards.viewmodels.LoginStatus
import com.example.librewards.viewmodels.LoginViewModel
import com.example.librewards.viewmodels.LoginViewModelFactory
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.MainViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val userRepo = UserRepository(FirebaseDatabase.getInstance().reference)
    val mainSharedViewModel: MainSharedViewModel by viewModels {
        MainViewModelFactory(userRepo)
    }
    val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(Firebase.auth)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFragmentsView()
        setupLogoutListener()
        setupObservers()
        mainSharedViewModel.createQRCode()
    }

    private fun setupFragmentsView() {
        val fragments = listOf(TimerFragment(), RewardsFragment())
        binding.viewPager.setupWithFragments(this, binding.tabLayout, fragments)
    }

    private fun setupLogoutListener() {
        binding.profileImage.setOnClickListener {
            mainSharedViewModel.userRepo.stopAllListeners()
            loginViewModel.logout()
        }
    }

    private fun setupObservers() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        mainSharedViewModel.startObservingUser(currentUser?.email!!)
        observeUser()
        observeLoginStatus()
        observePanelSlider()
    }

    private fun observeUser() {
        mainSharedViewModel.user.observe(this) {
            if (it == null) return@observe
            binding.username.text = "${it.firstname} ${it.surname}"
        }
    }

    private fun observePanelSlider() {
        mainSharedViewModel.panelSlideOffset.observe(this) { slideOffset ->
            val alpha = (1.3 - slideOffset).toFloat()
            with(binding) {
                listOf(appBarLayout, profileImage, logo, tabLayout).forEach {
                    it.alpha = alpha
                }

                val panelIsUp = slideOffset > 0.1
                profileImage.isClickable = !panelIsUp
                tabLayout.touchables.forEach { it?.isEnabled = !panelIsUp }
                viewPager.isUserInputEnabled = !panelIsUp
            }
        }
    }

    private fun observeLoginStatus() {
        loginViewModel.loginState.observe(this) { status ->
            when (status) {
                LoginStatus.LoggedOut -> {
                    startLibRewardsActivity(Login::class.java, isLogOut = true)
                }
                else -> {}
            }
        }
    }
}
