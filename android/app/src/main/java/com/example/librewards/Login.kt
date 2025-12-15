package com.example.librewards

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.librewards.databinding.ActivityLoginBinding
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.LoginStatus
import com.example.librewards.viewmodels.LoginViewModel
import com.example.librewards.viewmodels.LoginViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(Firebase.auth)
    }

    companion object {
        val TAG: String = Login::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRegistrationButtonListener()
        setupLoginButtonListener()
    }

    public override fun onStart() {
        super.onStart()
        openUserApp()
    }

    private fun setupRegistrationButtonListener() {
        binding.registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupLoginButtonListener() {
        binding.loginButton.setOnClickListener {
            if (binding.loginEmail.text.isEmpty() || binding.loginPassword.text.isEmpty()) {
                toastMessage(this@Login, getString(R.string.error_msg_empty_fields))
            } else {
                login()
            }
        }
    }

    private fun login() {
        lifecycleScope.launch {
            val loginFlow = loginViewModel.login(binding.loginEmail.text.toString(), binding.loginPassword.text.toString())

            loginFlow.collect { status ->
                when (status) {
                    LoginStatus.Successful -> {
                        openUserApp()
                    }

                    LoginStatus.Failed -> {
                        toastMessage(this@Login, getString(R.string.auth_failed))
                    }
                }
            }
        }
    }

    private fun openUserApp() {
        Firebase.auth.currentUser?.getIdToken(true)?.addOnSuccessListener {
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
