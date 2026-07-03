package com.example.myapp.ui.checkup

import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R
import com.example.myapp.TaskAdapter
import com.example.myapp.data.Task
import com.example.myapp.data.Vehicle
import com.example.myapp.utils.TaskUtils
import com.example.myapp.databinding.EditRecurringCheckupBinding
import com.example.myapp.databinding.EditSingleCheckupBinding
import com.example.myapp.databinding.FragmentCheckupBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class CheckupFragment : Fragment() {

    private var _binding: FragmentCheckupBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CheckupViewModel
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckupBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[CheckupViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        setHasOptionsMenu(true)
        return binding.root
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task -> showTaskDetail(task) },
            onMarkDone = { task -> handleMarkDone(task) }
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = taskAdapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.START or ItemTouchHelper.END
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = taskAdapter.currentList[position]

                if (direction == ItemTouchHelper.START) {
                    showDeleteConfirmation(task, position)
                } else {
                    taskAdapter.notifyItemChanged(position)
                    showEditDialog(task)
                }
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isActive: Boolean
            ) {
                val itemView = vh.itemView
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val iconColor = ContextCompat.getColor(requireContext(), R.color.icon_swipe_color)

                    if (dX > 0) {
                        val icon = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.edit_document_24dp_e3e3e3_fill1_wght400_grad0_opsz24
                        )
                        icon?.let {
                            it.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                            val margin = (itemView.height - it.intrinsicHeight) / 2
                            it.setBounds(
                                itemView.left + margin,
                                itemView.top + margin,
                                itemView.left + margin + it.intrinsicWidth,
                                itemView.top + margin + it.intrinsicHeight
                            )
                            it.draw(c)
                        }
                    } else if (dX < 0) {
                        val icon = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.delete_forever_24dp_e3e3e3_fill1_wght400_grad0_opsz24
                        )
                        icon?.let {
                            it.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                            val margin = (itemView.height - it.intrinsicHeight) / 2
                            it.setBounds(
                                itemView.right - margin - it.intrinsicWidth,
                                itemView.top + margin,
                                itemView.right - margin,
                                itemView.top + margin + it.intrinsicHeight
                            )
                            it.draw(c)
                        }
                    }

                    itemView.alpha = 1.0f - Math.abs(dX) / rv.width.toFloat()
                    itemView.translationX = dX
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.tasksRecyclerView)
    }

    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
            binding.noTasksLayout.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            binding.tasksRecyclerView.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.vehicles.observe(viewLifecycleOwner) { vehicles ->
            taskAdapter.updateVehicles(vehicles)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.tasksLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                // RecyclerView / empty-state visibility handled in tasks observer
            }
        }
    }

    // ── Mark Done ────────────────────────────────────────────────────────────

    private fun handleMarkDone(task: Task) {
        if (task.taskType == "recurring") {
            showMarkRecurringDoneDialog(task)
        } else {
            showMarkSingleDoneDialog(task)
        }
    }

    private fun showMarkRecurringDoneDialog(task: Task) {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = isoFormat.format(Date())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mark as Done")
            .setMessage("Record today (${SimpleDateFormat.getDateInstance().format(Date())}) as the last completion date for \"${task.taskName}\"?")
            .setPositiveButton("Mark Done") { _, _ ->
                viewModel.markRecurringDone(task, today)
                Snackbar.make(binding.root, "Task marked as done!", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMarkSingleDoneDialog(task: Task) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Task")
            .setMessage("Mark \"${task.taskName}\" as completed?")
            .setPositiveButton("Complete") { _, _ ->
                viewModel.markSingleDone(task)
                Snackbar.make(binding.root, "Task completed!", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Edit ─────────────────────────────────────────────────────────────────

    private fun showEditDialog(task: Task) {
        if (task.taskType == "recurring") {
            showEditRecurringDialog(task)
        } else {
            showEditSingleDialog(task)
        }
    }

    private fun showEditRecurringDialog(task: Task) {
        val dialogBinding = EditRecurringCheckupBinding.inflate(layoutInflater)
        val vehicles = viewModel.vehicles.value ?: emptyList()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat.getDateInstance()

        var selectedVehicleId = task.taskVehicle
        var lastDoneString = task.taskLastDone ?: ""

        dialogBinding.apply {
            rcEditTaskName.editText?.setText(task.taskName)
            rcEditNotes.editText?.setText(task.taskNotes)

            // Frequency
            val freq = task.taskFrequency ?: ""
            if (freq.endsWith("miles")) {
                rcEditMileageRb.isChecked = true
                rcEditTimeRb.isChecked = false
                rcEditMileageLayout.visibility = View.VISIBLE
                rcEditTimeLayout.visibility = View.GONE
                rcEditMileageInput.editText?.setText(freq.removeSuffix(" miles").trim())
            } else {
                rcEditTimeRb.isChecked = true
                rcEditMileageRb.isChecked = false
                rcEditTimeLayout.visibility = View.VISIBLE
                rcEditMileageLayout.visibility = View.GONE
                val parts = freq.split(" ")
                if (parts.size >= 2) {
                    rcEditTimeInput.editText?.setText(parts[0])
                    val freqOptions = resources.getStringArray(R.array.time_frequencies)
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, freqOptions)
                    (rcEditTimeFrequencyOptions as? AutoCompleteTextView)?.apply {
                        setAdapter(adapter)
                        setText(parts[1], false)
                    }
                }
            }

            // Vehicle picker
            val vehicleNames = vehicles.map { it.vehicleTitle() }
            val vehicleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleNames)
            rcEditVehicleOptions.setAdapter(vehicleAdapter)
            rcEditVehicleOptions.setText(
                vehicles.find { it.vehicleId.toString() == selectedVehicleId }?.vehicleTitle() ?: "",
                false
            )
            rcEditVehicleOptions.setOnItemClickListener { _, _, pos, _ ->
                selectedVehicleId = vehicles[pos].vehicleId.toString()
            }

            // Done before
            if (!task.taskLastDone.isNullOrBlank()) {
                rcEditDoneBeforeBox.isChecked = true
                rcEditDoneBeforeDate.visibility = View.VISIBLE
                try {
                    val date = isoFormat.parse(task.taskLastDone!!)
                    rcEditDoneBeforeDate.editText?.setText(displayFormat.format(date!!))
                } catch (e: Exception) {
                    rcEditDoneBeforeDate.editText?.setText(task.taskLastDone)
                }
            }
            rcEditDoneBeforeBox.setOnCheckedChangeListener { _, checked ->
                rcEditDoneBeforeDate.visibility = if (checked) View.VISIBLE else View.GONE
            }
            rcEditDoneBeforeDate.editText?.setOnClickListener {
                val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Date of Work").build()
                picker.addOnPositiveButtonClickListener { selection ->
                    val tz = TimeZone.getDefault()
                    val offset = tz.getOffset(Date().time) * -1
                    val date = Date(selection + offset)
                    lastDoneString = isoFormat.format(date)
                    rcEditDoneBeforeDate.editText?.setText(displayFormat.format(date))
                }
                picker.show(childFragmentManager, "DONE_DATE")
            }

            rcEditMileageRb.setOnClickListener {
                rcEditMileageLayout.visibility = View.VISIBLE
                rcEditTimeLayout.visibility = View.GONE
            }
            rcEditTimeRb.setOnClickListener {
                rcEditTimeLayout.visibility = View.VISIBLE
                rcEditMileageLayout.visibility = View.GONE
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val freqOptions = resources.getStringArray(R.array.time_frequencies)
                val defaultFreq = freqOptions.firstOrNull() ?: "months"
                val frequency = if (dialogBinding.rcEditMileageRb.isChecked) {
                    "${dialogBinding.rcEditMileageInput.editText?.text?.toString()?.trim()} miles"
                } else {
                    val num = dialogBinding.rcEditTimeInput.editText?.text?.toString()?.trim() ?: ""
                    val unit = (dialogBinding.rcEditTimeFrequencyOptions as? AutoCompleteTextView)
                        ?.text?.toString()?.trim() ?: defaultFreq
                    "$num $unit"
                }
                val lastDone = if (dialogBinding.rcEditDoneBeforeBox.isChecked && lastDoneString.isNotBlank()) lastDoneString else task.taskLastDone
                val dueDate = if (frequency.contains("miles")) null else TaskUtils.calculateNextDueDate(lastDone, frequency)
                
                val updatedTask = task.copy(
                    taskName = dialogBinding.rcEditTaskName.editText?.text?.toString()?.trim(),
                    taskVehicle = selectedVehicleId,
                    taskFrequency = frequency,
                    taskNotes = dialogBinding.rcEditNotes.editText?.text?.toString()?.trim(),
                    taskLastDone = lastDone,
                    taskDueDate = dueDate
                )
                viewModel.updateTask(updatedTask)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditSingleDialog(task: Task) {
        val dialogBinding = EditSingleCheckupBinding.inflate(layoutInflater)
        val vehicles = viewModel.vehicles.value ?: emptyList()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat.getDateInstance()

        var selectedVehicleId = task.taskVehicle
        var selectedDateString = task.taskDueDate ?: ""

        dialogBinding.apply {
            scEditTaskName.editText?.setText(task.taskName)
            scEditNotes.editText?.setText(task.taskNotes)
            scEditMileage.editText?.setText(task.taskDueMileage ?: "")

            if (!task.taskDueDate.isNullOrBlank()) {
                try {
                    val date = isoFormat.parse(task.taskDueDate!!)
                    scEditDate.editText?.setText(displayFormat.format(date!!))
                } catch (e: Exception) {
                    scEditDate.editText?.setText(task.taskDueDate)
                }
            }
            scEditDate.editText?.setOnClickListener {
                val picker = MaterialDatePicker.Builder.datePicker().setTitleText("Due Date").build()
                picker.addOnPositiveButtonClickListener { selection ->
                    val tz = TimeZone.getDefault()
                    val offset = tz.getOffset(Date().time) * -1
                    val date = Date(selection + offset)
                    selectedDateString = isoFormat.format(date)
                    scEditDate.editText?.setText(displayFormat.format(date))
                }
                picker.show(childFragmentManager, "DUE_DATE")
            }

            val vehicleNames = vehicles.map { it.vehicleTitle() }
            val vehicleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleNames)
            scEditVehicleOptions.setAdapter(vehicleAdapter)
            scEditVehicleOptions.setText(
                vehicles.find { it.vehicleId.toString() == selectedVehicleId }?.vehicleTitle() ?: "",
                false
            )
            scEditVehicleOptions.setOnItemClickListener { _, _, pos, _ ->
                selectedVehicleId = vehicles[pos].vehicleId.toString()
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val mileageValue = dialogBinding.scEditMileage.editText?.text?.toString()?.trim()
                val updatedTask = task.copy(
                    taskName = dialogBinding.scEditTaskName.editText?.text?.toString()?.trim(),
                    taskVehicle = selectedVehicleId,
                    taskDueDate = selectedDateString.ifBlank { null },
                    taskDueMileage = if (mileageValue.isNullOrBlank()) null else mileageValue,
                    taskNotes = dialogBinding.scEditNotes.editText?.text?.toString()?.trim()
                )
                viewModel.updateTask(updatedTask)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Detail (tap) ──────────────────────────────────────────────────────────

    private fun showTaskDetail(task: Task) {
        // Tapping a task card opens the edit dialog for quick edits
        showEditDialog(task)
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private fun showDeleteConfirmation(task: Task, position: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.taskName}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task)
                Snackbar.make(binding.root, "Task deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { viewModel.undoDelete(task, position) }
                    .show()
            }
            .setNegativeButton("Cancel") { _, _ -> taskAdapter.notifyItemChanged(position) }
            .show()
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tasks_menu, menu)
        val searchItem = menu.findItem(R.id.app_bar_search)
        val searchView = searchItem.actionView as? SearchView
        searchView?.queryHint = "Search tasks…"
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
            R.id.filter_tasks -> {
                showFilterDialog()
                true
            }
            R.id.sort_tasks -> {
                showSortDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val vehicles = viewModel.vehicles.value ?: emptyList()
        val typeOptions = arrayOf("All Types", "Recurring", "One-Time")
        val typeValues = arrayOf("All", "recurring", "single")

        val vehicleOptions = mutableListOf("All Vehicles")
        vehicleOptions.addAll(vehicles.map { it.vehicleTitle() })

        // Two-step filter: type first, then vehicle
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Type")
            .setItems(typeOptions) { _, typeWhich ->
                viewModel.setTypeFilter(typeValues[typeWhich])
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filter by Vehicle")
                    .setItems(vehicleOptions.toTypedArray()) { _, vWhich ->
                        val vehicleId = if (vWhich == 0) "All" else vehicles[vWhich - 1].vehicleId.toString()
                        viewModel.setVehicleFilter(vehicleId)
                    }
                    .show()
            }
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Title", "Date Created", "Due Date")
        val values = arrayOf("title", "created", "due_date")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Tasks By")
            .setItems(options) { _, which ->
                val selectedSort = values[which]
                
                // After selecting what to sort by, ask for order
                val orderOptions = arrayOf("Ascending", "Descending")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Order")
                    .setItems(orderOptions) { _, orderWhich ->
                        val isAscending = orderWhich == 0
                        viewModel.setSort(selectedSort, isAscending)
                    }
                    .show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
