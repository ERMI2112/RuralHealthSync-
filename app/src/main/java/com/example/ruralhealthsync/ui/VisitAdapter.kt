package com.example.ruralhealthsync.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ruralhealthsync.R
import com.example.ruralhealthsync.data.local.VisitEntity
import com.example.ruralhealthsync.databinding.ItemVisitBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VisitAdapter : ListAdapter<VisitEntity, VisitAdapter.VisitViewHolder>(VisitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = ItemVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class VisitViewHolder(private val binding: ItemVisitBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(visit: VisitEntity) {
            binding.visitDateText.text = dateFormat.format(Date(visit.visitDate))
            
            // Sync status
            binding.syncIcon.setImageResource(
                if (visit.isSynced) android.R.drawable.stat_sys_upload_done 
                else android.R.drawable.stat_notify_sync
            )

            // Symptoms
            if (visit.symptoms.isNotBlank()) {
                binding.symptomsText.text = visit.symptoms
                binding.symptomsText.visibility = View.VISIBLE
            } else {
                binding.symptomsText.text = "None reported"
            }

            // Prescription / Notes
            val combinedNotes = buildString {
                if (visit.prescription.isNotBlank()) append("Rx: ${visit.prescription}\n")
                if (visit.notes.isNotBlank()) append(visit.notes)
            }.trim()

            if (combinedNotes.isNotEmpty()) {
                binding.notesText.text = combinedNotes
                binding.notesText.visibility = View.VISIBLE
            } else {
                binding.notesText.text = "No notes recorded"
            }

            // Follow-up
            if (visit.followUpDate > 0L) {
                binding.followUpContainer.visibility = View.VISIBLE
                binding.followUpDateText.text = "Follow-up: ${dateFormat.format(Date(visit.followUpDate))}"
            } else {
                binding.followUpContainer.visibility = View.GONE
            }
        }
    }
}

class VisitDiffCallback : DiffUtil.ItemCallback<VisitEntity>() {
    override fun areItemsTheSame(oldItem: VisitEntity, newItem: VisitEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: VisitEntity, newItem: VisitEntity): Boolean {
        return oldItem == newItem
    }
}
