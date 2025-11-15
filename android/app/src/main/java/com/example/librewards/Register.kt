package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {
    private lateinit var localDb: DatabaseHandler
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var uniSelected: String
    private var spinnerPos: Int? = null
    private lateinit var fh: FirebaseHandler
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        localDb = DatabaseHandler(applicationContext)
        fh = FirebaseHandler()
        database = FirebaseDatabase.getInstance().reference
        //storeUniversities()

        binding.backToLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.registrationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                spinnerPos = position
                if (position == 0) {
                    Log.d(TAG, "First element in spinner")
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

        binding.registerHereButton.setOnClickListener {
            if (binding.registrationEmail.text.toString() == "" || binding.registrationPassword.text.toString() == "" || binding.registrationFirstName.text.toString() == "" || binding.registrationLastName.text.toString() == "" || spinnerPos == 0) {
                Toast.makeText(
                    baseContext,
                    "Please ensure all fields are correctly filled out.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                signUp()
            }
        }

    }

    private fun signUp() {
        auth.createUserWithEmailAndPassword(
            binding.registrationEmail.text.toString(),
            binding.registrationPassword.text.toString()
        )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    fh.writeNewUser(
                        binding.registrationEmail.text.toString(),
                        binding.registrationFirstName.text.toString(),
                        binding.registrationLastName.text.toString(),
                        binding.registrationEmail.text.toString(),
                        uniSelected
                    )
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)

                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(null)
                }
            }
    }

    private fun loadSpinnerData() {
        val listFromFile = ListFromFile(applicationContext)
        val uniList: MutableList<String> = listFromFile.readLine("universities.txt")
        // Spinner Drop down elements
        uniList.add(0, "Please choose a University")
        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(
            this,
            R.layout.spinner_text, uniList
        )
        // Drop down layout style - list view with radio button
        dataAdapter
            .setDropDownViewResource(R.layout.simple_spinner_dropdown)

        // attaching data adapter to spinner
        binding.registrationSpinner.adapter = dataAdapter
    }


    private fun updateUI(user: FirebaseUser?) {
    }

    companion object {
        val TAG: String = Register::class.java.simpleName
    }
}
