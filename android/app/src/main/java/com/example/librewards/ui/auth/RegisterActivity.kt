package com.example.librewards.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.data.repositories.UserRepository
import com.example.librewards.data.resources.universities
import com.example.librewards.databinding.ActivityRegisterBinding
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.getDbReference
import com.example.librewards.utils.startLibRewardsActivity
import com.example.librewards.utils.toastMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class RegisterActivity : AppCompatActivity() {
    companion object {
        val TAG: String = RegisterActivity::class.java.simpleName
    }

    private val registerViewModel: RegisterViewModel by viewModels {
        val userRepo = UserRepository(getDbReference("users"))
        RegisterViewModelFactory(Firebase.auth, userRepo)
    }
    private lateinit var binding: ActivityRegisterBinding
    private var uniSelected: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeRegisterStatus()
        setupButtonListeners()
        setupSpinner()
    }

    private fun setupButtonListeners() {
        binding.backToLogin.setOnClickListener {
            startLibRewardsActivity(LoginActivity::class.java)
        }

        binding.registerHereButton.setOnClickListener {
            signUp()
        }
    }

    private fun setupSpinner() {
        loadSpinnerData()
        binding.registrationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "No university selected")
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    Log.d(TAG, "Placeholder university selected")
                } else {
                    uniSelected = parent?.getItemAtPosition(position).toString()
                    toastMessage(this@RegisterActivity, getString(R.string.selected_uni, uniSelected))
                }
            }
        }
    }

    private fun signUp() {
        val user: User
        try {
            user = User.create(
                binding.registrationFirstName.text.toString(),
                binding.registrationLastName.text.toString(),
                binding.registrationEmail.text.toString(),
                uniSelected,
            )
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Validation of user registration request failed: ${e.message}")
            toastMessage(this, getString(R.string.error_msg_empty_fields))
            return
        }

        registerViewModel.signUp(user, binding.registrationPassword.text.toString())
    }

    private fun observeRegisterStatus() {
        registerViewModel.registerStatus.observe(this) { status ->
            when (status) {
                RegisterStatus.Registered -> {
                    startLibRewardsActivity(MainActivity::class.java)
                }

                RegisterStatus.RegisteredWithoutLogin -> {
                    startLibRewardsActivity(LoginActivity::class.java)
                }

                RegisterStatus.Failed -> {
                    toastMessage(this, getString(R.string.auth_failed))
                }
            }
        }
    }

    private fun loadSpinnerData() {
        val uniList: MutableList<String> = universities.toMutableList()
        uniList.add(0, getString(R.string.choose_uni))
        val dataAdapter = ArrayAdapter(this, R.layout.spinner_text, uniList)
        dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown)
        binding.registrationSpinner.adapter = dataAdapter
    }
}
