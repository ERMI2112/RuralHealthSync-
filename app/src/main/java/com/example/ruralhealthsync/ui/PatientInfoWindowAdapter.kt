package com.example.ruralhealthsync.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.data.local.PatientEntity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom InfoWindow that shows rich patient details when a map pin is tapped.
 */
class PatientInfoWindowAdapter(private val inflater: LayoutInflater) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? = null // Use default window frame

    override fun getInfoContents(marker: Marker): View {
        val view = inflater.inflate(R.layout.marker_info_window, null)

        val patient = marker.tag as? PatientEntity

        val nameText = view.findViewById<TextView>(R.id.infoName)
        val chwText = view.findViewById<TextView>(R.id.infoChw)
        val diagnosisText = view.findViewById<TextView>(R.id.infoDiagnosis)
        val addressText = view.findViewById<TextView>(R.id.infoAddress)
        val dateText = view.findViewById<TextView>(R.id.infoDate)
        val coordsText = view.findViewById<TextView>(R.id.infoCoords)

        if (patient != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            nameText.text = patient.fullName
            chwText.text = "CHW Worker #${patient.workerId}"
            diagnosisText.text = if (patient.diagnosis.isBlank()) "No diagnosis recorded" else patient.diagnosis
            addressText.text = if (patient.address.isBlank()) "No address" else patient.address
            addressText.visibility = View.VISIBLE
            dateText.text = "Registered: ${dateFormat.format(Date(patient.createdAt))}"
            coordsText.text = "GPS: %.4f, %.4f".format(patient.latitude, patient.longitude)
        } else {
            nameText.text = marker.title
            chwText.text = ""
            diagnosisText.text = marker.snippet ?: ""
            addressText.visibility = View.GONE
            dateText.text = ""
            coordsText.text = ""
        }

        return view
    }
}
