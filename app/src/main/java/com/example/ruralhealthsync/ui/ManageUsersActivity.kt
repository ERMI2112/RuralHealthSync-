package com.example.ruralhealthsync.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.example.ruralhealthsync.data.remote.ManageUsersRequest
import com.example.ruralhealthsync.data.remote.RetrofitClient
import com.example.ruralhealthsync.data.remote.UserDto
import com.example.ruralhealthsync.databinding.ActivityManageUsersBinding
import kotlinx.coroutines.launch

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var userAdapter: UserAdapter
    private var adminId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        adminId = preferenceManager.getUserId()

        if (!preferenceManager.isAdmin()) {
            Toast.makeText(this, "Unauthorized", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        loadUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter { user ->
            confirmDeleteUser(user)
        }
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usersRecyclerView.adapter = userAdapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationIcon(com.example.ruralhealthsync.R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.addUserFab.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(com.example.ruralhealthsync.R.layout.dialog_add_user, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val fullNameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.ruralhealthsync.R.id.fullNameInput)
        val usernameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.ruralhealthsync.R.id.usernameInput)
        val passwordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(com.example.ruralhealthsync.R.id.passwordInput)
        val roleSpinner = dialogView.findViewById<android.widget.Spinner>(com.example.ruralhealthsync.R.id.roleSpinner)
        val createButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.example.ruralhealthsync.R.id.createButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(com.example.ruralhealthsync.R.id.cancelButton)

        createButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val role = roleSpinner.selectedItem.toString()

            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createUser(fullName, username, password, role)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createUser(fullName: String, username: String, password: String, role: String) {
        binding.loadingProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = ManageUsersRequest(
                    adminId = adminId,
                    action = "create",
                    username = username,
                    password = password,
                    full_name = fullName,
                    role = role
                )
                val response = RetrofitClient.getApiService(this@ManageUsersActivity).manageUsers(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ManageUsersActivity, "User created successfully", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    val msg = response.body()?.message ?: "Failed to create user"
                    Toast.makeText(this@ManageUsersActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageUsersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun loadUsers() {
        binding.loadingProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = ManageUsersRequest(adminId = adminId, action = "list")
                val response = RetrofitClient.getApiService(this@ManageUsersActivity).manageUsers(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val users = response.body()?.users ?: emptyList()
                    userAdapter.setUsers(users)
                } else {
                    Toast.makeText(this@ManageUsersActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageUsersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingProgress.visibility = View.GONE
            }
        }
    }

    private fun confirmDeleteUser(user: UserDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.full_name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(targetId: Int) {
        binding.loadingProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val request = ManageUsersRequest(adminId = adminId, action = "delete", targetId = targetId)
                val response = RetrofitClient.getApiService(this@ManageUsersActivity).manageUsers(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ManageUsersActivity, "User deleted", Toast.LENGTH_SHORT).show()
                    loadUsers() // Refresh list
                } else {
                    val msg = response.body()?.message ?: "Failed to delete"
                    Toast.makeText(this@ManageUsersActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageUsersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingProgress.visibility = View.GONE
            }
        }
    }
}
