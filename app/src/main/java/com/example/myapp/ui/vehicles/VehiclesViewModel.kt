package com.example.myapp.ui.vehicles

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.data.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class VehiclesViewModel : ViewModel() {

    private val _vehicles = MutableLiveData<List<Vehicle>>()
    val vehicles: LiveData<List<Vehicle>> = _vehicles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var databaseRef: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null

    private var allVehicles: List<Vehicle> = emptyList()
    private var currentSort: String = "make_asc"
    private var searchQuery: String = ""

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
                allVehicles = vehicleList
                applyFiltersAndSort()
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }
        databaseRef?.addValueEventListener(eventListener!!)
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
        var filtered = allVehicles

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.make?.contains(searchQuery, ignoreCase = true) == true ||
                it.model?.contains(searchQuery, ignoreCase = true) == true ||
                it.year?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        val sorted = when (currentSort) {
            "year_desc" -> filtered.sortedWith(compareByDescending<Vehicle> { it.year }.thenByDescending { it.entryTime })
            "year_asc" -> filtered.sortedWith(compareBy<Vehicle> { it.year }.thenBy { it.entryTime })
            "make_desc" -> filtered.sortedWith(compareByDescending<Vehicle> { it.make }.thenByDescending { it.entryTime })
            "make_asc" -> filtered.sortedWith(compareBy<Vehicle> { it.make }.thenBy { it.entryTime })
            else -> filtered
        }

        _vehicles.value = sorted
    }

    fun deleteVehicle(vehicle: Vehicle) {
        val newList = allVehicles.toMutableList()
        newList.remove(vehicle)
        databaseRef?.child("vehicles")?.setValue(newList)
    }

    fun updateVehicle(updatedVehicle: Vehicle) {
        val newList = allVehicles.toMutableList()
        val index = newList.indexOfFirst { it.vehicleId == updatedVehicle.vehicleId }
        if (index != -1) {
            newList[index] = updatedVehicle
            databaseRef?.child("vehicles")?.setValue(newList)
        }
    }

    override fun onCleared() {
        eventListener?.let { databaseRef?.removeEventListener(it) }
    }
}
