package com.example.myapp;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDao;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class AddRecord extends AppCompatActivity {

    private int darkMode;
    private final Record record = new Record();
    private final Vehicle vehicle = new Vehicle();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private final ArrayList<String> vehicleOptions = new ArrayList<>();
    private String recordDateString;
    private EditText recordTitle, recordNotes, recordDate, recordOdometer;
    private TextInputLayout recordTitleLayout, recordDateLayout, recordVehicleLayout, recordOdometerLayout, recordNotesLayout;
    private RecordDatabase recordDatabase;
    private RecordDao recordDao;
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("users");
    private DatabaseReference ranking;
    private SharedPreferences sharedPref;
    private AutoCompleteTextView recordVehiclePicker;
    private int vehicleSelection;

    private static final String CHANNEL_ID = "Achievement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_record);

        setSupportActionBar(findViewById(R.id.add_record_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Add Record");
        }

        initFirebase();
        getVehicles();
        initVehiclePicker();
        initVars();

        Button addRecordButton = findViewById(R.id.add_record_btn);
        addRecordButton.setOnClickListener(view -> {
            addRecord();
            //startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        recordDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar myCalendar = Calendar.getInstance();

                DatePickerDialog.OnDateSetListener datePicker = (dateView, year, monthOfYear, dayOfMonth) -> {
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    recordDateString = sdf.format(myCalendar.getTime());
                    recordDate.setText(SimpleDateFormat.getDateInstance().format(myCalendar.getTime()));
                };

                if (!recordDate.getText().toString().isEmpty()) {
                    Date date;
                    try {
                        date = SimpleDateFormat.getDateInstance().parse(recordDate.getText().toString());
                        assert date != null;
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    myCalendar.setTime(date);
                    new DatePickerDialog(AddRecord.this, datePicker, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                } else {
                    new DatePickerDialog(AddRecord.this, datePicker, myCalendar
                            .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                            myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                }

                /*
                long date = 0;
                if (!recordDate.getText().toString().isEmpty()) {
                    try {
                        date = Objects.requireNonNull(SimpleDateFormat.getDateInstance().parse(recordDate.getText().toString())).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else {
                    date = MaterialDatePicker.todayInUtcMilliseconds();
                }
                MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                     .setTitleText("Date of Work")
                     .setSelection(date)
                     .build();
                materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                 @Override
                 public void onPositiveButtonClick(Long selection) {
                     TimeZone timeZoneUTC = TimeZone.getDefault();
                     int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
                     SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                     Date date = new Date(selection + offsetFromUTC);
                     recordDateString = simpleFormat.format(date);
                     recordDate.setText(SimpleDateFormat.getDateInstance().format(date));
                 }
                });
                materialDatePicker.show(getSupportFragmentManager(), "date");

                 */
            }
        });
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        assert mUser != null;
        ranking = database.getReference("users/" + mUser.getUid() + "/ranking");
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

        recordTitleLayout = findViewById(R.id.record_title_input);
        recordDateLayout = findViewById(R.id.record_date_input);
        recordVehicleLayout = findViewById(R.id.record_vehicle_picker);
        recordOdometerLayout = findViewById(R.id.record_odometer_input);
        recordNotesLayout = findViewById(R.id.record_notes_input);

        recordTitle = recordTitleLayout.getEditText();
        recordDate = recordDateLayout.getEditText();
        recordOdometer = recordOdometerLayout.getEditText();
        recordNotes = recordNotesLayout.getEditText();
        recordArrayList.clear();
    }

    private void initVehiclePicker() {
        int darkMode = sharedPref.getInt("dark_mode", 0);
        for (Vehicle vehicle: vehicleArrayList) {
            vehicleOptions.add(vehicle.vehicleTitle());
        }
        if (darkMode == 0) {
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_light, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            recordVehiclePicker = findViewById(R.id.record_vehicle_options);
            recordVehiclePicker.setAdapter(stringArrayAdapter);
        } else if (darkMode == 1){
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_dark, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            recordVehiclePicker = findViewById(R.id.record_vehicle_options);
            recordVehiclePicker.setAdapter(stringArrayAdapter);
        }
        recordVehiclePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                vehicleSelection = i;
            }
        });
    }

    private void addRecord() {
        int errors = checkRecordReqs();
        if (errors == 0) {
            recordArrayList.addAll(recordDao.getAllRecords());
            record.setTitle(recordTitle.getText().toString().trim());
            record.setDate(recordDateString);
            record.setVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection).getVehicleId()));
            record.setOdometer(recordOdometer.getText().toString().trim());
            record.setDescription(recordNotes.getText().toString().trim());
            record.setEntryTime(Calendar.getInstance().getTimeInMillis());

            recordDao.addRecord(record);
            recordArrayList.clear();
            recordArrayList.addAll(recordDao.getAllRecords());
            userRef.child(mUser.getUid()).child("records").setValue(recordArrayList);
            checkAchievements();
        }
    }

    private void checkAchievements() {
        createNotificationChannel();
        userRef.child(mUser.getUid()).child("achievements").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().child("first_oil_change").getValue() == null) {
                        if (recordTitle.getText().toString().trim().toLowerCase(Locale.ROOT).equals("oil change")) {
                            userRef.child(mUser.getUid()).child("achievements").child("first_oil_change").setValue("true");
                            createNotification("Achievement Unlocked!", "You completed your first oil change!", 1);
                        }
                    }
                    if (task.getResult().child("high_mileage").getValue() == null) {
                        if (Integer.parseInt(recordOdometer.getText().toString().trim()) >= 100000) {
                            userRef.child(mUser.getUid()).child("achievements").child("high_mileage").setValue("true");
                            createNotification("Achievement Unlocked!", "Your vehicle is starting to get some high miles on it!", 2);
                        }
                    }
                    if (task.getResult().child("very_high_mileage").getValue() == null) {
                        if (Integer.parseInt(recordOdometer.getText().toString().trim()) >= 200000) {
                            userRef.child(mUser.getUid()).child("achievements").child("very_high_mileage").setValue("true");
                            createNotification("Achievement Unlocked!", "Your vehicle has some serious miles on it!", 3);
                        }
                    }
                    if (task.getResult().child("extremely_high_mileage").getValue() == null) {
                        if (Integer.parseInt(recordOdometer.getText().toString().trim()) >= 300000) {
                            userRef.child(mUser.getUid()).child("achievements").child("extremely_high_mileage").setValue("true");
                            createNotification("Achievement Unlocked!", "It might be time to start looking for a new vehicle!", 4);
                        }
                    }
                }
            }
        });
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
        if (recordVehiclePicker.getText().toString().trim().isEmpty()) {
            recordVehiclePicker.setError("Pick a vehicle for the record");
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
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void createNotification(String title, String description, int id) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_achievement_unlocked)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }
}