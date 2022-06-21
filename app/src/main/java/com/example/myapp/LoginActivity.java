package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDao;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private ArrayList<Vehicle> localVehicleList = new ArrayList<>();
    private ArrayList<Record> localRecordList = new ArrayList<>();
    private ArrayList<Vehicle> remoteVehicleList = new ArrayList<>();
    private ArrayList<Record> remoteRecordList = new ArrayList<>();
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private RecordDatabase recordDatabase;
    private RecordDao recordDao;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private String filterValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        int themePref = sharedPref.getInt("theme_pref_value", 0);
        Log.d("Theme Pref", String.valueOf(themePref));
        if (themePref == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Log.d("Theme", "Light Theme");
        } else if (themePref == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Log.d("Theme", "Dark Theme");
        }
        setTitle("Login");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();

        LinearLayout normalView, loadingView;
        normalView = findViewById(R.id.normal_login_view);
        loadingView = findViewById(R.id.loading_user_view);
        normalView.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);

        sharedPref.edit().putString("filter_by_value", "All").apply();
        filterValue = sharedPref.getString("filter_by_value", "All");
        Log.d("Filter Value", filterValue);

        initFirebase();
        if (mUser!=null) {
            assert actionBar != null;
            actionBar.hide();
            normalView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
            loadData();
        }

        Button login_user_button = findViewById(R.id.login_btn);
        login_user_button.setOnClickListener(v -> {
            loginUser();
        });

        Button register_user_button = findViewById(R.id.register_btn);
        register_user_button.setOnClickListener(v -> {
            registerUser();
        });
    }

    //Check if user is currently logged in
    @Override
    public void onStart() {
        super.onStart();
    }

    @SuppressLint("SetTextI18n")
    private void compareDatabases() {
        Log.d("Local Record Data", localRecordList.toString());
        Log.d("Remote Record Data", remoteRecordList.toString());
        Log.d("Local Vehicle Data", localVehicleList.toString());
        Log.d("Remote Vehicle Data", remoteVehicleList.toString());

        vehicleDao = vehicleDatabase.vehicleDao();
        recordDao = recordDatabase.recordDao();

        if (localRecordList.toString().equals(remoteRecordList.toString()) && localVehicleList.toString().equals(remoteVehicleList.toString())) {
            Log.d("Equal Databases", "Local and Remote Databases are synced");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            Toast.makeText(this, mUser.getEmail() + " is logged in", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            AlertDialog dialog;
            @SuppressLint("InflateParams") final View compareDatabases = getLayoutInflater().inflate(R.layout.popup_compare_databases, null);
            TextView popupText = compareDatabases.findViewById(R.id.popup_text);
            Button popupLocalBtn = compareDatabases.findViewById(R.id.popup_local_btn);
            Button popupRemoteBtn = compareDatabases.findViewById(R.id.popup_remote_btn);

            if ((localRecordList.size() == 0 & remoteRecordList.size() > 0) || (localVehicleList.size() == 0 & remoteVehicleList.size() > 0)) {
                popupText.setText("There is no local data saved, but there is remote data saved.");
            } else if (localRecordList.size() != 0 & remoteRecordList.size() != 0 & localVehicleList.size() != 0 & remoteVehicleList.size() != 0) {
                if (localRecordList.get(localRecordList.size() - 1).getEntryTime() > remoteRecordList.get(remoteRecordList.size() - 1).getEntryTime()) {
                    Log.d("Newer Record Database", "Local Record Database");
                    popupText.setText("The local record data is newer than the remote record data.");
                } else if (localRecordList.get(localRecordList.size() - 1).getEntryTime() < remoteRecordList.get(remoteRecordList.size() - 1).getEntryTime()) {
                    Log.d("Newer Record Database", "Remote Record Database");
                    popupText.setText("The remote record data is newer than the local record data.");
                };
                if (localVehicleList.get(localVehicleList.size() - 1).getEntryTime() > remoteVehicleList.get(remoteVehicleList.size() - 1).getEntryTime()) {
                    Log.d("Newer Vehicle Database", "Local Vehicle Database");
                    popupText.setText("The local vehicle data is newer than the remote vehicle data.");
                } else if (localVehicleList.get(localVehicleList.size() - 1).getEntryTime() < remoteVehicleList.get(remoteVehicleList.size() - 1).getEntryTime()) {
                    Log.d("Newer Vehicle Database", "Remote Vehicle Database");
                    popupText.setText("The remote vehicle data is newer than the local vehicle data.");
                };
            }

            dialogBuilder.setView(compareDatabases);
            dialog = dialogBuilder.create();
            dialog.show();
            popupLocalBtn.setOnClickListener(view -> {
                userRef.child(mUser.getDisplayName()).child("Records").setValue(localRecordList);
                userRef.child(mUser.getDisplayName()).child("Vehicles").setValue(localVehicleList);
                dialog.dismiss();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                Toast.makeText(this, mUser.getEmail() + " is logged in", Toast.LENGTH_SHORT).show();
            });
            popupRemoteBtn.setOnClickListener(view -> {
                recordDao.deleteAllRecords();
                vehicleDao.deleteAllVehicles();
                for (Record remoteRecord:remoteRecordList) {
                    recordDao.addRecord(remoteRecord);
                }
                for (Vehicle remoteVehicle:remoteVehicleList) {
                    vehicleDao.addVehicle(remoteVehicle);
                }
                Log.d("Local Record Data", localRecordList.toString());
                Log.d("Remote Record Data", remoteRecordList.toString());
                Log.d("Local Vehicle Data", localVehicleList.toString());
                Log.d("Remote Vehicle Data", remoteVehicleList.toString());
                dialog.dismiss();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                Toast.makeText(this, mUser.getEmail() + " is logged in", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadData() {
        localVehicleList.clear();
        localRecordList.clear();
        remoteVehicleList.clear();
        remoteRecordList.clear();
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        localVehicleList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        localRecordList.addAll(recordDatabase.recordDao().getAllRecords());
        userRef.child(mUser.getDisplayName()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                for (DataSnapshot dataSnapshot : task.getResult().child("Vehicles").getChildren()) {
                    remoteVehicleList.add(dataSnapshot.getValue(Vehicle.class));
                }
                for (DataSnapshot dataSnapshot : task.getResult().child("Records").getChildren()) {
                    remoteRecordList.add(dataSnapshot.getValue(Record.class));
                }
                compareDatabases();
            }
        });
    }

    //Initializes Firebase Authentication
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    //Log in user
    private void loginUser() {
        EditText userEmailInput, userPasswordInput;
        String userEmail, userPassword;
        userEmailInput = findViewById(R.id.login_email_input);
        userPasswordInput = findViewById(R.id.login_password_input);
        userEmail = userEmailInput.getText().toString().trim();
        userPassword = userPasswordInput.getText().toString().trim();
        int errors = 0;

        if (userEmail.isEmpty()) {
            userEmailInput.setError("Please enter a valid email address");
            errors++;
        }
        if (userPassword.isEmpty()) {
            userPasswordInput.setError("Please enter a valid password");
            errors++;
        }
        if (errors == 0) {
            mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    //Takes user to registration page
    private void registerUser() {
        startActivity(new Intent(this, RegistrationActivity.class));
    }
}