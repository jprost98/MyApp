package com.example.myapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.example.myapp.data.Record
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.ActivityAddRecordBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AddRecord : BaseActivity() {

    private lateinit var binding: ActivityAddRecordBinding
    private val vehicles = mutableListOf<Vehicle>()
    private val records = mutableListOf<Record>()
    private var selectedVehicle: Vehicle? = null
    private var selectedDate: Date = Date()

    private val user = FirebaseAuth.getInstance().currentUser
    private val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(user?.uid ?: "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.addRecordTb)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Record"
        }

        setupDatePicker()
        loadInitialData()

        binding.addRecordBtn.setOnClickListener {
            if (validateInputs()) {
                saveRecord()
            }
        }
    }

    private fun setupDatePicker() {
        binding.recordDateInput?.editText?.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(selectedDate.time)
                .setTitleText("Select Date")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedDate = Date(selection)
                binding.recordDateInput?.editText?.setText(SimpleDateFormat.getDateInstance().format(selectedDate))
            }
            picker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun loadInitialData() {
        databaseRef.get().addOnSuccessListener { snapshot ->
            snapshot.child("vehicles").children.forEach {
                it.getValue(Vehicle::class.java)?.let { v -> vehicles.add(v) }
            }
            snapshot.child("records").children.forEach {
                it.getValue(Record::class.java)?.let { r -> records.add(r) }
            }
            setupVehiclePicker()
        }
    }

    private fun setupVehiclePicker() {
        val options = vehicles.map { it.vehicleTitle() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        (binding.recordVehiclePicker?.editText as? AutoCompleteTextView)?.apply {
            setAdapter(adapter)
            setOnItemClickListener { _, _, position, _ ->
                selectedVehicle = vehicles[position]
                binding.recordVehiclePicker?.error = null
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.recordTitleInput?.error = null
        binding.recordVehiclePicker?.error = null
        binding.recordOdometerInput?.error = null

        if (binding.recordTitleInput?.editText?.text.isNullOrBlank()) {
            binding.recordTitleInput?.error = "Title is required"
            isValid = false
        }
        if (selectedVehicle == null) {
            binding.recordVehiclePicker?.error = "Vehicle is required"
            isValid = false
        }
        if (binding.recordOdometerInput?.editText?.text.isNullOrBlank()) {
            binding.recordOdometerInput?.error = "Odometer is required"
            isValid = false
        }
        return isValid
    }

    private fun saveRecord() {
        val newRecord = Record(
            recordId = (records.lastOrNull()?.recordId ?: 0) + 1,
            title = binding.recordTitleInput?.editText?.text.toString(),
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate),
            vehicle = selectedVehicle?.vehicleId.toString(),
            odometer = binding.recordOdometerInput?.editText?.text.toString(),
            description = binding.recordNotesInput?.editText?.text.toString(),
            entryTime = System.currentTimeMillis()
        )

        records.add(newRecord)
        databaseRef.child("records").setValue(records).addOnSuccessListener {
            checkAchievements(newRecord)
            finish()
        }
    }

    private fun checkAchievements(record: Record) {
        val odometer = record.odometer?.toIntOrNull() ?: 0
        if (odometer >= 100000) {
            unlockAchievement("high_mileage", "High Mileage Unlocked!")
        }
    }

    private fun unlockAchievement(id: String, message: String) {
        databaseRef.child("achievements").child(id).setValue(true)
        showNotification("Achievement Unlocked!", message)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "ACHIEVEMENTS"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Achievements", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_achievement_unlocked)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        getSystemService(NotificationManager::class.java).notify(Random().nextInt(), builder.build())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
