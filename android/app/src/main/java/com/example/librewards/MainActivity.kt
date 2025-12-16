package com.example.librewards

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.librewards.adapters.ViewPagerAdapter
import com.example.librewards.databinding.ActivityMainBinding
import com.example.librewards.repositories.UserRepository
import com.example.librewards.viewmodels.LoginStatus
import com.example.librewards.viewmodels.LoginViewModel
import com.example.librewards.viewmodels.LoginViewModelFactory
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.MainViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
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
        val timerFragment = TimerFragment()
        val rewardsFragment = RewardsFragment()

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        val fragments = listOf(timerFragment, rewardsFragment)
        viewPagerAdapter.addFragments(fragments)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.icon = ContextCompat.getDrawable(this, fragments[position].icon)
        }.attach()
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
                    exitActivity()
                }
                else -> {}
            }
        }
    }

    private fun exitActivity() {
        val intent = Intent(this, Login::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
