package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDao;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AddRecord extends AppCompatActivity {

    private int darkMode;
    private Record record = new Record();
    private Vehicle vehicle = new Vehicle();
    private ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private ArrayList<Record> recordArrayList = new ArrayList<>();
    private ArrayList<String> spinnerOptions = new ArrayList<>();
    private Spinner recordVehicle;
    private EditText recordTitle, recordDescription, recordDate, recordOdometer;
    final Calendar myCalendar = Calendar.getInstance();
    private RecordDatabase recordDatabase;
    private RecordDao recordDao;
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        int darkMode = sharedPref.getInt("dark_mode", 0);
        int themePref = sharedPref.getInt("theme_pref", 0);
        if (themePref == 0) this.setTheme(R.style.DefaultTheme);
        else if (themePref == 1) this.setTheme(R.style.RedTheme);
        else if (themePref == 2) this.setTheme(R.style.BlueTheme);
        else if (themePref == 3) this.setTheme(R.style.GreenTheme);
        else if (themePref == 4) this.setTheme(R.style.GreyscaleTheme);
        Log.d("Theme", String.valueOf(themePref));
        setTitle("Add Maintenance Record");
        if (darkMode == 0) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_record);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initFirebase();
        getVehicles();
        initSpinner();
        initVars();

        Button addRecordButton = findViewById(R.id.add_record_btn);
        addRecordButton.setOnClickListener(view -> {
            addRecord();
        });

    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    private void getVehicles() {
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
    }

    private void initVars() {
        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        recordDao = recordDatabase.recordDao();
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleDao = vehicleDatabase.vehicleDao();
        recordTitle = findViewById(R.id.record_title_input);
        recordDate = findViewById(R.id.record_date_input);
        createCalender();
        recordVehicle = findViewById(R.id.record_vehicle_spinner);
        recordOdometer = findViewById(R.id.record_input_odometer);
        recordDescription = findViewById(R.id.record_input_description);
        recordArrayList.clear();
    }

    private void initSpinner() {
        recordVehicle = findViewById(R.id.record_vehicle_spinner);
        for (Vehicle vehicle: vehicleArrayList) {
            spinnerOptions.add(vehicle.vehicleTitle());
        }
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
        stringArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recordVehicle.setAdapter(stringArrayAdapter);
    }

    private void addRecord() {
        int errors = checkRecordReqs();
        if (errors == 0) {
            record.setTitle(recordTitle.getText().toString().trim());
            record.setDate(recordDate.getText().toString().trim());
            record.setVehicle(recordVehicle.getSelectedItem().toString());
            record.setOdometer(recordOdometer.getText().toString().trim());
            record.setDescription(recordDescription.getText().toString().trim());
            record.setEntryTime(Calendar.getInstance().getTimeInMillis());
            Log.d("New Record", record.toString());
            recordDao.addRecord(record);
            recordArrayList.addAll(recordDao.getAllRecords());
            userRef.child(mUser.getUid()).child("Records").child(String.valueOf(recordArrayList.size() - 1)).setValue(record);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private int checkRecordReqs() {
        int errors = 0;
        if (recordTitle.getText().toString().trim().isEmpty()) {
            recordTitle.setError("Enter a title for the record");
            errors++;
        }
        if (recordDate.getText().toString().trim().isEmpty()) {
            recordDate.setError("Enter the date of the maintenance");
            errors++;
        }
        if (recordOdometer.getText().toString().trim().isEmpty()) {
            recordOdometer.setError("Enter the odometer reading for the record");
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

    private void createCalender() {
        DatePickerDialog.OnDateSetListener datePicker = (view, year, monthOfYear, dayOfMonth) -> {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String myFormat = "MM/dd/yy"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

            recordDate.setText(sdf.format(myCalendar.getTime()));
        };

        recordDate.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            new DatePickerDialog(this, datePicker, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }
}