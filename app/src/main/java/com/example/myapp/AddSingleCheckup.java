package com.example.myapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class AddSingleCheckup extends BaseActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;

    // Vehicles
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private AutoCompleteTextView scVehiclePicker;
    private int vehicleSelection;

    // Task
    private final ArrayList<Task> taskArrayList = new ArrayList<>();
    private String taskDateString;

    // Layout
    private TextInputLayout scTaskNameLayout, scVehicleLayout, scDateLayout, scMileageLayout, scNotesLayout;
    private EditText scTaskName, scDate, scMileage, scNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_single_checkup);

        setSupportActionBar(findViewById(R.id.single_checkup_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Create One-Time Task");
        }

        initFirebase();
        initVars();

        Button finishBtn = findViewById(R.id.sc_finish_btn);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        scDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long date = 0;
                if (!scDate.getText().toString().isEmpty()) {
                    try {
                        date = Objects.requireNonNull(SimpleDateFormat.getDateInstance().parse(scDate.getText().toString())).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    date = MaterialDatePicker.todayInUtcMilliseconds();
                }
                MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Due Date")
                        .setSelection(date)
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                    @Override
                    public void onPositiveButtonClick(Long selection) {
                        TimeZone timeZoneUTC = TimeZone.getDefault();
                        int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
                        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date date = new Date(selection + offsetFromUTC);
                        taskDateString = simpleFormat.format(date);
                        scDate.setText(SimpleDateFormat.getDateInstance().format(date));
                    }
                });
                materialDatePicker.show(getSupportFragmentManager(), "date");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference("users/" + mUser.getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    for (DataSnapshot dataSnapshot : task.getResult().child("vehicles").getChildren()) {
                        vehicleArrayList.add(dataSnapshot.getValue(Vehicle.class));
                    }
                    for (DataSnapshot dataSnapshot : task.getResult().child("tasks").getChildren()) {
                        taskArrayList.add(dataSnapshot.getValue(Task.class));
                    }
                    initVehiclePicker();
                }
            }
        });
    }

    private void initVehiclePicker() {
        ArrayList<String> vehicleOptions = new ArrayList<>();
        for (Vehicle vehicle : vehicleArrayList) {
            vehicleOptions.add(vehicle.vehicleTitle());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, vehicleOptions);
        scVehiclePicker = findViewById(R.id.sc_vehicle_options);
        scVehiclePicker.setAdapter(adapter);
        scVehiclePicker.setOnItemClickListener((adapterView, view, i, l) -> vehicleSelection = i);
    }

    private void initVars() {
        scTaskNameLayout = findViewById(R.id.sc_task_name);
        scVehicleLayout = findViewById(R.id.sc_vehicle_picker);
        scDateLayout = findViewById(R.id.sc_date);
        scMileageLayout = findViewById(R.id.sc_mileage);
        scNotesLayout = findViewById(R.id.sc_notes);

        scTaskName = scTaskNameLayout.getEditText();
        scDate = scDateLayout.getEditText();
        scMileage = scMileageLayout.getEditText();
        scNotes = scNotesLayout.getEditText();

        initVehiclePicker();
    }

    private void addTask() {
        int errors = 0;
        Task newTask = new Task();

        String dateValue = scDate.getText().toString().trim();
        String mileageValue = scMileage.getText().toString().trim();

        if (scTaskName.getText().toString().trim().equals("")) {
            scTaskName.setError("Cannot be blank");
            errors++;
        }
        if (scVehiclePicker.getText().toString().trim().equals("")) {
            scVehiclePicker.setError("Cannot be blank");
            errors++;
        }
        // At least one trigger must be set
        if (dateValue.isEmpty() && mileageValue.isEmpty()) {
            scDateLayout.setError("Set a due date, a mileage, or both");
            scMileageLayout.setError("Set a due date, a mileage, or both");
            errors++;
        } else {
            scDateLayout.setError(null);
            scMileageLayout.setError(null);
        }

        if (errors == 0) {
            newTask.setTaskName(scTaskName.getText().toString().trim());
            newTask.setTaskVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection).getVehicleId()));
            if (!dateValue.isEmpty()) newTask.setTaskDueDate(taskDateString);
            if (!mileageValue.isEmpty()) newTask.setTaskDueMileage(mileageValue);
            newTask.setTaskNotes(scNotes.getText().toString().trim());
            newTask.setTaskType("single");
            newTask.setTaskCompleted(false);
            newTask.setTaskId(java.util.UUID.randomUUID().toString());
            newTask.setEntryTime(Calendar.getInstance().getTimeInMillis());

            taskArrayList.add(newTask);
            userRef.child("tasks").setValue(taskArrayList);

            finish();
        }
    }
}