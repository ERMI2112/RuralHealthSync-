package com.example.ruralhealthsync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.VolleyError
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.data.local.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.nio.charset.Charset

class RemoteDataViewActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnFetchVolley: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvServerUrl: TextView
    private lateinit var tvRecordCount: TextView
    private lateinit var resultsContainer: LinearLayout
    private lateinit var patientsContainer: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var errorCard: MaterialCardView
    private lateinit var tvError: TextView
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_data_view)

        preferenceManager = PreferenceManager(this)
        
        initViews()
        setupToolbar()
        setupListeners()
        showEmptyState()
        
        // Display server URL
        tvServerUrl.text = "Server: ${preferenceManager.getServerUrlDisplay()}"
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnFetchVolley = findViewById(R.id.btnFetchVolley)
        progressBar = findViewById(R.id.progressBar)
        tvServerUrl = findViewById(R.id.tvServerUrl)
        tvRecordCount = findViewById(R.id.tvRecordCount)
        resultsContainer = findViewById(R.id.resultsContainer)
        patientsContainer = findViewById(R.id.patientsContainer)
        emptyState = findViewById(R.id.emptyState)
        errorCard = findViewById(R.id.errorCard)
        tvError = findViewById(R.id.tvError)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        btnFetchVolley.setOnClickListener {
            fetchRecordsWithVolley()
        }
        
        swipeRefresh.setOnRefreshListener {
            fetchRecordsWithVolley()
        }
    }

    private fun fetchRecordsWithVolley() {
        val baseUrl = preferenceManager.getServerUrl()
        val url = "${baseUrl}get_all_patients.php"

        showLoading()
        
        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val success = response.getBoolean("success")
                    if (success) {
                        val patientsArray = response.getJSONArray("patients")
                        
                        if (patientsArray.length() == 0) {
                            showEmptyState()
                            tvRecordCount.text = "0 records on server"
                        } else {
                            displayPatients(patientsArray)
                            tvRecordCount.text = "${patientsArray.length()} records on server"
                        }
                    } else {
                        showError("Server returned success: false")
                    }
                } catch (e: Exception) {
                    showError("JSON Parse Error: ${e.message}")
                } finally {
                    hideLoading()
                }
            },
            { error ->
                val errMsg = buildVolleyErrorMessage(error)
                showError(errMsg)
                hideLoading()
            }
        )

        queue.add(jsonObjectRequest)
    }

    private fun displayPatients(patientsArray: org.json.JSONArray) {
        // Clear previous results
        patientsContainer.removeAllViews()
        
        // Hide other states
        emptyState.visibility = View.GONE
        errorCard.visibility = View.GONE
        resultsContainer.visibility = View.VISIBLE
        
        // Add patient cards
        for (i in 0 until patientsArray.length()) {
            val patient = patientsArray.getJSONObject(i)
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_remote_patient, patientsContainer, false)
            
            // Populate card
            val tvName = cardView.findViewById<TextView>(R.id.tvPatientName)
            val tvId = cardView.findViewById<TextView>(R.id.tvPatientId)
            val tvAge = cardView.findViewById<TextView>(R.id.tvPatientAge)
            val tvGender = cardView.findViewById<TextView>(R.id.tvPatientGender)
            val tvPhone = cardView.findViewById<TextView>(R.id.tvPatientPhone)
            val tvWorkerId = cardView.findViewById<TextView>(R.id.tvWorkerId)
            val tvDiagnosis = cardView.findViewById<TextView>(R.id.tvDiagnosis)
            val diagnosisContainer = cardView.findViewById<LinearLayout>(R.id.diagnosisContainer)
            val ivPatientPhoto = cardView.findViewById<android.widget.ImageView>(R.id.ivPatientPhoto)
            
            tvName.text = patient.optString("name", "Unknown")
            tvId.text = "ID: ${patient.optInt("id", 0)}"
            tvAge.text = "${patient.optInt("age", 0)} years"
            tvGender.text = patient.optString("gender", "N/A")
            tvPhone.text = patient.optString("phone", "N/A")
            tvWorkerId.text = "CHW-${patient.optInt("worker_id", 0)}"
            
            val diagnosis = patient.optString("diagnosis", "")
            if (diagnosis.isNotEmpty()) {
                tvDiagnosis.text = diagnosis
                diagnosisContainer.visibility = View.VISIBLE
            } else {
                diagnosisContainer.visibility = View.GONE
            }
            
            val photoStr = patient.optString("photoUrl", patient.optString("photo_url", ""))
            if (photoStr.isNotEmpty()) {
                try {
                    val imageBytes = android.util.Base64.decode(photoStr, android.util.Base64.DEFAULT)
                    val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivPatientPhoto.setImageBitmap(decodedImage)
                } catch (e: Exception) {
                    ivPatientPhoto.setImageResource(R.drawable.ic_launcher)
                }
            } else {
                ivPatientPhoto.setImageResource(R.drawable.ic_launcher)
            }
            
            patientsContainer.addView(cardView)
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnFetchVolley.isEnabled = false
        btnFetchVolley.text = "Loading..."
        errorCard.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnFetchVolley.isEnabled = true
        btnFetchVolley.text = "Fetch Server Data"
        swipeRefresh.isRefreshing = false
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        resultsContainer.visibility = View.GONE
        errorCard.visibility = View.GONE
        tvRecordCount.text = "No data loaded yet"
    }

    private fun showError(message: String) {
        errorCard.visibility = View.VISIBLE
        tvError.text = message
        resultsContainer.visibility = View.GONE
        emptyState.visibility = View.GONE
        tvRecordCount.text = "Error loading data"
        Toast.makeText(this, "Error: $message", Toast.LENGTH_SHORT).show()
    }

    private fun buildVolleyErrorMessage(error: VolleyError): String {
        val response = error.networkResponse
        if (response != null) {
            val body = response.data?.toString(Charset.defaultCharset()).orEmpty().trim()
            return if (body.isNotEmpty()) {
                "HTTP ${response.statusCode}: ${body.take(160)}"
            } else {
                "HTTP ${response.statusCode}"
            }
        }

        val host = preferenceManager.getServerUrlDisplay()
        return error.localizedMessage ?: "Cannot reach server at $host"
    }
}
