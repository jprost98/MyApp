package com.example.myapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.Record
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.RecordViewBinding
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter(
    private val onRecordClick: (Record) -> Unit
) : ListAdapter<Record, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    private var vehicles: List<Vehicle> = emptyList()

    fun updateVehicles(newVehicles: List<Vehicle>) {
        this.vehicles = newVehicles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = RecordViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record, vehicles, onRecordClick)
    }

    class RecordViewHolder(private val binding: RecordViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Record, vehicles: List<Vehicle>, onClick: (Record) -> Unit) {
            binding.recordTitle.text = record.title
            
            val displayDate = record.date?.let {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(it)
                    date?.let { d -> SimpleDateFormat.getDateInstance().format(d) }
                } catch (e: Exception) { it }
            } ?: ""
            binding.recordDate.text = displayDate

            val vehicleTitle = vehicles.find { it.vehicleId.toString() == record.vehicle }?.vehicleTitle() ?: ""
            binding.recordVehicle.text = vehicleTitle

            val odometer = record.odometer?.let {
                it.reversed().chunked(3).joinToString(",").reversed()
            } ?: ""
            binding.recordOdometer.text = odometer

            binding.recordMaterialCard.setOnClickListener {
                onClick(record)
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
            return oldItem.recordId == newItem.recordId
        }

        override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
            return oldItem == newItem
        }
    }
}
