package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.FragmentFacebookUniversityBinding
import com.example.librewards.resources.universities

class FacebookUniversity : AppCompatActivity() {
    private var spinnerPos: Int? = null
    private lateinit var uniSelected: String
    private lateinit var login: Login
    private lateinit var fh: FirebaseHandler
    private var _binding: FragmentFacebookUniversityBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentFacebookUniversityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        login = Login()
        fh = FirebaseHandler()

        binding.fbUniversitySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.d("FacebookUniversity", "Unselected")
                }
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    spinnerPos = position
                    if (position == 0) {
                        Log.d("FacebookUniversity", "First element in spinner")
                    } else {
                        // On selecting a spinner item
                        uniSelected = parent?.getItemAtPosition(position).toString()

                        // Showing selected spinner item
                        Toast.makeText(
                            parent?.context, "You selected: $uniSelected",
                            Toast.LENGTH_LONG
                        ).show()

                    }
                }
            }
        loadSpinnerData()
        binding.selectUniversityButton.setOnClickListener {
            if (spinnerPos != 0) {
                val extras = intent.extras
                val email = extras?.getString("email")
                val firstName = extras?.getString("first_name")
                val lastName = extras?.getString("last_name")
                val photoUrl = extras?.getString("photo")
                fh.getChild("users", email!!, "university").setValue(uniSelected)
                val intent = Intent(this@FacebookUniversity, MainActivity::class.java)
                intent.putExtra("university", uniSelected)
                intent.putExtra("email", email)
                intent.putExtra("photo", photoUrl)
                intent.putExtra("last_name", lastName)
                intent.putExtra("first_name", firstName)
                fh.writeNewUser(email, firstName!!, lastName!!, email, uniSelected)

                startActivity(intent)
            } else {
                Toast.makeText(
                    baseContext,
                    "Please ensure you have selected a University",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadSpinnerData() {
        // Spinner Drop down elements
        val universities: MutableList<String> = universities as MutableList<String>
        universities.add(0, "Please choose a University")
        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, universities
        )

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // attaching data adapter to spinner
        binding.fbUniversitySpinner.adapter = dataAdapter
    }

}