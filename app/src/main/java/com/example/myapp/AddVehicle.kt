package com.example.myapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.ActivityAddVehicleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class AddVehicle : BaseActivity() {

    private lateinit var binding: ActivityAddVehicleBinding
    private val vehicleArrayList = mutableListOf<Vehicle>()
    private var yearValue: Int = 0
    private var makeValue: String = ""
    private var modelValue: String = ""

    private val user = FirebaseAuth.getInstance().currentUser
    private val userRef = FirebaseDatabase.getInstance().getReference("users").child(user?.uid ?: "")

    private lateinit var requestQueue: RequestQueue
    private val makeOptions = mutableListOf<String>()
    private val modelOptions = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVehicleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.addVehicleTb as? Toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Vehicle"
        }

        requestQueue = Volley.newRequestQueue(this)
        loadVehicles()
        fetchMakes()
        setupListeners()

        binding.addVehicleBtn.setOnClickListener {
            if (validateInputs()) {
                saveVehicle()
            }
        }
    }

    private fun loadVehicles() {
        userRef.child("vehicles").get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach {
                it.getValue(Vehicle::class.java)?.let { v -> vehicleArrayList.add(v) }
            }
        }
    }

    private fun setupListeners() {
        binding.vehicleYearInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim()
                if (input.length == 4) {
                    yearValue = input.toIntOrNull() ?: 0
                    val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1
                    if (yearValue !in 1941..maxYear) {
                        binding.vehicleYearInput.error = "Invalid year (1941 - $maxYear)"
                    } else {
                        binding.vehicleYearInput.error = null
                        if (makeValue.isNotEmpty()) fetchModels()
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        (binding.vehicleMakeInput.editText as? AutoCompleteTextView)?.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    makeValue = s.toString().trim()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            setOnItemClickListener { _, _, _, _ ->
                makeValue = text.toString().trim()
                if (yearValue > 0) fetchModels()
            }
        }

        (binding.vehicleModelInput.editText as? AutoCompleteTextView)?.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    modelValue = s.toString().trim()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            setOnItemClickListener { _, _, _, _ ->
                modelValue = text.toString().trim()
            }
        }

        binding.vehicleSubmodelInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim()
                if (input.contains("(")) {
                    val parts = input.split("(")
                    val trim = parts[0].trim()
                    val engine = parts[1].replace(")", "").trim()
                    binding.vehicleSubmodelInput.editText?.setText(trim)
                    binding.vehicleEngineInput.editText?.setText(engine)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchMakes() {
        val url = "https://vpic.nhtsa.dot.gov/api/vehicles/getallmakes?format=json"
        val request = StringRequest(Request.Method.GET, url, { response ->
            try {
                val json = JSONObject(response)
                val array = json.getJSONArray("Results")
                makeOptions.clear()
                for (i in 0 until array.length()) {
                    val makeObject = array.getJSONObject(i)
                    makeOptions.add(makeObject.getString("Make_Name"))
                }
                val makeTextView = binding.vehicleMakeInput.editText as? AutoCompleteTextView
                if (makeTextView != null) setupPicker(makeTextView, makeOptions)
            } catch (e: JSONException) { e.printStackTrace() }
        }, { Log.e("Volley", it.toString()) })
        requestQueue.add(request)
    }

    private fun fetchModels() {
        val url = "https://vpic.nhtsa.dot.gov/api/vehicles/getmodelsformakeyear/make/$makeValue/modelyear/$yearValue?format=json"
        val request = StringRequest(Request.Method.GET, url, { response ->
            try {
                val json = JSONObject(response)
                val array = json.getJSONArray("Results")
                modelOptions.clear()
                for (i in 0 until array.length()) {
                    modelOptions.add(array.getJSONObject(i).getString("Model_Name"))
                }
                val modelTextView = binding.vehicleModelInput.editText as? AutoCompleteTextView
                if (modelTextView != null) setupPicker(modelTextView, modelOptions)
            } catch (e: Exception) { e.printStackTrace() }
        }, { Log.e("Volley", it.toString()) })
        requestQueue.add(request)
    }

    private fun setupPicker(textView: AutoCompleteTextView, options: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        textView.setAdapter(adapter)
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.vehicleYearInput.error = null
        binding.vehicleMakeInput.error = null
        binding.vehicleModelInput.error = null
        binding.vehicleSubmodelInput.error = null

        val yearStr = binding.vehicleYearInput.editText?.text.toString().trim()
        val year = yearStr.toIntOrNull() ?: 0
        val maxYear = Calendar.getInstance().get(Calendar.YEAR) + 1

        if (yearStr.isEmpty() || year < 1941 || year > maxYear) {
            binding.vehicleYearInput.error = "Invalid year (1941 - $maxYear)"
            isValid = false
        }
        if (binding.vehicleMakeInput.editText?.text.isNullOrBlank()) {
            binding.vehicleMakeInput.error = "Make is required"
            isValid = false
        }
        if (binding.vehicleModelInput.editText?.text.isNullOrBlank()) {
            binding.vehicleModelInput.error = "Model is required"
            isValid = false
        }
        return isValid
    }

    private fun saveVehicle() {
        val newVehicle = Vehicle(
            vehicleId = (vehicleArrayList.lastOrNull()?.vehicleId ?: 0) + 1,
            year = binding.vehicleYearInput.editText?.text.toString(),
            make = binding.vehicleMakeInput.editText?.text.toString(),
            model = binding.vehicleModelInput.editText?.text.toString(),
            submodel = binding.vehicleSubmodelInput.editText?.text.toString(),
            engine = binding.vehicleEngineInput.editText?.text.toString(),
            notes = binding.vehicleNotesInput.editText?.text.toString(),
            entryTime = System.currentTimeMillis()
        )

        vehicleArrayList.add(newVehicle)
        userRef.child("vehicles").setValue(vehicleArrayList).addOnSuccessListener {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
