package com.example.librewards

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.librewards.adapters.ViewPagerAdapter
import com.example.librewards.databinding.ActivityMainBinding
import com.example.librewards.repositories.UserRepository
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.MainViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val userRepo = UserRepository(FirebaseDatabase.getInstance().reference)
    val mainSharedViewModel: MainSharedViewModel by viewModels {
        MainViewModelFactory(userRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets the layout to the XML file associated with it
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        val timerFragment = TimerFragment()
        val rewardsFragment = RewardsFragment()

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        val fragments = listOf(timerFragment, rewardsFragment)
        viewPagerAdapter.addFragments(fragments)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.icon = ContextCompat.getDrawable(this, fragments[position].icon)
        }.attach()

        binding.profileImage.setOnClickListener {
            logoutApp()
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        mainSharedViewModel.startObservingUser(currentUser?.email!!)
        observeUser()
        mainSharedViewModel.createQRCode()
        mainSharedViewModel.panelSlideOffset.observe(this) { slideOffset ->
            val alpha = (1.3 - slideOffset).toFloat()
            binding.appBarLayout.alpha = alpha
            binding.profileImage.alpha = alpha
            binding.logo.alpha = alpha
            binding.tabLayout.alpha = alpha

            val panelIsUp = slideOffset > 0.9
            binding.profileImage.isClickable = !panelIsUp
            binding.tabLayout.touchables.forEach { it?.isEnabled = !panelIsUp }
            binding.viewPager.isUserInputEnabled = !panelIsUp
        }
    }

    private fun observeUser() {
        mainSharedViewModel.user.observe(this) {
            if (it == null) return@observe
            binding.username.text = "${it.firstname} ${it.surname}"
        }
    }

    private fun logoutApp() {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            auth.signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            supportFragmentManager.popBackStack()
            startActivity(intent)
            finish()
        }
    }
}
