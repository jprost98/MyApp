package com.example.myapp.ui.checkup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.data.Task
import com.example.myapp.data.Vehicle
import com.example.myapp.utils.TaskUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class CheckupViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _vehicles = MutableLiveData<List<Vehicle>>()
    val vehicles: LiveData<List<Vehicle>> = _vehicles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var allTasks = listOf<Task>()
    private var searchQuery = ""
    private var vehicleFilter = "All"   // vehicleId string or "All"
    private var typeFilter = "All"      // "All", "recurring", "single"
    private var sortOption = "date_desc"

    private var databaseRef: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null

    init {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            _isLoading.value = true
            databaseRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            startListening()
        }
    }

    private fun startListening() {
        eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vehicleList = mutableListOf<Vehicle>()
                snapshot.child("vehicles").children.forEach {
                    it.getValue(Vehicle::class.java)?.let { v -> vehicleList.add(v) }
                }
                _vehicles.value = vehicleList

                val taskList = mutableListOf<Task>()
                snapshot.child("tasks").children.forEach {
                    it.getValue(Task::class.java)?.let { task -> taskList.add(task) }
                }
                allTasks = taskList
                applyFiltersAndSort()
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }
        databaseRef?.addValueEventListener(eventListener!!)
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFiltersAndSort()
    }

    fun setVehicleFilter(vehicleId: String) {
        vehicleFilter = vehicleId
        applyFiltersAndSort()
    }

    fun setTypeFilter(type: String) {
        typeFilter = type
        applyFiltersAndSort()
    }

    fun setSort(option: String, ascending: Boolean = true) {
        sortOption = option
        isAscending = ascending
        applyFiltersAndSort()
    }

    private var isAscending = true

    private fun applyFiltersAndSort() {
        var result = allTasks

        if (typeFilter != "All") {
            result = result.filter { it.taskType == typeFilter }
        }

        if (vehicleFilter != "All") {
            result = result.filter { it.taskVehicle == vehicleFilter }
        }

        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.taskName?.contains(searchQuery, ignoreCase = true) == true ||
                it.taskNotes?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        result = when (sortOption) {
            "title" -> if (isAscending) result.sortedBy { it.taskName?.lowercase() ?: "" } else result.sortedByDescending { it.taskName?.lowercase() ?: "" }
            "created" -> if (isAscending) result.sortedBy { it.entryTime ?: 0L } else result.sortedByDescending { it.entryTime ?: 0L }
            "due_date" -> {
                val sorted = result.sortedWith(compareBy<Task> { it.taskCompleted }
                    .thenBy {
                        it.taskDueDate ?: "9999-12-31" // Push null due dates to the end
                    }
                )
                if (isAscending) sorted else sorted.reversed()
            }
            else -> result
        }

        _tasks.value = result
    }

    private fun getStatusValue(task: Task): Int {
        if (task.taskCompleted) return 4 // Completed at the bottom

        if (!task.taskDueDate.isNullOrBlank()) {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val date = isoFormat.parse(task.taskDueDate!!)!!
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.time
                val daysUntilDue = TimeUnit.MILLISECONDS.toDays(date.time - today.time)

                if (daysUntilDue < 0) return 0 // Overdue first
                if (daysUntilDue <= 7) return 1 // Due soon second
                return 2 // Upcoming third
            } catch (e: Exception) {
                // fall through
            }
        }

        if (task.taskType == "recurring") return 3 // Recurring (no due date)
        
        return 5 // Unknown/No info
    }


    fun deleteTask(task: Task) {
        val updated = allTasks.toMutableList()
        updated.removeAll { it.taskId == task.taskId && it.entryTime == task.entryTime }
        databaseRef?.child("tasks")?.setValue(updated)
    }

    fun undoDelete(task: Task, position: Int) {
        val updated = allTasks.toMutableList()
        val insertAt = position.coerceAtMost(updated.size)
        updated.add(insertAt, task)
        databaseRef?.child("tasks")?.setValue(updated)
    }

    fun updateTask(task: Task) {
        val updated = allTasks.toMutableList()
        val index = updated.indexOfFirst {
            it.taskId == task.taskId ||
            (it.taskId == null && it.entryTime == task.entryTime)
        }
        if (index >= 0) {
            updated[index] = task
        } else {
            updated.add(task)
        }
        databaseRef?.child("tasks")?.setValue(updated)
    }

    /** Mark a recurring task as completed today (updates lastDone and calculates next dueDate). */
    fun markRecurringDone(task: Task, dateString: String) {
        val nextDueDate = TaskUtils.calculateNextDueDate(dateString, task.taskFrequency)
        updateTask(task.copy(taskLastDone = dateString, taskDueDate = nextDueDate))
    }

    /** Mark a single task complete (sets taskCompleted = true). */
    fun markSingleDone(task: Task) {
        updateTask(task.copy(taskCompleted = true))
    }

    override fun onCleared() {
        eventListener?.let { databaseRef?.removeEventListener(it) }
    }
}
