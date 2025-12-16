package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.ActivityRegisterBinding
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository
import com.example.librewards.resources.universities
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.RegisterStatus
import com.example.librewards.viewmodels.RegisterViewModel
import com.example.librewards.viewmodels.RegisterViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {
    private lateinit var uniSelected: String
    private var spinnerPos: Int? = null
    private val registerViewModel: RegisterViewModel by viewModels {
        val database = FirebaseDatabase.getInstance().reference
        val userRepo = UserRepository(database)
        RegisterViewModelFactory(Firebase.auth, userRepo)
    }
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeRegisterStatus()

        binding.backToLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.registrationSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.d(TAG, "Unselected")
                }

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    spinnerPos = position
                    if (position == 0) {
                        Log.d(TAG, "First element in spinner")
                    } else {
                        uniSelected = parent?.getItemAtPosition(position).toString()
                        toastMessage(this@Register, "You selected: $uniSelected")
                    }
                }
            }

        loadSpinnerData()

        binding.registerHereButton.setOnClickListener {
            if (binding.registrationEmail.text.toString() == "" ||
                binding.registrationPassword.text.toString() == "" ||
                binding.registrationFirstName.text.toString() == "" ||
                binding.registrationLastName.text.toString() == "" ||
                spinnerPos == 0
            ) {
                toastMessage(this, "Please ensure all fields are correctly filled out.")
            } else {
                signUp()
            }
        }
    }

    private fun signUp() {
        val user = User(
            binding.registrationFirstName.text.toString(),
            binding.registrationLastName.text.toString(),
            binding.registrationEmail.text.toString(),
            uniSelected,
        )

        registerViewModel.signUp(user, binding.registrationPassword.text.toString())
    }

    private fun observeRegisterStatus() {
        registerViewModel.registerStatus.observe(this) { status ->
            when (status) {
                RegisterStatus.Registered -> {
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                }

                RegisterStatus.Failed -> {
                    toastMessage(this, "Authentication failed.")
                }
            }
        }
    }

    private fun loadSpinnerData() {
        val uniList: MutableList<String> = universities.toMutableList()
        // Spinner Drop down elements
        uniList.add(0, "Please choose a University")
        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(
            this,
            R.layout.spinner_text, uniList,
        )
        // Drop down layout style - list view with radio button
        dataAdapter
            .setDropDownViewResource(R.layout.simple_spinner_dropdown)

        // attaching data adapter to spinner
        binding.registrationSpinner.adapter = dataAdapter
    }

    companion object {
        val TAG: String = Register::class.java.simpleName
    }
}
