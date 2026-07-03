package com.example.myapp.ui.home

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapp.R
import com.example.myapp.RecordAdapter
import com.example.myapp.FileChooser
import com.example.myapp.data.Record
import com.example.myapp.databinding.FragmentHomeBinding
import com.example.myapp.databinding.PopupEditRecordBinding
import com.example.myapp.databinding.RecordDetailedViewBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var recordAdapter: RecordAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        setupRecyclerView()
        observeViewModel()
        
        setHasOptionsMenu(true)
        return binding.root
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter { record ->
            showDetailedRecord(record)
        }
        
        binding.recordsRecyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recordAdapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = recordAdapter.currentList[position]
                
                if (direction == ItemTouchHelper.START) { // Delete
                    showDeleteConfirmation(record, position)
                } else { // Edit
                    showEditDialog(record)
                    recordAdapter.notifyItemChanged(position)
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
        itemTouchHelper.attachToRecyclerView(binding.recordsRecyclerview)
    }

    private fun observeViewModel() {
        viewModel.records.observe(viewLifecycleOwner) { records ->
            recordAdapter.submitList(records)
            binding.noRecordsLayout.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.vehicles.observe(viewLifecycleOwner) { vehicles ->
            recordAdapter.updateVehicles(vehicles)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.recordsLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.recordsRecyclerview.visibility = if (isLoading) View.GONE else View.VISIBLE
        }
    }

    private fun showDeleteConfirmation(record: Record, position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this record?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRecord(record)
                Snackbar.make(binding.root, "Record Deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { viewModel.undoDelete(record, position) }
                    .show()
            }
            .setNegativeButton("Cancel") { _, _ -> recordAdapter.notifyItemChanged(position) }
            .show()
    }

    private fun showEditDialog(record: Record) {
        val dialogBinding = PopupEditRecordBinding.inflate(layoutInflater)
        val vehicles = viewModel.vehicles.value ?: emptyList()
        var selectedVehicleId = record.vehicle
        var selectedDateString = record.date ?: ""

        dialogBinding.apply {
            editRecordTitleInput.editText?.setText(record.title)
            editRecordOdometerInput.editText?.setText(record.odometer)
            editRecordDescriptionInput.editText?.setText(record.description)
            
            val displayFormat = SimpleDateFormat.getDateInstance()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val date = isoFormat.parse(selectedDateString)
                (editRecordDateInput as? TextInputLayout)?.editText?.setText(displayFormat.format(date!!))
            } catch (e: Exception) {
                (editRecordDateInput as? TextInputLayout)?.editText?.setText(selectedDateString)
            }

            (editRecordDateInput as? TextInputLayout)?.editText?.setOnClickListener {
                val picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .build()
                picker.addOnPositiveButtonClickListener { selection ->
                    val date = Date(selection)
                    selectedDateString = isoFormat.format(date)
                    (editRecordDateInput as? TextInputLayout)?.editText?.setText(displayFormat.format(date))
                }
                picker.show(childFragmentManager, "DATE_PICKER")
            }

            val vehicleNames = vehicles.map { it.vehicleTitle() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleNames)
            ((editRecordVehiclePicker as? TextInputLayout)?.editText as? AutoCompleteTextView)?.apply {
                setAdapter(adapter)
                setText(vehicles.find { it.vehicleId.toString() == selectedVehicleId }?.vehicleTitle() ?: "", false)
                setOnItemClickListener { _, _, position, _ ->
                    selectedVehicleId = vehicles[position].vehicleId.toString()
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val updatedRecord = record.copy(
                    title = (dialogBinding.editRecordTitleInput as? TextInputLayout)?.editText?.text.toString(),
                    date = selectedDateString,
                    vehicle = selectedVehicleId,
                    odometer = (dialogBinding.editRecordOdometerInput as? TextInputLayout)?.editText?.text.toString(),
                    description = (dialogBinding.editRecordDescriptionInput as? TextInputLayout)?.editText?.text.toString()
                )
                viewModel.updateRecord(updatedRecord)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDetailedRecord(record: Record) {
        val detailBinding = RecordDetailedViewBinding.inflate(layoutInflater)
        val vehicles = viewModel.vehicles.value ?: emptyList()
        
        detailBinding.apply {
            recordDetailTitle.text = record.title
            
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val date = isoFormat.parse(record.date!!)
                recordDetailDate.text = SimpleDateFormat.getDateInstance().format(date!!)
            } catch (e: Exception) {
                recordDetailDate.text = record.date
            }
            
            recordDetailVehicle.text = vehicles.find { it.vehicleId.toString() == record.vehicle }?.vehicleTitle() ?: "Unknown Vehicle"
            recordDetailOdometer.text = getString(R.string.odometer_miles, record.odometer ?: "0")
            recordDetailNotes.text = record.description.takeIf { !it.isNullOrBlank() } ?: "---"

            addPhotoBtn.setOnClickListener {
                val intent = Intent(requireContext(), FileChooser::class.java).apply {
                    putExtra("record", record)
                    putExtra("uploadType", "photo")
                }
                startActivity(intent)
            }

            addRecordDocBtn.setOnClickListener {
                val intent = Intent(requireContext(), FileChooser::class.java).apply {
                    putExtra("record", record)
                    putExtra("uploadType", "document")
                }
                startActivity(intent)
            }

            fetchMedia(record, "images", recordPhotosLayout, recordDetailPhotoLoading)
            fetchMedia(record, "documents", recordDetailDocsLayout, recordDetailDocumentLoading)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(detailBinding.root)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun fetchMedia(record: Record, type: String, layout: LinearLayout, progressBar: ProgressBar) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val storageRef = FirebaseStorage.getInstance().getReference("users/${user.uid}/$type/records/record_${record.recordId}")
        
        progressBar.visibility = View.VISIBLE
        
        storageRef.listAll().addOnSuccessListener { listResult ->
            progressBar.visibility = View.GONE
            listResult.items.forEach { item ->
                addMediaToLayout(item, type, layout)
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
        }
    }

    private fun addMediaToLayout(item: StorageReference, type: String, layout: LinearLayout) {
        val inflater = LayoutInflater.from(requireContext())
        if (type == "images") {
            val itemView = inflater.inflate(R.layout.item_detail_image, layout, false)
            val photoBtn = itemView.findViewById<MaterialButton>(R.id.detail_photo_btn)
            photoBtn.text = item.name

            photoBtn.setOnClickListener {
                item.downloadUrl.addOnSuccessListener { uri ->
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }.addOnFailureListener {
                    Snackbar.make(binding.root, "Error opening photo", Snackbar.LENGTH_SHORT).show()
                }
            }

            layout.addView(itemView)
        } else {
            val itemView = inflater.inflate(R.layout.item_detail_doc, layout, false)
            val docBtn = itemView.findViewById<MaterialButton>(R.id.detail_doc_btn)
            docBtn.text = item.name

            docBtn.setOnClickListener {
                item.downloadUrl.addOnSuccessListener { uri ->
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                }.addOnFailureListener {
                    Snackbar.make(binding.root, "Error opening file", Snackbar.LENGTH_SHORT).show()
                }
            }

            layout.addView(itemView)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.records_menu, menu)
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
            R.id.filter_records -> {
                showFilterDialog()
                true
            }
            R.id.sort_records -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val vehicles = viewModel.vehicles.value ?: return
        val options = mutableListOf("All")
        options.addAll(vehicles.map { it.vehicleTitle() })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Vehicle")
            .setItems(options.toTypedArray()) { _, which ->
                val selection = if (which == 0) "All" else vehicles[which - 1].vehicleId.toString()
                viewModel.setFilter(selection)
            }
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Date (Newest First)", "Date (Oldest First)", "Mileage (Highest First)", "Mileage (Lowest First)", "Title (A-Z)", "Title (Z-A)")
        val values = arrayOf("date_desc", "date_asc", "miles_desc", "miles_asc", "title_asc", "title_desc")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Records")
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
