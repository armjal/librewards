package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.example.librewards.RewardsFragment.RewardsListener
import com.example.librewards.TimerFragment.TimerListener
import com.example.librewards.databinding.ActivityMainBinding
import com.example.librewards.databinding.PopupLayoutBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), TimerListener, RewardsListener {
    private lateinit var timerFragment: TimerFragment
    private lateinit var rewardsFragment: RewardsFragment
    lateinit var email: String
    lateinit var firstName: String
    lateinit var lastName: String
    lateinit var photoURL: String
    lateinit var university: String
    private lateinit var database: DatabaseReference
    private lateinit var fh: FirebaseHandler
    private var popupLayoutBinding: PopupLayoutBinding? = null
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // Exposing views for fragments temporarily
    val profileImage: ImageView get() = binding.profileImage
    val logo: ImageView get() = binding.logo
    val appBarLayout: AppBarLayout get() = binding.appBarLayout
    val tabLayout: TabLayout get() = binding.tabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Sets the layout to the XML file associated with it
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Assigns the field to the view's specified in the fragment_timer XML file file
        timerFragment = TimerFragment()
        rewardsFragment = RewardsFragment()
        fh = FirebaseHandler()
        database = FirebaseDatabase.getInstance().reference


        initialiseVariables()

        //val navLayout = setContentView(R.layout.nav_header)
        Picasso.get().load(photoURL).into(binding.profileImage)

        "$firstName $lastName".also { binding.username.text = it }

        binding.tabLayout.setupWithViewPager(binding.viewPager)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, 0)
        viewPagerAdapter.addFragment(timerFragment, "")
        viewPagerAdapter.addFragment(rewardsFragment, "")
        binding.viewPager.adapter = viewPagerAdapter
        binding.tabLayout.getTabAt(0)?.setIcon(R.drawable.timer)
        binding.tabLayout.getTabAt(1)?.setIcon(R.drawable.reward)

        binding.profileImage.setOnClickListener {
            logoutApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        popupLayoutBinding = null
    }

    private fun initialiseVariables() {
        val extras = intent.extras
        email = extras?.getString("email").toString()
        firstName = extras?.getString("first_name").toString()
        lastName = extras?.getString("last_name").toString()
        university = extras?.getString("university").toString()
        photoURL = extras?.getString("photo").toString()

    }

    private fun logoutApp() {
        val auth = Firebase.auth
        if (auth.currentUser != null) {
            auth.signOut()
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            supportFragmentManager.popBackStack()
            startActivity(intent)
            finish()
        }
    }

    //Using the interface in both fragments, the main activity is able to facilitate communication between the two fragments. Here, it sets the points in each fragment each time
    //it's updated
    override fun onPointsRewardsSent(points: Int) {
        timerFragment.updatePoints(points)
    }

    override fun onPointsTimerSent(points: Int) {
        rewardsFragment.updatedPoints(points)
    }

    //Using a tab layout tutorial from YouTube user @Coding In Flow, I was able to create a tab layout and customise it so it fit my theme.
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
