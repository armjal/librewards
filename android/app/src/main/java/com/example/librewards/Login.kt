package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.ActivityLoginBinding
import com.example.librewards.models.User
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database


class Login : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        database = Firebase.database.reference

        binding.registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener {
            if (binding.loginEmail.text.toString() == "" || binding.loginPassword.text.toString() == "") {
                Toast.makeText(
                    baseContext,
                    "Please ensure all fields are correctly filled out.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                signIn()
            }
        }
    }

    private fun getUserLoginInfo(email: String, activity: AppCompatActivity) {
        val id = hashFunction(email)
        val refChild = database.child("users").child(id)
        refChild.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                val intent = Intent(this@Login, activity::class.java)
                intent.putExtra("email", user?.email)
                intent.putExtra("first_name", user?.firstname)
                intent.putExtra("last_name", user?.surname)
                intent.putExtra("university", user?.university)
                startActivity(intent)
                finish()

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    public override fun onStart() {
        super.onStart()
        openUserApp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun signIn() {
        auth.signInWithEmailAndPassword(
            binding.loginEmail.text.toString(),
            binding.loginPassword.text.toString()
        )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    openUserApp()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun openUserApp(){
        auth.currentUser?.getIdToken(true)?.addOnSuccessListener {
            val isAdmin = it.claims["admin"]
            if (isAdmin == true) {
                getUserLoginInfo(auth.currentUser?.email.toString(), AdminActivity())
            } else {
                getUserLoginInfo(auth.currentUser?.email.toString(), MainActivity())
            }
        }
    }

    companion object {
        val TAG: String = Login::class.java.simpleName
    }

}
