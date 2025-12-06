package com.example.librewards

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.librewards.adapters.ViewPagerAdapter
import com.example.librewards.databinding.ActivityAdminBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AdminActivity : AppCompatActivity() {
    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var lastName: String
    lateinit var university: String

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sets the layout to the XML file associated with it
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialiseVariables()
        binding.adminProfileImage.setOnClickListener { logoutApp() }
        ("$firstName $lastName").also { binding.adminUsername.text = it }

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.adminViewPager.adapter = viewPagerAdapter
        val fragments = listOf(AdminHomeFragment(), AdminRewardsFragment())
        viewPagerAdapter.addFragments(fragments)
        TabLayoutMediator(binding.adminTabLayout, binding.adminViewPager) { tab, position ->
            tab.icon = ContextCompat.getDrawable(this, fragments[position].icon)
        }.attach()
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

    private fun initialiseVariables() {
        val extras = intent.extras
        email = extras?.getString("email").toString()
        firstName = extras?.getString("first_name").toString()
        firstName = firstName[0].uppercaseChar().toString() + firstName.substring(1)
        lastName = extras?.getString("last_name").toString()
        lastName = lastName[0].uppercaseChar().toString() + lastName.substring(1)
        university = extras?.getString("university").toString()
    }

    companion object {
        val TAG: String = AdminActivity::class.java.simpleName
    }
}
