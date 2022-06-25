package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.constraintlayout.widget.ConstraintLayout;
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
    private LinearLayout normalView;
    private ActionBar actionBar;
    private SharedPreferences sharedPref;
    private ConstraintLayout loadingView;
    private EditText userEmailInput, userPasswordInput;
    private Button dummy_login_button, login_user_button, register_user_button, dummy_fp_button, forgot_password_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        int darkMode = sharedPref.getInt("dark_mode", 0);
        int themePref = sharedPref.getInt("theme_pref", 0);
        if (themePref == 0) this.setTheme(R.style.DefaultTheme);
        else if (themePref == 1) this.setTheme(R.style.RedTheme);
        else if (themePref == 2) this.setTheme(R.style.BlueTheme);
        else if (themePref == 3) this.setTheme(R.style.GreenTheme);
        else if (themePref == 4) this.setTheme(R.style.GreyscaleTheme);
        Log.d("Theme", String.valueOf(themePref));
        setTitle("Login");
        if (darkMode == 0) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        setContentView(R.layout.activity_login);
        actionBar = getSupportActionBar();

        normalView = findViewById(R.id.normal_login_view);
        loadingView = findViewById(R.id.loading_user_view);
        dummy_login_button = findViewById(R.id.dummy_login_btn);
        login_user_button = findViewById(R.id.login_btn);
        register_user_button = findViewById(R.id.register_btn);
        dummy_fp_button = findViewById(R.id.dummy_fp_btn);
        forgot_password_button = findViewById(R.id.forgot_password_btn);
        userEmailInput = findViewById(R.id.login_email_input);
        userPasswordInput = findViewById(R.id.login_password_input);

        normalView.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        userEmailInput.setVisibility(View.GONE);
        userPasswordInput.setVisibility(View.GONE);
        dummy_login_button.setVisibility(View.VISIBLE);
        login_user_button.setVisibility(View.GONE);
        register_user_button.setVisibility(View.VISIBLE);
        dummy_fp_button.setVisibility(View.VISIBLE);
        forgot_password_button.setVisibility(View.GONE);

        sharedPref.edit().putString("filter_by_value", "All").apply();
        filterValue = sharedPref.getString("filter_by_value", "All");
        Log.d("Filter Value", filterValue);

        initFirebase();

        dummy_login_button.setOnClickListener(view -> {
            dummy_fp_button.setVisibility(View.GONE);
            register_user_button.setVisibility(View.GONE);
            dummy_login_button.setVisibility(View.GONE);
            userEmailInput.setVisibility(View.VISIBLE);
            userPasswordInput.setVisibility(View.VISIBLE);
            login_user_button.setVisibility(View.VISIBLE);
        });
        login_user_button.setOnClickListener(v -> {
            loginUser();
        });
        register_user_button.setOnClickListener(v -> {
            registerUser();
        });
        dummy_fp_button.setOnClickListener(view -> {
            dummy_login_button.setVisibility(View.GONE);
            register_user_button.setVisibility(View.GONE);
            dummy_fp_button.setVisibility(View.GONE);
            userEmailInput.setVisibility(View.VISIBLE);
            forgot_password_button.setVisibility(View.VISIBLE);
        });
        forgot_password_button.setOnClickListener(view -> {
            forgotPassword();
        });
    }

    @Override
    public void onBackPressed() {
        recreate();
    }

    private void forgotPassword() {
        String userEmail;
        userEmailInput = findViewById(R.id.login_email_input);
        userEmail = userEmailInput.getText().toString().trim();

        if (userEmail.isEmpty()) {
            userEmailInput.setError("Please enter a valid email address");
        } else {
            mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(LoginActivity.this, "A reset link has been sent to the email you provided", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
        } else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            AlertDialog dialog;
            @SuppressLint("InflateParams") final View compareDatabases = getLayoutInflater().inflate(R.layout.popup_compare_databases, null);
            TextView recordPopupText = compareDatabases.findViewById(R.id.popup_record_text);
            TextView vehiclePopupText = compareDatabases.findViewById(R.id.popup_vehicle_text);
            TextView recordIssueTxt = compareDatabases.findViewById(R.id.record_issue_txt);
            TextView vehicleIssueTxt = compareDatabases.findViewById(R.id.vehicle_issue_txt);
            recordPopupText.setVisibility(View.GONE);
            vehiclePopupText.setVisibility(View.GONE);
            recordIssueTxt.setVisibility(View.GONE);
            vehicleIssueTxt.setVisibility(View.GONE);
            Button popupLocalBtn = compareDatabases.findViewById(R.id.popup_local_btn);
            Button popupRemoteBtn = compareDatabases.findViewById(R.id.popup_remote_btn);
            Button popupLogOutBtn = compareDatabases.findViewById(R.id.popout_logout_btn);

            if (localRecordList.size() == 0 & remoteRecordList.size() > 0) {
                recordPopupText.setVisibility(View.VISIBLE);
                recordIssueTxt.setVisibility(View.VISIBLE);
                recordPopupText.setText("There are no local records saved, but there are remote records saved.");
            }
            if (localVehicleList.size() == 0 & remoteVehicleList.size() > 0) {
                vehiclePopupText.setVisibility(View.VISIBLE);
                vehicleIssueTxt.setVisibility(View.VISIBLE);
                vehiclePopupText.setText("There are no local vehicles saved, but there are remote vehicles saved.");
            }
            if (remoteRecordList.size() == 0 & localRecordList.size() > 0) {
                recordPopupText.setVisibility(View.VISIBLE);
                recordIssueTxt.setVisibility(View.VISIBLE);
                recordPopupText.setText("There are local records saved, but there are no remote records saved.");
            }
            if (remoteVehicleList.size() == 0 & localVehicleList.size() > 0) {
                vehiclePopupText.setVisibility(View.VISIBLE);
                vehicleIssueTxt.setVisibility(View.VISIBLE);
                vehiclePopupText.setText("There are local vehicles saved, but there are no remote vehicles saved.");
            }
            if (localRecordList.size() != 0 & remoteRecordList.size() != 0) {
                if (localRecordList.get(localRecordList.size() - 1).getEntryTime() > remoteRecordList.get(remoteRecordList.size() - 1).getEntryTime()) {
                    recordPopupText.setVisibility(View.VISIBLE);
                    recordIssueTxt.setVisibility(View.VISIBLE);
                    recordPopupText.setText("The local record data is newer than the remote record data.");
                } else if (localRecordList.get(localRecordList.size() - 1).getEntryTime() < remoteRecordList.get(remoteRecordList.size() - 1).getEntryTime()) {
                    recordPopupText.setVisibility(View.VISIBLE);
                    recordIssueTxt.setVisibility(View.VISIBLE);
                    recordPopupText.setText("The remote record data is newer than the local record data.");
                }
            }
            if (localVehicleList.size() != 0 & remoteVehicleList.size() != 0) {
                if (localVehicleList.get(localVehicleList.size() - 1).getEntryTime() > remoteVehicleList.get(remoteVehicleList.size() - 1).getEntryTime()) {
                    vehiclePopupText.setVisibility(View.VISIBLE);
                    vehicleIssueTxt.setVisibility(View.VISIBLE);
                    vehiclePopupText.setText("The local vehicle data is newer than the remote vehicle data.");
                } else if (localVehicleList.get(localVehicleList.size() - 1).getEntryTime() < remoteVehicleList.get(remoteVehicleList.size() - 1).getEntryTime()) {
                    vehiclePopupText.setVisibility(View.VISIBLE);
                    vehicleIssueTxt.setVisibility(View.VISIBLE);
                    vehiclePopupText.setText("The remote vehicle data is newer than the local vehicle data.");
                };
            }

            dialogBuilder.setView(compareDatabases);
            dialog = dialogBuilder.create();
            dialog.show();
            popupLocalBtn.setOnClickListener(view -> {
                dialog.dismiss();
                userRef.child(mUser.getUid()).child("Records").setValue(localRecordList);
                userRef.child(mUser.getUid()).child("Vehicles").setValue(localVehicleList);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
            popupRemoteBtn.setOnClickListener(view -> {
                dialog.dismiss();
                recordDao.deleteAllRecords();
                vehicleDao.deleteAllVehicles();
                for (Record remoteRecord:remoteRecordList) {
                    recordDao.addRecord(remoteRecord);
                }
                for (Vehicle remoteVehicle:remoteVehicleList) {
                    vehicleDao.addVehicle(remoteVehicle);
                }
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
            popupLogOutBtn.setOnClickListener(view -> {
                mAuth.signOut();
                startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    private void loadData() {
        Toast.makeText(this, mUser.getEmail() + " is logged in", Toast.LENGTH_SHORT).show();
        actionBar.hide();
        normalView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        localVehicleList.clear();
        localRecordList.clear();
        remoteVehicleList.clear();
        remoteRecordList.clear();
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        localVehicleList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        localRecordList.addAll(recordDatabase.recordDao().getAllRecords());
        userRef.child(mUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                for (DataSnapshot dataSnapshot : task.getResult().child("Vehicles").getChildren()) {
                    remoteVehicleList.add(dataSnapshot.getValue(Vehicle.class));
                }
                for (DataSnapshot dataSnapshot : task.getResult().child("Records").getChildren()) {
                    remoteRecordList.add(dataSnapshot.getValue(Record.class));
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        compareDatabases();
                    }
                }, 1500);
            }
        });
    }

    //Initializes Firebase Authentication
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser!=null) {
            userRef.child(mUser.getUid()).child("Settings").child("Dark Mode").setValue(sharedPref.getInt("dark_mode", 0));
            userRef.child(mUser.getUid()).child("Settings").child("Theme").setValue(sharedPref.getInt("theme_pref", 0));
            loadData();
        }
    }

    //Log in user
    private void loginUser() {
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
                            mUser = mAuth.getCurrentUser();
                            loadData();
                        } else Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    //Takes user to registration page
    private void registerUser() {
        startActivity(new Intent(this, RegistrationActivity.class));
    }
}