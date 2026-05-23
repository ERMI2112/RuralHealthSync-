package com.example.ruralhealthsync.ui

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.RuralHealthApplication
import com.example.ruralhealthsync.data.local.PatientEntity
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.example.ruralhealthsync.data.repository.PatientRepository
import com.example.ruralhealthsync.databinding.ActivityPatientListBinding
import kotlinx.coroutines.launch

class PatientListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientListBinding
    private lateinit var repository: PatientRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var patientAdapter: PatientAdapter
    
    private var workerId: Int = -1
    private var currentPatients: List<PatientEntity> = emptyList()
    private var patientsLiveData: LiveData<List<PatientEntity>>? = null
    private var unsyncedLiveData: LiveData<Int>? = null

    private val exportLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                saveCsvToFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferenceManager = PreferenceManager(this)
        if (!preferenceManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityPatientListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.subtitle = if (preferenceManager.isAdmin()) "Administrator" else "CHW"

        workerId = preferenceManager.getUserId()
        
        repository = PatientRepository(
            (application as RuralHealthApplication).database,
            this
        )
        
        setupRecyclerView()
        setupListeners()
        observeUnsyncedCount()
        observePatients("")
        checkNetworkAndShowBanner()
        if (!preferenceManager.isAdmin()) {
            scheduleSyncWork()
        }
    }

    private fun checkNetworkAndShowBanner() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val isOnline = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: run {
                binding.offlineBanner.visibility = View.VISIBLE
                return
            }
            val caps = cm.getNetworkCapabilities(network)
            caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
        binding.offlineBanner.visibility = if (isOnline) View.GONE else View.VISIBLE
    }

    private fun scheduleSyncWork() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.ruralhealthsync.worker.SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.example.ruralhealthsync.worker.SyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun setupRecyclerView() {
        val isAdmin = preferenceManager.isAdmin()
        patientAdapter = PatientAdapter(
            isAdmin = isAdmin,
            currentWorkerId = workerId,
            onClick = { patient ->
            if (!isAdmin && patient.workerId != workerId) {
                Toast.makeText(this, getString(R.string.edit_patient_denied), Toast.LENGTH_LONG).show()
                return@PatientAdapter
            }

            val intent = Intent(this, AddPatientActivity::class.java).apply {
                putExtra(AddPatientActivity.EXTRA_PATIENT_ID, patient.id)
                putExtra(AddPatientActivity.EXTRA_FULL_NAME, patient.fullName)
                putExtra(AddPatientActivity.EXTRA_AGE, patient.age)
                putExtra(AddPatientActivity.EXTRA_GENDER, patient.gender)
                putExtra(AddPatientActivity.EXTRA_PHONE, patient.phone)
                putExtra(AddPatientActivity.EXTRA_ADDRESS, patient.address)
                putExtra(AddPatientActivity.EXTRA_DIAGNOSIS, patient.diagnosis)
                putExtra(AddPatientActivity.EXTRA_PHOTO_URL, patient.photoUrl)
                putExtra(AddPatientActivity.EXTRA_CREATED_AT, patient.createdAt)
                putExtra(AddPatientActivity.EXTRA_NEXT_VISIT_DATE, patient.nextVisitDate)
                putExtra(AddPatientActivity.EXTRA_LATITUDE, patient.latitude)
                putExtra(AddPatientActivity.EXTRA_LONGITUDE, patient.longitude)
                putExtra(AddPatientActivity.EXTRA_WORKER_ID, patient.workerId)
                putExtra(AddPatientActivity.EXTRA_SERVER_ID, patient.serverId)
                putExtra(AddPatientActivity.EXTRA_IS_SYNCED, patient.isSynced)
            }
            startActivity(intent)
        },
            onDelete = { patient -> confirmDeletePatient(patient) }
        )
        binding.patientRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.patientRecyclerView.adapter = patientAdapter
    }

    private fun confirmDeletePatient(patient: PatientEntity) {
        val isAdmin = preferenceManager.isAdmin()
        val ownsPatient = patient.workerId == workerId || patient.workerId == 0
        if (!isAdmin && !ownsPatient) {
            Toast.makeText(this, R.string.delete_patient_denied, Toast.LENGTH_LONG).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_patient_title)
            .setMessage(getString(R.string.delete_patient_message, patient.fullName))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete_patient_confirm) { _, _ ->
                lifecycleScope.launch {
                    if (repository.deletePatient(patient.id)) {
                        Toast.makeText(this@PatientListActivity, R.string.delete_patient_success, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val isAdmin = preferenceManager.isAdmin()
        menu.findItem(R.id.action_manage_users)?.isVisible = isAdmin
        menu.findItem(R.id.action_export_csv)?.isVisible = isAdmin
        menu.findItem(R.id.action_location_lab)?.isVisible = isAdmin
        menu.findItem(R.id.action_remote_data_volley)?.isVisible = isAdmin
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                preferenceManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.action_manage_users -> {
                startActivity(Intent(this, ManageUsersActivity::class.java))
                true
            }
            R.id.action_export_csv -> {
                exportPatientsToCsv()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_location_lab -> {
                if (!preferenceManager.isAdmin()) {
                    Toast.makeText(this, "This screen is available to administrators only.", Toast.LENGTH_LONG).show()
                    return true
                }
                startActivity(Intent(this, LocationTrackingActivity::class.java))
                true
            }
            R.id.action_remote_data_volley -> {
                startActivity(Intent(this, RemoteDataViewActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupListeners() {
        binding.addPatientButton.setOnClickListener {
            startActivity(Intent(this, AddPatientActivity::class.java))
        }

        if (!preferenceManager.isAdmin()) {
            binding.syncButton.setOnClickListener { performManualSync() }
        } else {
            binding.syncButton.visibility = View.GONE
            (binding.patientRecyclerView.layoutParams as ConstraintLayout.LayoutParams).apply {
                bottomToTop = binding.addPatientButton.id
                binding.patientRecyclerView.layoutParams = this
            }
        }

        binding.searchInput.doAfterTextChanged {
            observePatients(it?.toString().orEmpty())
        }
    }

    private fun observePatients(query: String) {
        val isAdmin = preferenceManager.isAdmin()
        val liveData = if (query.isEmpty()) {
            if (isAdmin) repository.getAllPatients() else repository.getPatientsForWorker(workerId)
        } else {
            if (isAdmin) repository.searchAllPatients(query) else repository.searchPatients(workerId, query)
        }

        patientsLiveData?.removeObservers(this)
        patientsLiveData = liveData

        patientsLiveData?.observe(this) { patients ->
            currentPatients = patients
            patientAdapter.setPatients(patients)
            binding.emptyViewText.visibility = if (patients.isEmpty()) View.VISIBLE else View.GONE
            
            if (isAdmin) {
                binding.adminDashboardCard.visibility = View.VISIBLE
                binding.syncStatusBar.visibility = View.GONE
                binding.totalPatientsText.text = "Total: ${patients.size}"
                
                // Calculate added today
                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val addedToday = patients.count { it.createdAt >= todayStart }
                binding.patientsTodayText.text = "Today: $addedToday"
            } else {
                binding.adminDashboardCard.visibility = View.GONE
                binding.syncStatusBar.visibility = View.VISIBLE
                binding.syncButton.visibility = View.VISIBLE
            }
        }

        updateLastSyncTimeText()
    }

    private fun observeUnsyncedCount() {
        unsyncedLiveData?.removeObservers(this)
        unsyncedLiveData = repository.getUnsyncedCount()
        unsyncedLiveData?.observe(this) { count ->
            binding.pendingSyncText.text = "Pending to sync: $count"
        }
    }
    
    private fun updateLastSyncTimeText() {
        val lastSync = preferenceManager.getLastSyncTime()
        if (lastSync == 0L) {
            binding.lastSyncText.text = "Last sync: Never"
        } else {
            val format = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            binding.lastSyncText.text = "Last sync: ${format.format(java.util.Date(lastSync))}"
        }
    }

    private fun performManualSync() {
        binding.syncProgress.visibility = View.VISIBLE
        binding.syncButton.isEnabled = false

        lifecycleScope.launch {
            val (success, message) = repository.performFullSync(workerId)
            binding.syncProgress.visibility = View.GONE
            binding.syncButton.isEnabled = true
            
            if (success) {
                preferenceManager.saveLastSyncTime(System.currentTimeMillis())
                updateLastSyncTimeText()
                Toast.makeText(this@PatientListActivity, message, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@PatientListActivity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun exportPatientsToCsv() {
        if (currentPatients.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "patients_export_${System.currentTimeMillis()}.csv")
        }
        exportLauncher.launch(intent)
    }

    private fun saveCsvToFile(uri: android.net.Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.bufferedWriter()
                // Header
                writer.write("ID,Full Name,Age,Gender,Phone,Address,Diagnosis,Latitude,Longitude,Created At,Next Visit\n")
                
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                
                currentPatients.forEach { p ->
                    val createdAt = dateFormat.format(java.util.Date(p.createdAt))
                    val nextVisit = if (p.nextVisitDate > 0) dateFormat.format(java.util.Date(p.nextVisitDate)) else "N/A"

                    val exportId = if (p.serverId != 0) p.serverId else p.id
                    val line = "$exportId,\"${p.fullName}\",${p.age},${p.gender},${p.phone},\"${p.address}\",\"${p.diagnosis}\",${p.latitude},${p.longitude},$createdAt,$nextVisit\n"
                    writer.write(line)
                }
                writer.flush()
            }
            Toast.makeText(this, "Export Successful!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
