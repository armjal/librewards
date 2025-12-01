package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.librewards.adapters.ViewPagerAdapter
import com.example.librewards.databinding.ActivityMainBinding
import com.example.librewards.databinding.PopupLayoutBinding
import com.example.librewards.repositories.UserRepository
import com.example.librewards.viewmodels.MainViewModel
import com.example.librewards.viewmodels.MainViewModelFactory
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {
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
        FirebaseApp.initializeApp(this)

        timerFragment = TimerFragment()
        rewardsFragment = RewardsFragment()
        fh = FirebaseHandler()
        database = FirebaseDatabase.getInstance().reference


        initialiseVariables()

        Picasso.get().load(photoURL).into(binding.profileImage)

        "$firstName $lastName".also { binding.username.text = it }
        val mainViewModel: MainViewModel by viewModels {
            MainViewModelFactory(UserRepository(database))
        }
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
        mainViewModel.getUser(email)
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
}
