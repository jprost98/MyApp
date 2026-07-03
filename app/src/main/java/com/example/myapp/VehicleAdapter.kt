package com.example.myapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.VehicleViewBinding

class VehicleAdapter(
    private val onVehicleClick: (Vehicle) -> Unit
) : ListAdapter<Vehicle, VehicleAdapter.VehicleViewHolder>(VehicleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = VehicleViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = getItem(position)
        holder.bind(vehicle, onVehicleClick)
    }

    class VehicleViewHolder(private val binding: VehicleViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vehicle: Vehicle, onClick: (Vehicle) -> Unit) {
            binding.vehicleYear.text = vehicle.year
            binding.vehicleMake.text = vehicle.make
            binding.vehicleModel.text = vehicle.model
            binding.vehicleSubmodel.text = vehicle.submodel

            if (!vehicle.engine.isNullOrBlank()) {
                binding.vehicleEngine.visibility = android.view.View.VISIBLE
                binding.vehicleEngine.text = vehicle.engine
            } else {
                binding.vehicleEngine.visibility = android.view.View.GONE
            }

            if (!vehicle.notes.isNullOrBlank()) {
                binding.vehicleNotesLayout.visibility = android.view.View.VISIBLE
                binding.vehicleNotes.text = vehicle.notes
            } else {
                binding.vehicleNotesLayout.visibility = android.view.View.GONE
            }

            binding.vehicleMaterialCard.setOnClickListener {
                onClick(vehicle)
            }
        }
    }

    class VehicleDiffCallback : DiffUtil.ItemCallback<Vehicle>() {
        override fun areItemsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean {
            return oldItem.vehicleId == newItem.vehicleId
        }

        override fun areContentsTheSame(oldItem: Vehicle, newItem: Vehicle): Boolean {
            return oldItem == newItem
        }
    }
}
