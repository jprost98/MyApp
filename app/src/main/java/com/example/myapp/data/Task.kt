package com.example.myapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Task(
    var taskId: String? = null,        // UUID — stable unique key
    var taskName: String? = null,
    var taskVehicle: String? = null,   // vehicleId as String
    var taskLastDone: String? = null,  // "yyyy-MM-dd" — recurring: date last completed
    var taskDueDate: String? = null,    // "yyyy-MM-dd" — single: due date
    var taskDueMileage: String? = null, // single: due at this odometer reading
    var taskFrequency: String? = null,  // e.g. "5000 miles" or "3 months"
    var taskNotes: String? = null,
    var taskType: String? = null,      // "recurring" or "single"
    var taskCompleted: Boolean = false, // single tasks: marked complete
    var entryTime: Long? = null        // creation timestamp (fallback sort key)
) : Parcelable
