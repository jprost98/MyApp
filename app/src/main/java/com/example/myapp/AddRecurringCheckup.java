package com.example.myapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.example.myapp.utils.TaskUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.checkbox.MaterialCheckBox;
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

public class AddRecurringCheckup extends BaseActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;

    // Vehicles
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private AutoCompleteTextView rcVehiclePicker;
    private AutoCompleteTextView rcFrequencyPicker;
    private int vehicleSelection;

    // Task
    private final ArrayList<Task> taskArrayList = new ArrayList<>();
    private String taskDateString;

    // Layout
    private TextInputLayout rcTaskNameLayout, rcVehicleLayout, rcMileageInputLayout, rcTimeInputLayout, rcNotesLayout, rcDoneBeforeLayout;
    private EditText rcTaskName, rcMileage, rcTime, rcNotes, rcDoneBeforeDate;
    private RadioButton rcMileageRB, rcTimeRB;
    private LinearLayout rcMileageLL, rcTimeLL, rcDoneBeforeLL;
    private MaterialCheckBox rcDoneBeforeBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_recurring_checkup);

        setSupportActionBar(findViewById(R.id.recurring_checkup_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Create Recurring Task");
        }

        initFirebase();
        initVars();

        Button finishBtn = findViewById(R.id.rc_finish_btn);
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTask();
            }
        });

        rcMileageRB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rcMileageRB.setChecked(true);
                rcMileageLL.setVisibility(View.VISIBLE);
                rcTimeRB.setChecked(false);
                rcTimeLL.setVisibility(View.GONE);
            }
        });
        rcTimeRB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rcTimeRB.setChecked(true);
                rcTimeLL.setVisibility(View.VISIBLE);
                rcMileageRB.setChecked(false);
                rcMileageLL.setVisibility(View.GONE);
            }
        });
        rcDoneBeforeBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (rcDoneBeforeBox.isChecked()) {
                    Log.d("Box Checked", "True");
                    rcDoneBeforeLL.setVisibility(View.VISIBLE);
                }
                else {
                    Log.d("Box Checked", "False");
                    rcDoneBeforeLL.setVisibility(View.GONE);
                }
            }
        });
        rcDoneBeforeDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long date = 0;
                if (!rcDoneBeforeDate.getText().toString().isEmpty()) {
                    try {
                        date = Objects.requireNonNull(SimpleDateFormat.getDateInstance().parse(rcDoneBeforeDate.getText().toString())).getTime();
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
                        rcDoneBeforeDate.setText(SimpleDateFormat.getDateInstance().format(date));
                    }
                });
                materialDatePicker.show(getSupportFragmentManager(), "date");
            }
        });
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
        rcVehiclePicker = findViewById(R.id.rc_vehicle_options);
        rcVehiclePicker.setAdapter(adapter);
        rcVehiclePicker.setOnItemClickListener((adapterView, view, i, l) -> vehicleSelection = i);
    }

    private void initFrequencyPicker() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.time_frequencies));
        rcFrequencyPicker = findViewById(R.id.rc_time_frequency_options);
        rcFrequencyPicker.setAdapter(adapter);
    }

    private void initVars() {
        rcMileageLL = findViewById(R.id.rc_mileage_layout);
        rcTimeLL = findViewById(R.id.rc_time_layout);
        rcDoneBeforeLL = findViewById(R.id.rc_done_before_layout);
        rcDoneBeforeBox = findViewById(R.id.rc_done_before_box);

        rcTaskNameLayout = findViewById(R.id.rc_task_name);
        rcVehicleLayout = findViewById(R.id.rc_vehicle_picker);
        rcMileageInputLayout = findViewById(R.id.rc_mileage_input);
        rcTimeInputLayout = findViewById(R.id.rc_time_input);
        rcNotesLayout = findViewById(R.id.rc_notes);
        rcDoneBeforeLayout = findViewById(R.id.rc_done_before_date);

        rcTaskName = rcTaskNameLayout.getEditText();
        rcMileage = rcMileageInputLayout.getEditText();
        rcTime = rcTimeInputLayout.getEditText();
        rcNotes = rcNotesLayout.getEditText();
        rcDoneBeforeDate = rcDoneBeforeLayout.getEditText();

        rcMileageRB = findViewById(R.id.rc_mileage_rb);
        rcMileageRB.setChecked(true);
        rcMileageLL.setVisibility(View.VISIBLE);
        rcTimeRB = findViewById(R.id.rc_time_rb);
        rcTimeRB.setChecked(false);
        rcTimeLL.setVisibility(View.GONE);

        // initVehiclePicker() is called after Firebase loads vehicles
        initFrequencyPicker();
    }

    private void addTask() {
        String frequency = null;
        int errors = 0;
        Task newTask = new Task();

        if (rcMileageRB.isChecked()) {
            frequency = rcMileage.getText().toString().trim() + " miles";
            if (rcMileage.getText().toString().trim().equals("")) {
                rcMileage.setError("Cannot be blank");
                errors++;
            }
        } else if (rcTimeRB.isChecked()) {
            frequency = rcTime.getText().toString().trim() + " " + rcFrequencyPicker.getText().toString().trim();
            if (rcTime.getText().toString().trim().equals("")) {
                rcTime.setError("Cannot be blank");
                errors++;
            }
            if (rcFrequencyPicker.getText().toString().trim().equals("")) {
                rcFrequencyPicker.setError("Cannot be blank");
                errors++;
            }
        }
        if (rcTaskName.getText().toString().trim().equals("")) {
            rcTaskName.setError("Cannot be blank");
            errors++;
        }
        if (rcVehiclePicker.getText().toString().trim().equals("")) {
            rcVehiclePicker.setError("Cannot be blank");
        }
        if (errors == 0) {
            newTask.setTaskName(rcTaskName.getText().toString().trim());
            newTask.setTaskVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection).getVehicleId()));
            newTask.setTaskFrequency(frequency);
            newTask.setTaskNotes(rcNotes.getText().toString().trim());
            if (!rcDoneBeforeDate.getText().toString().isEmpty()) {
                newTask.setTaskLastDone(taskDateString);
                String nextDueDate = TaskUtils.INSTANCE.calculateNextDueDate(taskDateString, frequency);
                newTask.setTaskDueDate(nextDueDate);
            }
            newTask.setTaskType("recurring");
            newTask.setTaskId(java.util.UUID.randomUUID().toString());
            newTask.setEntryTime(Calendar.getInstance().getTimeInMillis());

            taskArrayList.add(newTask);
            userRef.child("tasks").setValue(taskArrayList);

            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}