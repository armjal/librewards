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
            val shadowAlpha = (slideOffset * 0.6f).coerceIn(0f, 0.6f)
            with(binding) {
                shadowOverlay.alpha = shadowAlpha

                val isPanelActive = slideOffset > 0.01f
                profileImage.isClickable = !isPanelActive
                val tabStrip = tabLayout.getChildAt(0) as? android.view.ViewGroup
                tabStrip?.let {
                    for (i in 0 until it.childCount) {
                        it.getChildAt(i).isEnabled = !isPanelActive
                    }
                }
                viewPager.isUserInputEnabled = !isPanelActive
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
