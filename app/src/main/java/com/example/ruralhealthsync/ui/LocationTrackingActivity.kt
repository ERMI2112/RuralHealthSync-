package com.example.ruralhealthsync.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.RuralHealthApplication
import com.example.ruralhealthsync.data.local.PatientEntity
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.example.ruralhealthsync.data.repository.PatientRepository
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var repository: PatientRepository
    private var googleMap: GoogleMap? = null

    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnRefreshLocations: Button
    private lateinit var btnLocateMap: Button
    private lateinit var mapLoadingProgress: View
    private lateinit var tvMapStatus: TextView

    private var selectedPatient: PatientEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceManager = PreferenceManager(this)
        if (!preferenceManager.isAdmin()) {
            Toast.makeText(this, "This screen is available to administrators only.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_location_tracking_lab)

        supportActionBar?.title = "Recorded Patient Locations"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = PatientRepository(
            (application as RuralHealthApplication).database,
            this
        )

        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvStatus = findViewById(R.id.tvStatus)
        btnRefreshLocations = findViewById(R.id.btnGetLocation)
        btnLocateMap = findViewById(R.id.btnLocateMap)
        mapLoadingProgress = findViewById(R.id.mapLoadingProgress)
        tvMapStatus = findViewById(R.id.tvMapStatus)

        tvStatus.text = "Patient GPS is taken when CHWs save records."

        btnRefreshLocations.setOnClickListener {
            loadPatientsOnMap()
        }

        btnLocateMap.setOnClickListener {
            openSelectedPatientLocation()
        }

        updateOpenMapButton(enabled = false)

        if (!isMapsConfigured()) {
            showMapError("Google Maps is not configured for this build.")
            return
        }

        val playServicesStatus = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this)
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            showMapError(GoogleApiAvailability.getInstance().getErrorString(playServicesStatus))
            return
        }

        try {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit()
            mapFragment.getMapAsync(this)
        } catch (e: Exception) {
            showMapError("Map load failed: ${e.message}")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.setInfoWindowAdapter(PatientInfoWindowAdapter(layoutInflater))
        map.setOnMarkerClickListener { marker ->
            val patient = marker.tag as? PatientEntity
            if (patient != null) {
                selectedPatient = patient
                updateSelectedPatientInfo(patient)
                updateOpenMapButton(enabled = true)
            }
            false
        }
        loadPatientsOnMap()
    }

    private fun loadPatientsOnMap() {
        mapLoadingProgress.isVisible = true
        tvMapStatus.isVisible = false
        btnRefreshLocations.isEnabled = false

        lifecycleScope.launch {
            try {
                val patients = repository.getPatientsWithLocation()
                    .sortedByDescending { it.createdAt }

                mapLoadingProgress.isVisible = false
                btnRefreshLocations.isEnabled = true
                updateSummary(patients)

                if (patients.isEmpty()) {
                    selectedPatient = null
                    updateSelectedPatientInfo(null)
                    updateOpenMapButton(enabled = false)
                    tvMapStatus.isVisible = true
                    tvMapStatus.text = "No patient records with captured GPS are available yet."
                    return@launch
                }

                placeMarkersOnMap(patients)
                selectedPatient = patients.first()
                updateSelectedPatientInfo(selectedPatient)
                updateOpenMapButton(enabled = true)
            } catch (e: Exception) {
                mapLoadingProgress.isVisible = false
                btnRefreshLocations.isEnabled = true
                selectedPatient = null
                updateOpenMapButton(enabled = false)
                tvMapStatus.isVisible = true
                tvMapStatus.text = "Error loading map data: ${e.message}"
            }
        }
    }

    private fun isMapsConfigured(): Boolean {
        return try {
            val key = packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString("com.google.android.geo.API_KEY")
                .orEmpty()
            key.isNotBlank() && !key.contains("YOUR_MAPS_API_KEY")
        } catch (_: Exception) {
            false
        }
    }

    private fun showMapError(message: String) {
        mapLoadingProgress.isVisible = false
        tvMapStatus.isVisible = true
        tvMapStatus.text = message
        btnRefreshLocations.isEnabled = true
        updateOpenMapButton(enabled = false)
    }

    private fun updateSummary(patients: List<PatientEntity>) {
        tvLatitude.text = "GPS Records: ${patients.size}"
        val chwCount = patients.map { it.workerId }.distinct().size
        tvStatus.text = "Patient GPS is taken when CHWs save records. CHWs represented: $chwCount"
    }

    private fun updateSelectedPatientInfo(patient: PatientEntity?) {
        if (patient == null) {
            tvLongitude.text = "Selected record: None"
            return
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        tvLongitude.text = "Selected: ${patient.fullName}"
        tvMapStatus.isVisible = true
        tvMapStatus.text = "Recorded by CHW #${patient.workerId} on ${dateFormat.format(Date(patient.createdAt))} at %.4f, %.4f".format(
            patient.latitude,
            patient.longitude
        )
    }

    private fun updateOpenMapButton(enabled: Boolean) {
        btnLocateMap.isEnabled = enabled
        btnLocateMap.alpha = if (enabled) 1f else 0.5f
    }

    private fun openSelectedPatientLocation() {
        val patient = selectedPatient ?: return
        val lat = patient.latitude
        val lng = patient.longitude
        val label = patient.fullName.ifBlank { "Recorded Patient Location" }
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
        }
    }

    private fun placeMarkersOnMap(patients: List<PatientEntity>) {
        val map = googleMap ?: return
        map.clear()
        val boundsBuilder = LatLngBounds.Builder()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        patients.forEach { patient ->
            val position = LatLng(patient.latitude, patient.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(patient.fullName)
                    .snippet("Recorded by CHW #${patient.workerId} on ${dateFormat.format(Date(patient.createdAt))}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            marker?.tag = patient
            boundsBuilder.include(position)
        }

        try {
            val bounds = boundsBuilder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        } catch (e: Exception) {
            val first = patients.first()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 12f)
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
