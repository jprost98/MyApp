package com.example.myapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.data.Record
import com.example.myapp.data.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeViewModel : ViewModel() {

    private val _records = MutableLiveData<List<Record>>()
    val records: LiveData<List<Record>> = _records

    private val _vehicles = MutableLiveData<List<Vehicle>>()
    val vehicles: LiveData<List<Vehicle>> = _vehicles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var databaseRef: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null

    private var currentFilter: String = "All"
    private var currentSort: String = "date_desc"
    private var searchQuery: String = ""

    private var allRecords: List<Record> = emptyList()

    init {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            databaseRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            startListening()
        }
    }

    private fun startListening() {
        _isLoading.value = true
        eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vehicleList = mutableListOf<Vehicle>()
                snapshot.child("vehicles").children.forEach {
                    it.getValue(Vehicle::class.java)?.let { vehicle -> vehicleList.add(vehicle) }
                }
                _vehicles.value = vehicleList

                val recordList = mutableListOf<Record>()
                snapshot.child("records").children.forEach {
                    it.getValue(Record::class.java)?.let { record -> recordList.add(record) }
                }
                allRecords = recordList
                applyFiltersAndSort()
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }
        databaseRef?.addValueEventListener(eventListener!!)
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        applyFiltersAndSort()
    }

    fun setSort(sort: String) {
        currentSort = sort
        applyFiltersAndSort()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var filtered = allRecords

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title?.contains(searchQuery, ignoreCase = true) == true ||
                it.description?.contains(searchQuery, ignoreCase = true) == true ||
                it.date?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        if (currentFilter != "All") {
            filtered = filtered.filter { it.vehicle == currentFilter }
        }

        val sorted = when (currentSort) {
            "date_desc" -> filtered.sortedWith(compareByDescending<Record> { it.date }.thenByDescending { it.entryTime })
            "date_asc" -> filtered.sortedWith(compareBy<Record> { it.date }.thenBy { it.entryTime })
            "miles_desc" -> filtered.sortedWith(compareByDescending<Record> { it.odometer?.toIntOrNull() ?: 0 }.thenByDescending { it.entryTime })
            "miles_asc" -> filtered.sortedWith(compareBy<Record> { it.odometer?.toIntOrNull() ?: 0 }.thenBy { it.entryTime })
            "title_desc" -> filtered.sortedWith(compareByDescending<Record> { it.title }.thenByDescending { it.entryTime })
            "title_asc" -> filtered.sortedWith(compareBy<Record> { it.title }.thenBy { it.entryTime })
            else -> filtered
        }

        _records.value = sorted
    }

    fun deleteRecord(record: Record) {
        val newList = allRecords.toMutableList()
        newList.remove(record)
        databaseRef?.child("records")?.setValue(newList)
    }

    fun undoDelete(record: Record, position: Int) {
        val newList = allRecords.toMutableList()
        if (position <= newList.size) {
            newList.add(position, record)
        } else {
            newList.add(record)
        }
        databaseRef?.child("records")?.setValue(newList)
    }

    fun updateRecord(updatedRecord: Record) {
        val newList = allRecords.toMutableList()
        val index = newList.indexOfFirst { it.recordId == updatedRecord.recordId }
        if (index != -1) {
            newList[index] = updatedRecord
            databaseRef?.child("records")?.setValue(newList)
        }
    }

    override fun onCleared() {
        eventListener?.let { databaseRef?.removeEventListener(it) }
    }
}
