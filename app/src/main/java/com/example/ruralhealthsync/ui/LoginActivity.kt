package com.example.ruralhealthsync.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.example.ruralhealthsync.data.remote.LoginRequest
import com.example.ruralhealthsync.data.remote.RetrofitClient
import com.example.ruralhealthsync.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceManager = PreferenceManager(this)
        super.onCreate(savedInstanceState)
        if (preferenceManager.isLoggedIn()) {
            startActivity(Intent(this, PatientListActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.serverUrlInput.setText(preferenceManager.getServerUrlDisplay())
        setupListeners()
    }

    private fun setupListeners() {
        binding.authButton.setOnClickListener {
            performAuth()
        }
    }

    private fun performAuth() {
        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString()?.trim().orEmpty()
        val serverInput = binding.serverUrlInput.text?.toString()?.trim().orEmpty()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (serverInput.isEmpty()) {
            Toast.makeText(this, "Enter your PC's IP address", Toast.LENGTH_SHORT).show()
            return
        }

        preferenceManager.saveServerUrl(serverInput)
        RetrofitClient.invalidateCache()

        binding.progressBar.visibility = View.VISIBLE
        binding.authButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@LoginActivity)
                val response = apiService.login(LoginRequest(username, password))

                if (response.isSuccessful && response.body()?.success == true) {
                    val authData = response.body()!!
                    val role = PreferenceManager.normalizeRole(authData.role ?: "CHW")
                    preferenceManager.saveUser(authData.userId!!, authData.fullName!!, role)
                    startActivity(Intent(this@LoginActivity, PatientListActivity::class.java))
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: "Authentication failed"
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val message = connectionErrorMessage(e)
                Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.authButton.isEnabled = true
            }
        }
    }

    private fun connectionErrorMessage(e: Exception): String {
        val host = preferenceManager.getServerUrlDisplay()
        val networkFailure = generateSequence(e as Throwable?) { it.cause }
            .any {
                it is ConnectException ||
                    it is UnknownHostException ||
                    it is SocketTimeoutException ||
                    it.message?.contains("failed to connect", ignoreCase = true) == true ||
                    it.message?.contains("unable to resolve host", ignoreCase = true) == true
            }
        return if (networkFailure) {
            "Cannot reach server at $host. On PC run ipconfig and use the Wi‑Fi IPv4 address. Start Apache in XAMPP, same Wi‑Fi as phone, allow port 80 in Windows Firewall."
        } else {
            "Error: ${e.message}"
        }
    }
}
