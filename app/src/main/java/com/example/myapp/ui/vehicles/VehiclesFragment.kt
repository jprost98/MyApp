package com.example.myapp.ui.vehicles

import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.VehicleAdapter
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.FragmentVehiclesBinding
import com.example.myapp.databinding.PopupEditVehicleBinding
import com.example.myapp.databinding.VehicleDetailedViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

class VehiclesFragment : Fragment() {

    private var _binding: FragmentVehiclesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VehiclesViewModel
    private lateinit var vehicleAdapter: VehicleAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehiclesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[VehiclesViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun setupRecyclerView() {
        vehicleAdapter = VehicleAdapter { vehicle ->
            showDetailedVehicle(vehicle)
        }

        binding.vehiclesRecyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = vehicleAdapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val vehicle = vehicleAdapter.currentList[position]

                if (direction == ItemTouchHelper.START) { // Delete
                    showDeleteConfirmation(vehicle, position)
                } else { // Edit
                    showEditDialog(vehicle)
                    vehicleAdapter.notifyItemChanged(position)
                }
            }
            
            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
                val itemView = vh.itemView
                
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val iconColor = ContextCompat.getColor(requireContext(), R.color.icon_swipe_color)
                    
                    if (dX > 0) { // Swipe Right (Edit)
                        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.edit_document_24dp_e3e3e3_fill1_wght400_grad0_opsz24)
                        icon?.let {
                            it.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                            val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                            val iconTop = itemView.top + iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    } else if (dX < 0) { // Swipe Left (Delete)
                        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_forever_24dp_e3e3e3_fill1_wght400_grad0_opsz24)
                        icon?.let {
                            it.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                            val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                            val iconTop = itemView.top + iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight
                            val iconRight = itemView.right - iconMargin
                            val iconLeft = iconRight - it.intrinsicWidth
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    }
                    
                    val alpha = 1.0f - Math.abs(dX) / rv.width.toFloat()
                    itemView.alpha = alpha
                    itemView.translationX = dX
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.vehiclesRecyclerview)
    }

    private fun observeViewModel() {
        viewModel.vehicles.observe(viewLifecycleOwner) { vehicles ->
            vehicleAdapter.submitList(vehicles)
            binding.vehiclesLoading.visibility = View.GONE
            binding.vehiclesRecyclerview.visibility = if (vehicles.isEmpty()) View.GONE else View.VISIBLE
            binding.noVehiclesLayout.visibility = if (vehicles.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showDeleteConfirmation(vehicle: Vehicle, position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete this vehicle? All related records and tasks will be lost.")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteVehicle(vehicle)
                Snackbar.make(binding.root, "Vehicle Deleted", Snackbar.LENGTH_LONG).show()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> vehicleAdapter.notifyItemChanged(position) }
            .show()
    }

    private fun showEditDialog(vehicle: Vehicle) {
        val dialogBinding = PopupEditVehicleBinding.inflate(layoutInflater)
        
        dialogBinding.apply {
            (editVehicleYearInput as? TextInputLayout)?.editText?.setText(vehicle.year)
            (editVehicleMakeInput as? TextInputLayout)?.editText?.setText(vehicle.make)
            (editVehicleModelInput as? TextInputLayout)?.editText?.setText(vehicle.model)
            (editVehicleSubmodelInput as? TextInputLayout)?.editText?.setText(vehicle.submodel)
            (editVehicleEngineInput as? TextInputLayout)?.editText?.setText(vehicle.engine)
            (editVehicleNotesInput as? TextInputLayout)?.editText?.setText(vehicle.notes)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.update_vehicle)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val updatedVehicle = vehicle.copy(
                    year = (dialogBinding.editVehicleYearInput as? TextInputLayout)?.editText?.text.toString(),
                    make = (dialogBinding.editVehicleMakeInput as? TextInputLayout)?.editText?.text.toString(),
                    model = (dialogBinding.editVehicleModelInput as? TextInputLayout)?.editText?.text.toString(),
                    submodel = (dialogBinding.editVehicleSubmodelInput as? TextInputLayout)?.editText?.text.toString(),
                    engine = (dialogBinding.editVehicleEngineInput as? TextInputLayout)?.editText?.text.toString(),
                    notes = (dialogBinding.editVehicleNotesInput as? TextInputLayout)?.editText?.text.toString()
                )
                viewModel.updateVehicle(updatedVehicle)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDetailedVehicle(vehicle: Vehicle) {
        val detailBinding = VehicleDetailedViewBinding.inflate(layoutInflater)
        
        detailBinding.apply {
            vehicleDetailedYear.text = vehicle.year
            vehicleDetailedMake.text = vehicle.make
            vehicleDetailedModel.text = vehicle.model
            vehicleDetailedSubmodel.text = vehicle.submodel ?: "---"
            vehicleDetailedEngine.text = vehicle.engine ?: "---"
            vehicleDetailedNotes.text = vehicle.notes.takeIf { !it.isNullOrBlank() } ?: "---"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(detailBinding.root)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.vehicles_menu, menu)
        val searchItem = menu.findItem(R.id.app_bar_search)
        val searchView = searchItem.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sort_records -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val options = arrayOf("Year (Newest First)", "Year (Oldest First)", "Make (A-Z)", "Make (Z-A)")
        val values = arrayOf("year_desc", "year_asc", "make_asc", "make_desc")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Vehicles")
            .setItems(options) { _, which ->
                viewModel.setSort(values[which])
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
