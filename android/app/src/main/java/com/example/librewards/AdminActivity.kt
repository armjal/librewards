package com.example.librewards

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.librewards.databinding.ActivityAdminBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AdminActivity : AppCompatActivity() {
    private lateinit var email: String
    private lateinit var firstName: String
    private lateinit var lastName: String
    lateinit var university: String

    private lateinit var timerFragment: TimerFragment
    private lateinit var adminHomeFragment: AdminHomeFragment
    private lateinit var adminRewardsFragment: AdminRewardsFragment
    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Sets the layout to the XML file associated with it
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialiseVariables()
        binding.adminProfileImage.setOnClickListener { logoutApp() }
        ("$firstName $lastName").also { binding.adminUsername.text = it }
        timerFragment = TimerFragment()
        adminHomeFragment = AdminHomeFragment()
        adminRewardsFragment = AdminRewardsFragment()
        binding.adminTabLayout.setupWithViewPager(binding.adminViewPager)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, 0)
        viewPagerAdapter.addFragment(adminHomeFragment, "")
        viewPagerAdapter.addFragment(adminRewardsFragment, "")
        binding.adminViewPager.adapter = viewPagerAdapter
        binding.adminTabLayout.getTabAt(0)?.setIcon(R.drawable.home)
        binding.adminTabLayout.getTabAt(1)?.setIcon(R.drawable.reward)

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

    private class ViewPagerAdapter(fm: FragmentManager, behavior: Int) :
        FragmentPagerAdapter(fm, behavior) {
        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentTitle: MutableList<String> = ArrayList()
        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitle.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitle[position]
        }


    }


}
