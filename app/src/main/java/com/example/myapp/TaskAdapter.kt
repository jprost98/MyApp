package com.example.myapp

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.data.Task
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.TaskViewBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onMarkDone: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var vehicles: List<Vehicle> = emptyList()

    fun updateVehicles(newVehicles: List<Vehicle>) {
        vehicles = newVehicles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = TaskViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), vehicles, onTaskClick, onMarkDone)
    }

    class TaskViewHolder(private val binding: TaskViewBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            task: Task,
            vehicles: List<Vehicle>,
            onClick: (Task) -> Unit,
            onMarkDone: (Task) -> Unit
        ) {
            val ctx = binding.root.context
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat.getDateInstance()

            // Title
            binding.taskViewName.text = task.taskName

            // Strike-through completed single tasks
            if (task.taskCompleted) {
                binding.taskViewName.paintFlags = binding.taskViewName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskViewName.alpha = 0.5f
            } else {
                binding.taskViewName.paintFlags = binding.taskViewName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskViewName.alpha = 1f
            }

            // Vehicle
            val vehicle = vehicles.find { it.vehicleId.toString() == task.taskVehicle }
            binding.taskViewVehicle.text = vehicle?.vehicleTitle() ?: "Unknown Vehicle"

            if (task.taskType == "recurring") {
                binding.taskViewFrequencyLayout.visibility = View.VISIBLE
                
                // Last done
                if (!task.taskLastDone.isNullOrBlank()) {
                    try {
                        val date = isoFormat.parse(task.taskLastDone!!)
                        binding.taskViewLastDoneDate.text = displayFormat.format(date!!)
                    } catch (e: Exception) {
                        binding.taskViewLastDoneDate.text = task.taskLastDone
                    }
                } else {
                    binding.taskViewLastDoneDate.text = "Never"
                }
                binding.taskViewFrequency.text = task.taskFrequency

                // Due Date for recurring
                if (!task.taskDueDate.isNullOrBlank()) {
                    binding.taskViewDueDateLayout.visibility = View.VISIBLE
                    binding.taskViewDueMileageRow.visibility = View.GONE
                    binding.taskViewDueDateRow.visibility = View.VISIBLE
                    try {
                        val date = isoFormat.parse(task.taskDueDate!!)!!
                        binding.taskViewDueDate.text = displayFormat.format(date)

                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.time
                        val daysUntilDue = TimeUnit.MILLISECONDS.toDays(date.time - today.time)

                        binding.taskStatusChip.visibility = View.VISIBLE
                        if (daysUntilDue < 0) {
                            binding.taskStatusChip.text = "Overdue"
                            binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_overdue)
                        } else if (daysUntilDue <= 7) {
                            binding.taskStatusChip.text = "Due Soon"
                            binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_due_soon)
                        } else {
                            binding.taskStatusChip.text = "Recurring"
                            binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_recurring)
                        }
                    } catch (e: Exception) {
                        binding.taskViewDueDate.text = task.taskDueDate
                        binding.taskStatusChip.visibility = View.VISIBLE
                        binding.taskStatusChip.text = "Recurring"
                        binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_recurring)
                    }
                } else {
                    binding.taskViewDueDateLayout.visibility = View.GONE
                    binding.taskStatusChip.visibility = View.VISIBLE
                    binding.taskStatusChip.text = "Recurring"
                    binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_recurring)
                }

                // Mark Done button always available for recurring
                binding.taskMarkDoneBtn.visibility = View.VISIBLE
                binding.taskMarkDoneBtn.isEnabled = true
                binding.taskMarkDoneBtn.text = "Mark as Done"

            } else {
                // Single task
                binding.taskViewFrequencyLayout.visibility = View.GONE
                binding.taskViewDueDateLayout.visibility = View.VISIBLE

                // Date row
                if (!task.taskDueDate.isNullOrBlank()) {
                    binding.taskViewDueDateRow.visibility = View.VISIBLE
                    try {
                        val date = isoFormat.parse(task.taskDueDate!!)!!
                        binding.taskViewDueDate.text = displayFormat.format(date)

                        // Status chip based on date
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.time
                        val daysUntilDue = TimeUnit.MILLISECONDS.toDays(date.time - today.time)

                        binding.taskStatusChip.visibility = View.VISIBLE
                        when {
                            task.taskCompleted -> {
                                binding.taskStatusChip.text = "Completed"
                                binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_complete)
                            }
                            daysUntilDue < 0 -> {
                                binding.taskStatusChip.text = "Overdue"
                                binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_overdue)
                            }
                            daysUntilDue <= 7 -> {
                                binding.taskStatusChip.text = "Due Soon"
                                binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_due_soon)
                            }
                            else -> {
                                binding.taskStatusChip.text = "Upcoming"
                                binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_upcoming)
                            }
                        }
                    } catch (e: Exception) {
                        binding.taskViewDueDate.text = task.taskDueDate
                        binding.taskStatusChip.visibility = View.GONE
                    }
                } else {
                    binding.taskViewDueDateRow.visibility = View.GONE
                }

                // Mileage row
                if (!task.taskDueMileage.isNullOrBlank()) {
                    binding.taskViewDueMileageRow.visibility = View.VISIBLE
                    val formatted = try {
                        "%,d mi".format(task.taskDueMileage!!.toLong())
                    } catch (e: Exception) {
                        "${task.taskDueMileage} mi"
                    }
                    binding.taskViewDueMileage.text = formatted
                } else {
                    binding.taskViewDueMileageRow.visibility = View.GONE
                }

                // If only mileage is set (no date), chip just shows Completed or One-Time
                if (task.taskDueDate.isNullOrBlank()) {
                    binding.taskStatusChip.visibility = View.VISIBLE
                    if (task.taskCompleted) {
                        binding.taskStatusChip.text = "Completed"
                        binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_complete)
                    } else {
                        binding.taskStatusChip.text = "One-Time"
                        binding.taskStatusChip.setChipBackgroundColorResource(R.color.chip_upcoming)
                    }
                }

                // Hide Mark Done if already completed
                if (task.taskCompleted) {
                    binding.taskMarkDoneBtn.visibility = View.GONE
                } else {
                    binding.taskMarkDoneBtn.visibility = View.VISIBLE
                    binding.taskMarkDoneBtn.text = "Mark Complete"
                }
            }

            // Notes
            if (!task.taskNotes.isNullOrBlank()) {
                binding.taskViewNotesLayout.visibility = View.VISIBLE
                binding.taskViewNotes.text = task.taskNotes
            } else {
                binding.taskViewNotesLayout.visibility = View.GONE
            }

            binding.taskMaterialCard.setOnClickListener { onClick(task) }
            binding.taskMarkDoneBtn.setOnClickListener { onMarkDone(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            // Prefer stable taskId; fall back to entryTime for legacy tasks
            return if (oldItem.taskId != null && newItem.taskId != null) {
                oldItem.taskId == newItem.taskId
            } else {
                oldItem.entryTime == newItem.entryTime
            }
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
