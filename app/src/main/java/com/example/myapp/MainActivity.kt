package com.example.myapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapp.data.Record
import com.example.myapp.data.Vehicle
import com.example.myapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    private val database = FirebaseDatabase.getInstance()
    private val mAuth = FirebaseAuth.getInstance()
    private var userRef: DatabaseReference? = null
    private var eventListener: ValueEventListener? = null

    private val vehicleArrayList = ArrayList<Vehicle>()
    private val recordArrayList = ArrayList<Record>()
    private val taskArrayList = ArrayList<com.example.myapp.data.Task>()

    private var isFabOpen = false
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var rotateClock: Animation
    private lateinit var rotateAnticlock: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupNavigation()
        setupAnimations()
        initFirebase()
        setupFab()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_vehicles, R.id.navigation_checkups, R.id.navigation_settings)
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_settings) {
                binding.fab.hide()
            } else {
                binding.fab.show()
            }
            if (isFabOpen) closeFabMenu()
        }
    }

    private fun setupAnimations() {
        fabOpen = AnimationUtils.loadAnimation(this, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(this, R.anim.fab_close)
        rotateClock = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_clock)
        rotateAnticlock = AnimationUtils.loadAnimation(this, R.anim.fab_rotate_anticlock)
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            handleFabAction()
        }

        binding.recurringEventFab.setOnClickListener {
            if (vehicleArrayList.isNotEmpty()) {
                startActivity(Intent(this, AddRecurringCheckup::class.java))
            } else {
                showAddVehiclePrompt()
            }
        }

        binding.singleEventFab.setOnClickListener {
            if (vehicleArrayList.isNotEmpty()) {
                startActivity(Intent(this, AddSingleCheckup::class.java))
            } else {
                showAddVehiclePrompt()
            }
        }
    }

    private fun handleFabAction() {
        when (navController.currentDestination?.id) {
            R.id.navigation_home -> {
                if (vehicleArrayList.isNotEmpty()) {
                    startActivity(Intent(this, AddRecord::class.java))
                } else {
                    showAddVehiclePrompt()
                }
            }
            R.id.navigation_vehicles -> {
                startActivity(Intent(this, AddVehicle::class.java))
            }
            R.id.navigation_checkups -> {
                toggleFabMenu()
            }
        }
    }

    private fun toggleFabMenu() {
        if (isFabOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        binding.recurringEventFab.visibility = View.VISIBLE
        binding.singleEventFab.visibility = View.VISIBLE
        binding.recurringEventFab.startAnimation(fabOpen)
        binding.singleEventFab.startAnimation(fabOpen)
        binding.fab.startAnimation(rotateClock)
        binding.recurringEventFab.isClickable = true
        binding.singleEventFab.isClickable = true
        binding.fab.extend()
        isFabOpen = true
    }

    private fun closeFabMenu() {
        binding.recurringEventFab.startAnimation(fabClose)
        binding.singleEventFab.startAnimation(fabClose)
        binding.fab.startAnimation(rotateAnticlock)
        binding.recurringEventFab.isClickable = false
        binding.singleEventFab.isClickable = false
        binding.recurringEventFab.visibility = View.GONE
        binding.singleEventFab.visibility = View.GONE
        binding.fab.shrink()
        isFabOpen = false
    }

    private fun showAddVehiclePrompt() {
        Snackbar.make(binding.bottomNavView, "Add a vehicle first.", Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.bottomNavView)
            .setAction("Add vehicle") {
                startActivity(Intent(this, AddVehicle::class.java))
            }
            .show()
    }

    private fun initFirebase() {
        val user = mAuth.currentUser ?: return
        userRef = database.getReference("users").child(user.uid)
        userRef?.child("user_info")?.child("version")?.setValue(getString(R.string.version))
        
        addEventListener()
    }

    private fun addEventListener() {
        eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                vehicleArrayList.clear()
                snapshot.child("vehicles").children.forEach {
                    it.getValue(Vehicle::class.java)?.let { v -> vehicleArrayList.add(v) }
                }

                recordArrayList.clear()
                snapshot.child("records").children.forEach {
                    it.getValue(Record::class.java)?.let { r -> recordArrayList.add(r) }
                }

                taskArrayList.clear()
                snapshot.child("tasks").children.forEach {
                    it.getValue(com.example.myapp.data.Task::class.java)?.let { t -> taskArrayList.add(t) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Firebase error: ${error.message}")
            }
        }
        userRef?.addValueEventListener(eventListener!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.let { userRef?.removeEventListener(it) }
    }
}
