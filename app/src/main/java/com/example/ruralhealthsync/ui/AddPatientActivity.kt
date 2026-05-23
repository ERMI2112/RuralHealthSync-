package com.example.ruralhealthsync.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.RuralHealthApplication
import com.example.ruralhealthsync.data.local.PatientEntity
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.example.ruralhealthsync.data.repository.PatientRepository
import com.example.ruralhealthsync.databinding.ActivityAddPatientBinding
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class AddPatientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPatientBinding
    private lateinit var repository: PatientRepository
    private lateinit var preferenceManager: PreferenceManager
    
    private var editPatientId: Int = 0
    private var editWorkerId: Int = 0
    private var editServerId: Int = 0
    private var editIsSynced: Boolean = false
    private var editCreatedAt: Long = 0L
    private var currentPhotoBase64: String = ""

    private var nextVisitDate: Long = 0L
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var isSavingPatient = false

    private var singleUpdateListener: LocationListener? = null

    private var currentVoiceInputTarget: com.google.android.material.textfield.TextInputEditText? = null

    private val speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty() && currentVoiceInputTarget != null) {
                val currentText = currentVoiceInputTarget?.text?.toString() ?: ""
                val newText = if (currentText.isEmpty()) spokenText else "$currentText $spokenText"
                currentVoiceInputTarget?.setText(newText)
                currentVoiceInputTarget?.setSelection(currentVoiceInputTarget?.text?.length ?: 0)
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                binding.patientPhoto.setImageBitmap(it)
                currentPhotoBase64 = encodeImage(it)
            }
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            captureLocation()
        } else {
            Toast.makeText(this, "Location permission is required for GPS capture", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = PatientRepository(
            (application as RuralHealthApplication).database,
            this
        )
        preferenceManager = PreferenceManager(this)
        
        // Setup toolbar with back button
        binding.addPatientToolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Setup dropdowns
        setupDropdowns()
        
        // Setup BMI calculation
        setupBMICalculation()
        
        loadEditDataIfPresent()
        
        // Default to Ethiopia (+251) for new patients, but allow user to change it
        if (editPatientId == 0) {
            binding.phoneInput.setText("+251 ")
            // Put the cursor at the end of the text
            binding.phoneInput.setSelection(binding.phoneInput.text?.length ?: 0)
            
            binding.emergencyContactPhoneInput.setText("+251 ")
        }
        
        setupDeleteButtonIfAllowed()

        binding.capturePhotoButton.setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        binding.nextVisitDateInput.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    nextVisitDate = calendar.timeInMillis
                    val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    binding.nextVisitDateInput.setText(format.format(calendar.time))
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.captureGpsButton.setOnClickListener {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                captureLocation()
            } else {
                requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        binding.savePatientButton.setOnClickListener {
            checkDuplicateAndSave()
        }

        binding.deletePatientButton.setOnClickListener {
            confirmDeleteCurrentPatient()
        }

        binding.viewVisitHistoryButton.setOnClickListener {
            val intent = Intent(this, VisitHistoryActivity::class.java).apply {
                putExtra(VisitHistoryActivity.EXTRA_PATIENT_ID, editPatientId)
                putExtra(VisitHistoryActivity.EXTRA_PATIENT_NAME, binding.fullNameInput.text?.toString()?.trim() ?: "")
            }
            startActivity(intent)
        }

        setupVoiceInput()
    }

    private fun setupDropdowns() {
        // Gender dropdown
        val genderAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.gender_options)
        )
        binding.genderInput.setAdapter(genderAdapter)
        
        // Blood type dropdown
        val bloodTypeAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.blood_type_options)
        )
        binding.bloodTypeInput.setAdapter(bloodTypeAdapter)
    }

    private fun setupBMICalculation() {
        // Calculate BMI when weight or height changes
        val bmiWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                calculateAndDisplayBMI()
            }
        }
        
        binding.weightInput.addTextChangedListener(bmiWatcher)
        binding.heightInput.addTextChangedListener(bmiWatcher)
    }

    private fun calculateAndDisplayBMI() {
        val weightStr = binding.weightInput.text?.toString()
        val heightStr = binding.heightInput.text?.toString()
        
        if (weightStr.isNullOrEmpty() || heightStr.isNullOrEmpty()) {
            binding.bmiText.text = "BMI: --"
            return
        }
        
        val weight = weightStr.toFloatOrNull()
        val height = heightStr.toFloatOrNull()
        
        if (weight == null || height == null || weight <= 0 || height <= 0) {
            binding.bmiText.text = "BMI: --"
            return
        }
        
        // BMI = weight (kg) / (height (m))^2
        val heightInMeters = height / 100
        val bmi = weight / (heightInMeters * heightInMeters)
        
        val category = when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
        
        binding.bmiText.text = "BMI: %.1f (%s)".format(bmi, category)
    }

    private fun setupDeleteButtonIfAllowed() {
        if (editPatientId == 0) {
            binding.deletePatientButton.visibility = android.view.View.GONE
            binding.viewVisitHistoryButton.visibility = android.view.View.GONE
            return
        }
        val currentUserId = preferenceManager.getUserId()
        val ownsPatient = editWorkerId == currentUserId || editWorkerId == 0
        val canDelete = preferenceManager.isAdmin() || ownsPatient
        binding.deletePatientButton.visibility = if (canDelete) android.view.View.VISIBLE else android.view.View.GONE
        // Always show Visit History button for any existing patient
        binding.viewVisitHistoryButton.visibility = android.view.View.VISIBLE
    }

    private fun confirmDeleteCurrentPatient() {
        val name = binding.fullNameInput.text?.toString()?.trim().orEmpty().ifEmpty { "this patient" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_patient_title)
            .setMessage(getString(R.string.delete_patient_message, name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete_patient_confirm) { _, _ ->
                lifecycleScope.launch {
                    if (repository.deletePatient(editPatientId)) {
                        Toast.makeText(
                            this@AddPatientActivity,
                            R.string.delete_patient_success,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
            .show()
    }

    override fun onStop() {
        super.onStop()
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            singleUpdateListener?.let { locationManager.removeUpdates(it) }
        } catch (e: SecurityException) {
            // Ignore if permission is missing at stop time.
        }
    }

    private fun setupVoiceInput() {
        if (!preferenceManager.isVoiceInputEnabled()) return

        val fields = listOf(
            binding.fullNameLayout to binding.fullNameInput,
            binding.addressLayout to binding.addressInput,
            binding.diagnosisLayout to binding.diagnosisInput
        )

        for ((layout, input) in fields) {
            layout.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_CUSTOM
            layout.setEndIconDrawable(R.drawable.ic_mic)
            layout.setEndIconOnClickListener {
                currentVoiceInputTarget = input
                val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                }
                try {
                    speechRecognizerLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoLauncher.launch(intent)
    }

    private fun captureLocation() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider == null) {
                Toast.makeText(this, "Location is off. Enable GPS first.", Toast.LENGTH_LONG).show()
                return
            }

            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                applyLocation(location)
                return
            }

            singleUpdateListener?.let { locationManager.removeUpdates(it) }
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    applyLocation(loc)
                    locationManager.removeUpdates(this)
                }
            }
            singleUpdateListener = listener
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            Toast.makeText(this, "Searching for GPS...", Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLocation(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        binding.gpsStatusText.text = "GPS: %.4f, %.4f".format(latitude, longitude)
        Toast.makeText(this, "Location Captured!", Toast.LENGTH_SHORT).show()
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun applyPhotoFromBase64(base64: String) {
        try {
            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (decodedImage != null) {
                binding.patientPhoto.setImageBitmap(decodedImage)
            }
        } catch (e: Exception) {
            // Keep default image if decode fails.
        }
    }

    private fun loadEditDataIfPresent() {
        editPatientId = intent.getIntExtra(EXTRA_PATIENT_ID, 0)
        if (editPatientId == 0) return

        editWorkerId = intent.getIntExtra(EXTRA_WORKER_ID, preferenceManager.getUserId())
        editServerId = intent.getIntExtra(EXTRA_SERVER_ID, 0)
        editIsSynced = intent.getBooleanExtra(EXTRA_IS_SYNCED, false)
        editCreatedAt = intent.getLongExtra(EXTRA_CREATED_AT, System.currentTimeMillis())
        nextVisitDate = intent.getLongExtra(EXTRA_NEXT_VISIT_DATE, 0L)
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

        binding.addPatientTitleText.text = "Edit Patient"
        binding.addPatientSubtitleText.text = "Update patient information"
        binding.savePatientButton.text = "Update Patient"

        binding.fullNameInput.setText(intent.getStringExtra(EXTRA_FULL_NAME).orEmpty())
        binding.ageInput.setText(intent.getIntExtra(EXTRA_AGE, 0).takeIf { it > 0 }?.toString().orEmpty())
        binding.genderInput.setText(intent.getStringExtra(EXTRA_GENDER).orEmpty())
        binding.phoneInput.setText(intent.getStringExtra(EXTRA_PHONE).orEmpty())
        binding.addressInput.setText(intent.getStringExtra(EXTRA_ADDRESS).orEmpty())
        binding.diagnosisInput.setText(intent.getStringExtra(EXTRA_DIAGNOSIS).orEmpty())

        currentPhotoBase64 = intent.getStringExtra(EXTRA_PHOTO_URL).orEmpty()
        if (currentPhotoBase64.isNotEmpty()) {
            applyPhotoFromBase64(currentPhotoBase64)
        }
        
        if (nextVisitDate > 0L) {
            val format = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            binding.nextVisitDateInput.setText(format.format(java.util.Date(nextVisitDate)))
        }

        if (latitude != 0.0 || longitude != 0.0) {
            binding.gpsStatusText.text = "GPS: %.4f, %.4f".format(latitude, longitude)
        }
    }

    private fun checkDuplicateAndSave() {
        if (isSavingPatient) return

        // Clear previous errors
        binding.fullNameLayout.error = null
        binding.ageLayout.error = null
        binding.genderLayout.error = null
        binding.phoneLayout.error = null

        val fullName = binding.fullNameInput.text?.toString()?.trim().orEmpty()
        val ageText = binding.ageInput.text?.toString()?.trim().orEmpty()
        val age = ageText.toIntOrNull()
        val gender = binding.genderInput.text?.toString()?.trim().orEmpty()
        val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()

        // Validation: Required fields
        var hasError = false

        if (fullName.isEmpty()) {
            binding.fullNameLayout.error = "Name is required"
            hasError = true
        }

        if (ageText.isEmpty()) {
            binding.ageLayout.error = "Age is required"
            hasError = true
        } else if (age == null || age <= 0 || age > 120) {
            binding.ageLayout.error = "Age must be between 1 and 120"
            hasError = true
        }

        if (gender.isEmpty()) {
            binding.genderLayout.error = "Please select gender"
            hasError = true
        }

        if (phone.isEmpty()) {
            binding.phoneLayout.error = "Phone number is required"
            hasError = true
        } else if (phone.length < 10) {
            binding.phoneLayout.error = "Enter valid phone number (min 10 digits)"
            hasError = true
        }

        if (hasError) {
            Toast.makeText(this, "Please fix the errors above", Toast.LENGTH_SHORT).show()
            return
        }

        setSavingState(true)
        lifecycleScope.launch {
            try {
                val duplicate = repository.findDuplicatePatient(
                    name = fullName,
                    age = age!!,
                    gender = gender,
                    phone = phone,
                    excludePatientId = editPatientId
                )
                if (duplicate != null) {
                    setSavingState(false)
                    showDuplicateDialog(
                        fullName = duplicate.patient.fullName,
                        phone = duplicate.patient.phone,
                        matchedByPhone = duplicate.matchedByPhone
                    )
                    return@launch
                }

                persistPatient(
                    fullName = fullName,
                    age = age,
                    gender = gender,
                    phone = phone,
                    address = binding.addressInput.text?.toString()?.trim().orEmpty(),
                    diagnosis = binding.diagnosisInput.text?.toString()?.trim().orEmpty()
                )
                Toast.makeText(this@AddPatientActivity, "Patient saved offline!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                setSavingState(false)
                Toast.makeText(
                    this@AddPatientActivity,
                    "Unable to save patient: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showDuplicateDialog(fullName: String, phone: String, matchedByPhone: Boolean) {
        val messageRes = if (matchedByPhone && phone.isNotBlank()) {
            R.string.duplicate_patient_message
        } else {
            R.string.duplicate_patient_message_no_phone
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.duplicate_patient_title)
            .setMessage(
                if (messageRes == R.string.duplicate_patient_message) {
                    getString(messageRes, fullName, phone)
                } else {
                    getString(messageRes, fullName)
                }
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private suspend fun persistPatient(
        fullName: String,
        age: Int,
        gender: String,
        phone: String,
        address: String,
        diagnosis: String
    ) {
        val currentUserId = preferenceManager.getUserId()
        val existing = if (editPatientId > 0) repository.getPatientById(editPatientId) else null
        val createdAt = if (editPatientId == 0) System.currentTimeMillis() else editCreatedAt
        val workerId = when {
            editPatientId == 0 -> currentUserId
            existing != null && existing.workerId > 0 -> existing.workerId
            editWorkerId > 0 -> editWorkerId
            else -> currentUserId
        }
        val serverId = existing?.serverId ?: editServerId
        val isSynced = existing?.isSynced ?: editIsSynced

        // Get new health fields
        val bloodType = binding.bloodTypeInput.text?.toString()?.trim().orEmpty()
        val allergies = binding.allergiesInput.text?.toString()?.trim().orEmpty()
        val emergencyContactName = binding.emergencyContactNameInput.text?.toString()?.trim().orEmpty()
        val emergencyContactPhone = binding.emergencyContactPhoneInput.text?.toString()?.trim().orEmpty()
        val currentMedications = binding.medicationsInput.text?.toString()?.trim().orEmpty()

        // Get vital signs
        val bpSystolic = binding.bpSystolicInput.text?.toString()?.toIntOrNull() ?: 0
        val bpDiastolic = binding.bpDiastolicInput.text?.toString()?.toIntOrNull() ?: 0
        val temperature = binding.temperatureInput.text?.toString()?.toFloatOrNull() ?: 0f
        val weight = binding.weightInput.text?.toString()?.toFloatOrNull() ?: 0f
        val height = binding.heightInput.text?.toString()?.toFloatOrNull() ?: 0f

        // Get chronic conditions
        val conditions = mutableListOf<String>()
        if (binding.checkDiabetes.isChecked) conditions.add("Diabetes")
        if (binding.checkHypertension.isChecked) conditions.add("Hypertension")
        if (binding.checkAsthma.isChecked) conditions.add("Asthma")
        if (binding.checkHIV.isChecked) conditions.add("HIV/AIDS")
        if (binding.checkTB.isChecked) conditions.add("Tuberculosis")
        if (binding.checkHeartDisease.isChecked) conditions.add("Heart Disease")
        if (binding.checkKidneyDisease.isChecked) conditions.add("Kidney Disease")
        val chronicConditions = conditions.joinToString(", ")

        repository.insertPatient(
            PatientEntity(
                id = editPatientId,
                serverId = serverId,
                workerId = workerId,
                fullName = fullName,
                age = age,
                gender = gender,
                phone = phone,
                address = address,
                diagnosis = diagnosis,
                photoUrl = currentPhotoBase64,
                createdAt = createdAt,
                nextVisitDate = nextVisitDate,
                latitude = latitude,
                longitude = longitude,
                isSynced = if (editPatientId == 0) false else isSynced,
                bloodType = bloodType,
                allergies = allergies,
                emergencyContactName = emergencyContactName,
                emergencyContactPhone = emergencyContactPhone,
                currentMedications = currentMedications,
                bloodPressureSystolic = bpSystolic,
                bloodPressureDiastolic = bpDiastolic,
                temperature = temperature,
                weight = weight,
                height = height,
                chronicConditions = chronicConditions
            )
        )
    }

    private fun setSavingState(isSaving: Boolean) {
        isSavingPatient = isSaving
        binding.savePatientButton.isEnabled = !isSaving
    }

    companion object {
        const val EXTRA_PATIENT_ID = "extra_patient_id"
        const val EXTRA_FULL_NAME = "extra_full_name"
        const val EXTRA_AGE = "extra_age"
        const val EXTRA_GENDER = "extra_gender"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_DIAGNOSIS = "extra_diagnosis"
        const val EXTRA_PHOTO_URL = "extra_photo_url"
        const val EXTRA_CREATED_AT = "extra_created_at"
        const val EXTRA_NEXT_VISIT_DATE = "extra_next_visit_date"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_WORKER_ID = "extra_worker_id"
        const val EXTRA_SERVER_ID = "extra_server_id"
        const val EXTRA_IS_SYNCED = "extra_is_synced"
    }
}
