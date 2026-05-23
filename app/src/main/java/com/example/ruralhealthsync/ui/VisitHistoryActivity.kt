package com.example.ruralhealthsync.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ruralhealthsync.RuralHealthApplication
import com.example.ruralhealthsync.data.local.VisitDao
import com.example.ruralhealthsync.data.local.VisitEntity
import com.example.ruralhealthsync.databinding.ActivityVisitHistoryBinding
import com.example.ruralhealthsync.databinding.DialogAddVisitBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class VisitHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisitHistoryBinding
    private lateinit var visitAdapter: VisitAdapter
    private lateinit var visitDao: VisitDao

    private var patientId: Int = -1
    private var patientName: String = ""

    companion object {
        const val EXTRA_PATIENT_ID = "extra_patient_id"
        const val EXTRA_PATIENT_NAME = "extra_patient_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisitHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        patientId = intent.getIntExtra(EXTRA_PATIENT_ID, -1)
        patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Unknown Patient"

        if (patientId == -1) {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.patientNameHeader.text = "Patient: $patientName"

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // VisitDao is now fully integrated in AppDatabase!
        visitDao = (application as RuralHealthApplication).database.visitDao()

        setupRecyclerView()
        
        binding.addVisitFab.setOnClickListener {
            showAddVisitDialog()
        }

        loadVisits()
    }

    private fun setupRecyclerView() {
        visitAdapter = VisitAdapter()
        binding.visitRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@VisitHistoryActivity)
            adapter = visitAdapter
        }
    }

    private fun loadVisits() {
        visitDao.getVisitsForPatient(patientId).observe(this) { visits ->
            visitAdapter.submitList(visits)
            binding.emptyView.visibility = if (visits.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddVisitDialog() {
        val dialogBinding = DialogAddVisitBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSave.setOnClickListener {
            val symptoms = dialogBinding.symptomsInput.text.toString().trim()
            val rx = dialogBinding.prescriptionInput.text.toString().trim()
            val notes = dialogBinding.notesInput.text.toString().trim()

            if (symptoms.isEmpty() && rx.isEmpty() && notes.isEmpty()) {
                Toast.makeText(this, "Please enter at least some info", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var followUpDate = 0L
            val cal = Calendar.getInstance()
            when (dialogBinding.followUpRadioGroup.checkedRadioButtonId) {
                dialogBinding.radio1Week.id -> {
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                    followUpDate = cal.timeInMillis
                }
                dialogBinding.radio1Month.id -> {
                    cal.add(Calendar.MONTH, 1)
                    followUpDate = cal.timeInMillis
                }
            }

            val newVisit = VisitEntity(
                patientId = patientId,
                symptoms = symptoms,
                prescription = rx,
                notes = notes,
                followUpDate = followUpDate
            )

            lifecycleScope.launch {
                visitDao.insertVisit(newVisit)
                Toast.makeText(this@VisitHistoryActivity, "Visit recorded", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
