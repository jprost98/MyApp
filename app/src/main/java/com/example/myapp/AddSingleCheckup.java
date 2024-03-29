package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
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

public class AddSingleCheckup extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;

    // Vehicles
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private VehicleDatabase vehicleDatabase;
    private AutoCompleteTextView scVehiclePicker;
    private int vehicleSelection;

    // Task
    private final com.example.myapp.data.Task task = new com.example.myapp.data.Task();
    private final ArrayList<Task> taskArrayList = new ArrayList<>();
    private String taskDateString;

    // Layout
    private TextInputLayout scTaskNameLayout, scVehicleLayout, scDateLayout, scNotesLayout;
    private EditText scTaskName, scDate, scNotes;

    // Misc
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_single_checkup);

        setSupportActionBar(findViewById(R.id.single_checkup_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Create One-Time Task");
        }

        initFirebase();
        getVehicles();
        getTasks();
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
                final Calendar myCalendar = Calendar.getInstance();

                DatePickerDialog.OnDateSetListener datePicker = (dateView, year, monthOfYear, dayOfMonth) -> {
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    taskDateString = sdf.format(myCalendar.getTime());
                    scDate.setText(SimpleDateFormat.getDateInstance().format(myCalendar.getTime()));
                };

                if (!scDate.getText().toString().isEmpty()) {
                    Date date;
                    try {
                        date = SimpleDateFormat.getDateInstance().parse(scDate.getText().toString());
                        assert date != null;
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    myCalendar.setTime(date);
                    new DatePickerDialog(AddSingleCheckup.this, datePicker, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                } else {
                    new DatePickerDialog(AddSingleCheckup.this, datePicker, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                }

                /*
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

                 */
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
            scVehiclePicker =
                    findViewById(R.id.sc_vehicle_options);
            scVehiclePicker.setAdapter(stringArrayAdapter);
        } else if (darkMode == 1){
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_dark, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            scVehiclePicker =
                    findViewById(R.id.sc_vehicle_options);
            scVehiclePicker.setAdapter(stringArrayAdapter);
        }
        scVehiclePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                vehicleSelection = i;
            }
        });
    }

    private void initVars() {
        scTaskNameLayout = findViewById(R.id.sc_task_name);
        scVehicleLayout = findViewById(R.id.sc_vehicle_picker);
        scDateLayout = findViewById(R.id.sc_date);
        scNotesLayout = findViewById(R.id.sc_notes);

        scTaskName = scTaskNameLayout.getEditText();
        scDate = scDateLayout.getEditText();
        scNotes = scNotesLayout.getEditText();

        initVehiclePicker();
    }

    private void addTask() {
        int errors = 0;
        if (scTaskName.getText().toString().trim().equals("")) {
            scTaskName.setError("Cannot be blank");
            errors++;
        }
        if (scVehiclePicker.getText().toString().trim().equals("")) {
            scVehiclePicker.setError("Cannot be blank");
        }
        if (scDate.getText().toString().trim().equals("")) {
            scDate.setError("Cannot be blank");
            errors++;
        }
        if (errors == 0) {
            task.setTaskName(scTaskName.getText().toString().trim());
            task.setTaskVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection).getVehicleId()));
            task.setTaskDueDate(taskDateString);
            task.setTaskNotes(scNotes.getText().toString().trim());
            task.setTaskType("single");
            task.setEntryTime(Calendar.getInstance().getTimeInMillis());

            taskArrayList.add(task);
            userRef.child("tasks").setValue(taskArrayList);

            finish();
        }
    }
}