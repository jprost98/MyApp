package com.example.myapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
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

public class AddRecurringCheckup extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;
    private DatabaseReference taskRef;

    // Vehicles
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private VehicleDatabase vehicleDatabase;
    private AutoCompleteTextView rcVehiclePicker;
    private AutoCompleteTextView rcFrequencyPicker;
    private int vehicleSelection;

    // Task
    private final com.example.myapp.data.Task task = new com.example.myapp.data.Task();
    private final ArrayList<Task> taskArrayList = new ArrayList<>();
    private String taskDateString;

    // Layout
    private TextInputLayout rcTaskNameLayout, rcVehicleLayout, rcMileageInputLayout, rcTimeInputLayout, rcNotesLayout, rcDoneBeforeLayout;
    private EditText rcTaskName, rcMileage, rcTime, rcNotes, rcDoneBeforeDate;
    private RadioButton rcMileageRB, rcTimeRB;
    private LinearLayout rcMileageLL, rcTimeLL, rcDoneBeforeLL;
    private MaterialCheckBox rcDoneBeforeBox;

    // Misc
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_recurring_checkup);

        setSupportActionBar(findViewById(R.id.recurring_checkup_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Create Recurring Task");
        }

        initFirebase();
        getVehicles();
        getTasks();
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
                /*
                final Calendar myCalendar = Calendar.getInstance();

                DatePickerDialog.OnDateSetListener datePicker = (dateView, year, monthOfYear, dayOfMonth) -> {
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    taskDateString = sdf.format(myCalendar.getTime());
                    rcDoneBeforeDate.setText(SimpleDateFormat.getDateInstance().format(myCalendar.getTime()));
                };

                if (!rcDoneBeforeDate.getText().toString().isEmpty()) {
                    Date date;
                    try {
                        date = SimpleDateFormat.getDateInstance().parse(rcDoneBeforeDate.getText().toString());
                        assert date != null;
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    myCalendar.setTime(date);
                    new DatePickerDialog(AddRecurringCheckup.this, datePicker, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                } else {
                    new DatePickerDialog(AddRecurringCheckup.this, datePicker, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                }

                 */

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
    }

    private void getVehicles() {
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
    }

    private void getTasks() {
        userRef.child("tasks").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    for (DataSnapshot dataSnapshot : task.getResult().getChildren()) {
                        taskArrayList.add(dataSnapshot.getValue(Task.class));
                    }
                }
            }
        });
    }

    private void initVehiclePicker() {
        int darkMode = sharedPref.getInt("dark_mode", 0);
        ArrayList<String> vehicleOptions = new ArrayList<>();
        for (Vehicle vehicle: vehicleArrayList) {
            vehicleOptions.add(vehicle.vehicleTitle());
        }
        if (darkMode == 0) {
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_light, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            rcVehiclePicker =
                    findViewById(R.id.rc_vehicle_options);
            rcVehiclePicker.setAdapter(stringArrayAdapter);
        } else if (darkMode == 1){
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_dark, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            rcVehiclePicker =
                    findViewById(R.id.rc_vehicle_options);
            rcVehiclePicker.setAdapter(stringArrayAdapter);
        }
        rcVehiclePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                vehicleSelection = i;
            }
        });
    }

    private void initFrequencyPicker() {
        int darkMode = sharedPref.getInt("dark_mode", 0);
        if (darkMode == 0) {
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.time_frequencies));
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            rcFrequencyPicker =
                    findViewById(R.id.rc_time_frequency_options);
            rcFrequencyPicker.setAdapter(stringArrayAdapter);
        } else if (darkMode == 1){
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.time_frequencies));
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            rcFrequencyPicker =
                    findViewById(R.id.rc_time_frequency_options);
            rcFrequencyPicker.setAdapter(stringArrayAdapter);
        }
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

        initVehiclePicker();
        initFrequencyPicker();
    }

    private void addTask() {
        String frequency = null;
        int errors = 0;

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
            task.setTaskName(rcTaskName.getText().toString().trim());
            task.setTaskVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection).getVehicleId()));
            task.setTaskFrequency(frequency);
            task.setTaskNotes(rcNotes.getText().toString().trim());
            if (!rcDoneBeforeDate.getText().toString().isEmpty()) task.setTaskLastDone(taskDateString);
            task.setTaskType("recurring");
            task.setEntryTime(Calendar.getInstance().getTimeInMillis());

            taskArrayList.add(task);
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