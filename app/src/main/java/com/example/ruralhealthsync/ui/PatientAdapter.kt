package com.example.ruralhealthsync.ui

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.data.local.PatientEntity
import com.example.ruralhealthsync.databinding.PatientItemBinding

class PatientAdapter(
    private val isAdmin: Boolean,
    private val currentWorkerId: Int,
    private val onClick: (PatientEntity) -> Unit,
    private val onDelete: (PatientEntity) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    private var patients: List<PatientEntity> = emptyList()

    fun setPatients(newList: List<PatientEntity>) {
        patients = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = PatientItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }

    override fun getItemCount(): Int = patients.size

    inner class PatientViewHolder(private val binding: PatientItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(patient: PatientEntity) {
            binding.itemName.text = patient.fullName
            binding.itemDetails.text = "Age: ${patient.age} | ${patient.gender} | ${patient.phone}"
            binding.itemStatus.text = if (patient.isSynced) "Synced" else "Pending Sync"
            binding.syncIcon.isVisible = patient.isSynced

            if (patient.nextVisitDate > 0) {
                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                binding.visitWarningIcon.isVisible = patient.nextVisitDate <= todayStart + 86400000
            } else {
                binding.visitWarningIcon.isVisible = false
            }

            if (patient.photoUrl.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(patient.photoUrl, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (decodedImage != null) {
                        binding.itemPhoto.setImageBitmap(decodedImage)
                    } else {
                        binding.itemPhoto.setImageResource(R.drawable.ic_launcher)
                    }
                } catch (e: Exception) {
                    binding.itemPhoto.setImageResource(R.drawable.ic_launcher)
                }
            } else {
                binding.itemPhoto.setImageResource(R.drawable.ic_launcher)
            }

            // Admin: any patient. CHW: no delete.
            val canDelete = isAdmin
            binding.deletePatientButton.isVisible = canDelete
            binding.deletePatientButton.setOnClickListener { view ->
                view.isClickable = true
                onDelete(patient)
            }

            binding.root.setOnClickListener { onClick(patient) }
        }
    }
}
