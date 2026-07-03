package com.example.myapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapp.data.Record
import com.example.myapp.data.Task
import com.example.myapp.data.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SharedViewModel : ViewModel() {

    private val _vehicles = MutableLiveData<List<Vehicle>>(emptyList())
    val vehicles: LiveData<List<Vehicle>> = _vehicles

    private val _records = MutableLiveData<List<Record>>(emptyList())
    val records: LiveData<List<Record>> = _records

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> = _tasks

    private val _themeSelection = MutableLiveData<String?>()
    val themeSelection: LiveData<String?> = _themeSelection

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var databaseRef: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null

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
                _records.value = recordList

                val taskList = mutableListOf<Task>()
                snapshot.child("tasks").children.forEach {
                    it.getValue(Task::class.java)?.let { task -> taskList.add(task) }
                }
                _tasks.value = taskList

                _themeSelection.value = snapshot.child("settings").child("theme").getValue(String::class.java)
                
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }
        databaseRef?.addValueEventListener(eventListener!!)
    }

    override fun onCleared() {
        eventListener?.let { databaseRef?.removeEventListener(it) }
    }
}