package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.databinding.ActivityLoginBinding
import com.example.librewards.utils.toastMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding

    companion object {
        val TAG: String = Login::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

        binding.registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
            finish()
        }

        binding.loginButton.setOnClickListener {
            if (binding.loginEmail.text.toString() == "" || binding.loginPassword.text.toString() == "") {
                toastMessage(this, "Please ensure all fields are correctly filled out.")
            } else {
                signIn()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        openUserApp()
    }

    private fun signIn() {
        auth.signInWithEmailAndPassword(
            binding.loginEmail.text.toString(),
            binding.loginPassword.text.toString(),
        )
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    openUserApp()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    toastMessage(this, "Authentication failed.")
                }
            }
    }

    private fun openUserApp() {
        auth.currentUser?.getIdToken(true)?.addOnSuccessListener {
            val isAdmin = it.claims["admin"]
            if (isAdmin == true) {
                val adminIntent = Intent(this@Login, AdminActivity::class.java)
                startActivity(adminIntent)
                finish()
            } else {
                val mainIntent = Intent(this@Login, MainActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        }
    }
}
