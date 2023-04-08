package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class AddVehicle extends AppCompatActivity {

    private int darkMode;
    private final Vehicle vehicle = new Vehicle();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private EditText vehicleYear, vehicleMake, vehicleModel, vehicleSubmodel, vehicleEngine, vehicleNotes;
    private TextInputLayout vehicleYearLayout, vehicleMakeLayout, vehicleModelLayout, vehicleSubmodelLayout, vehicleEngineLayout, vehicleNotesLayout;
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_vehicle);

        setSupportActionBar(findViewById(R.id.add_vehicle_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Add Vehicle");
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

        vehicleYearLayout = findViewById(R.id.vehicle_year_input);
        vehicleMakeLayout = findViewById(R.id.vehicle_make_input);
        vehicleModelLayout = findViewById(R.id.vehicle_model_input);
        vehicleSubmodelLayout = findViewById(R.id.vehicle_submodel_input);
        vehicleEngineLayout = findViewById(R.id.vehicle_engine_input);
        vehicleNotesLayout = findViewById(R.id.vehicle_notes_input);

        vehicleYear = vehicleYearLayout.getEditText();
        vehicleMake = vehicleMakeLayout.getEditText();
        vehicleModel = vehicleModelLayout.getEditText();
        vehicleSubmodel = vehicleSubmodelLayout.getEditText();
        vehicleEngine = vehicleEngineLayout.getEditText();
        vehicleNotes = vehicleNotesLayout.getEditText();
    }

    private void addVehicle() {
        int errors = 0;
        errors = checkVehicleReqs(errors);
        if (errors == 0) {
            vehicleArrayList.addAll(vehicleDao.getAllVehicles());
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
            userRef.child(mUser.getUid()).child("vehicles").setValue(vehicleArrayList);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private int checkVehicleReqs(int errors) {
        errors = 0;
        if (vehicleYear.getText().toString().trim().isEmpty()) {
            vehicleYear.setError("Enter the year of the vehicle");
            errors++;
        }
        if (vehicleMake.getText().toString().trim().isEmpty()) {
            vehicleMake.setError("Enter the make of the vehicle");
            errors++;
        }
        if (vehicleModel.getText().toString().trim().isEmpty()) {
            vehicleModel.setError("Enter the model of the vehicle");
            errors++;
        }
        if (vehicleSubmodel.getText().toString().trim().isEmpty()) {
            vehicleSubmodel.setError("Enter the submodel of the vehicle");
            errors++;
        }
        return errors;
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