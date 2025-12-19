package com.example.librewards

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.librewards.adapters.ViewPagerAdapter
import com.example.librewards.databinding.ActivityAdminBinding
import com.example.librewards.repositories.UserRepository
import com.example.librewards.viewmodels.AdminViewModel
import com.example.librewards.viewmodels.AdminViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class AdminActivity : AppCompatActivity() {
    private val viewModel: AdminViewModel by viewModels {
        val userRepo = UserRepository(FirebaseDatabase.getInstance().reference)
        AdminViewModelFactory(userRepo)
    }

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets the layout to the XML file associated with it
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.adminProfileImage.setOnClickListener { logoutApp() }

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.adminViewPager.adapter = viewPagerAdapter
        val fragments = listOf(AdminHomeFragment(), AdminRewardsFragment())
        viewPagerAdapter.addFragments(fragments)
        TabLayoutMediator(binding.adminTabLayout, binding.adminViewPager) { tab, position ->
            tab.icon = ContextCompat.getDrawable(this, fragments[position].icon)
        }.attach()

        viewModel.setUser(Firebase.auth.currentUser?.email!!)
        observeUser()
    }

    private fun observeUser() {
        viewModel.user.observe(this) {
            if (it == null) return@observe
            binding.adminUsername.text = "${it.firstname} ${it.surname}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
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

    companion object {
        val TAG: String = AdminActivity::class.java.simpleName
    }
}
