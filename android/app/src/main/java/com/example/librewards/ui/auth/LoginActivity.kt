package com.example.librewards.ui.auth

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.librewards.R
import com.example.librewards.databinding.ActivityLoginBinding
import com.example.librewards.ui.admin.AdminActivity
import com.example.librewards.ui.main.MainActivity
import com.example.librewards.utils.startLibRewardsActivity
import com.example.librewards.utils.toastMessage
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(Firebase.auth)
    }

    companion object {
        val TAG: String = LoginActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRegistrationButtonListener()
        setupLoginButtonListener()
        setupNavigationObserver()
    }

    private fun setupRegistrationButtonListener() {
        binding.registerButton.setOnClickListener {
            startLibRewardsActivity(RegisterActivity::class.java)
        }
    }

    private fun setupLoginButtonListener() {
        binding.loginButton.setOnClickListener {
            if (binding.loginEmail.text.isEmpty() || binding.loginPassword.text.isEmpty()) {
                toastMessage(this@LoginActivity, getString(R.string.error_msg_empty_fields))
            } else {
                login()
            }
        }
    }

    private fun login() {
        lifecycleScope.launch {
            val loginFlow = loginViewModel.login(binding.loginEmail.text.toString(), binding.loginPassword.text.toString())

            loginFlow.take(1).collect { status ->
                when (status) {
                    LoginStatus.Failed -> {
                        toastMessage(this@LoginActivity, getString(R.string.auth_failed))
                    }

                    else -> {}
                }
            }
        }
    }

    private fun setupNavigationObserver() {
        loginViewModel.isAdmin.observe(this) { isAdmin ->
            if (isAdmin == null) return@observe
            if (isAdmin) {
                startLibRewardsActivity(AdminActivity::class.java)
            } else {
                startLibRewardsActivity(MainActivity::class.java)
            }
        }
    }
}
