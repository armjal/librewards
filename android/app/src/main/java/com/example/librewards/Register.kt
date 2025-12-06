package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.ActivityRegisterBinding
import com.example.librewards.models.User
import com.example.librewards.repositories.UserRepository
import com.example.librewards.resources.universities
import com.example.librewards.utils.toastMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var uniSelected: String
    private var spinnerPos: Int? = null
    private lateinit var userRepo: UserRepository
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        val database = FirebaseDatabase.getInstance().reference
        userRepo = UserRepository(database)

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
        auth.createUserWithEmailAndPassword(
            binding.registrationEmail.text.toString(),
            binding.registrationPassword.text.toString(),
        )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = User(
                        binding.registrationFirstName.text.toString(),
                        binding.registrationLastName.text.toString(),
                        binding.registrationEmail.text.toString(),
                        uniSelected,
                    )
                    userRepo.addUser(user)
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)

                    toastMessage(this, "Authentication failed.")
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
