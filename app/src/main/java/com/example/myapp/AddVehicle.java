package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class AddVehicle extends AppCompatActivity {

    private Integer savedPref;
    private Vehicle vehicle = new Vehicle();
    private ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private EditText vehicleYear, vehicleMake, vehicleModel, vehicleSubmodel, vehicleEngine, vehicleNotes;
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPref.edit();
        savedPref = sharedPref.getInt(getString(R.string.saved_value), 100);
        Log.d("Saved Pref", savedPref.toString());
        if (savedPref == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Log.d("Theme", "Light Theme");
        } else if (savedPref == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Log.d("Theme", "Dark Theme");
        }
        setTitle("Add Vehicle");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initFirebase();
        initVars();

        Button addVehicleBtn = findViewById(R.id.add_vehicle_btn);
        addVehicleBtn.setOnClickListener(view -> {
            addVehicle();
        });
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initVars() {
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleDao = vehicleDatabase.vehicleDao();
        vehicleYear = findViewById(R.id.vehicle_year_input);
        vehicleMake = findViewById(R.id.vehicle_make_input);
        vehicleModel = findViewById(R.id.vehicle_model_input);
        vehicleSubmodel = findViewById(R.id.vehicle_submodel_input);
        vehicleEngine = findViewById(R.id.vehicle_engine_input);
        vehicleNotes = findViewById(R.id.vehicle_notes_input);
    }

    private void addVehicle() {
        vehicle.setYear(vehicleYear.getText().toString().trim());
        vehicle.setMake(vehicleMake.getText().toString().trim());
        vehicle.setModel(vehicleModel.getText().toString().trim());
        vehicle.setSubmodel(vehicleSubmodel.getText().toString().trim());
        vehicle.setEngine(vehicleEngine.getText().toString().trim());
        vehicle.setNotes(vehicleNotes.getText().toString().trim());
        vehicle.setEntryTime(Calendar.getInstance().getTimeInMillis());
        Log.d("New Vehicle", vehicle.toString());
        vehicleDao.addVehicle(vehicle);
        vehicleArrayList.clear();
        vehicleArrayList.addAll(vehicleDao.getAllVehicles());
        userRef.child(mUser.getDisplayName()).child("Vehicles").child(String.valueOf(vehicleArrayList.size() - 1)).setValue(vehicle);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}