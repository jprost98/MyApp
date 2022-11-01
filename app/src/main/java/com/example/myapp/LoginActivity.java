package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.example.myapp.data.User;
import com.example.myapp.data.UserDao;
import com.example.myapp.data.UserDatabase;
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

    private FirebaseDatabase database;
    private DatabaseReference userRef;
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
    private UserDatabase userDatabase;
    private UserDao userDao;
    private User user = new User();
    private ArrayList<User> users = new ArrayList<>();
    private String filterValue;
    private ActionBar actionBar;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private LinearLayout loadingView;
    private LinearLayout normalView;
    private EditText userEmailInput, userPasswordInput;
    private TextView loadingText;
    private Button dummy_login_button, login_user_button, register_user_button, dummy_fp_button, forgot_password_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        editor.putString("filter_by_value", "All");
        //editor.putString("sort_records", "Date_Desc");
        //editor.putString("sort_vehicles", "Year_Desc");
        //editor.apply();
        int darkMode = sharedPref.getInt("dark_mode", 0);
        setTitle("Login");
        if (darkMode == 0) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        loadingText = findViewById(R.id.loading_text);

        normalView.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        userEmailInput.setVisibility(View.GONE);
        userPasswordInput.setVisibility(View.GONE);
        dummy_login_button.setVisibility(View.VISIBLE);
        login_user_button.setVisibility(View.GONE);
        register_user_button.setVisibility(View.VISIBLE);
        dummy_fp_button.setVisibility(View.VISIBLE);
        forgot_password_button.setVisibility(View.GONE);

        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        userDatabase = Room.databaseBuilder(getApplicationContext(), UserDatabase.class, "users").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        users.clear();
        users.addAll(userDatabase.userDao().getUser());

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
                    userEmailInput.setText("");
                    Toast.makeText(LoginActivity.this, "A reset link has been sent to the email you provided (Check Spam)", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void continueToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @SuppressLint("SetTextI18n")
    private void compareDatabases() {
        /*
        Log.d("LRecord Data", localRecordList.toString());
        Log.d("LRecord Data Size", String.valueOf(localRecordList.size()));
        Log.d("RRecord Data", remoteRecordList.toString());
        Log.d("RRecord Data Size", String.valueOf(remoteRecordList.size()));
        Log.d("LVehicle Data", localVehicleList.toString());
        Log.d("LVehicle Data Size", String.valueOf(localVehicleList.size()));
        Log.d("RVehicle Data", remoteVehicleList.toString());
        Log.d("RVehicle Data Size", String.valueOf(remoteVehicleList.size()));

        vehicleDao = vehicleDatabase.vehicleDao();
        recordDao = recordDatabase.recordDao();

        int errors = 0;
        int recordErrors = 0;
        int vehicleErrors = 0;

        if (!localRecordList.toString().equals(remoteRecordList.toString())) {
            errors++;
            recordErrors++;
        }
        if (!localVehicleList.toString().equals(remoteVehicleList.toString())) {
            errors++;
            vehicleErrors++;
        }
        if (localRecordList.size() == 0 & remoteRecordList.size() > 0) {
            recordDao.deleteAllRecords();
            for (Record remoteRecord:remoteRecordList) {
                recordDao.addRecord(remoteRecord);
            }
        }
        if (localVehicleList.size() == 0 & remoteVehicleList.size() > 0) {
            vehicleDao.deleteAllVehicles();
            for (Vehicle remoteVehicle:remoteVehicleList) {
                vehicleDao.addVehicle(remoteVehicle);
            }
        }
        if (remoteRecordList.size() == 0 & localRecordList.size() > 0) {
            userRef.child(mUser.getUid()).child("Records").removeValue();
            userRef.child(mUser.getUid()).child("Records").setValue(localRecordList);
        }
        if (remoteVehicleList.size() == 0 & localRecordList.size() > 0) {
            userRef.child(mUser.getUid()).child("Vehicles").removeValue();
            userRef.child(mUser.getUid()).child("Vehicles").setValue(localVehicleList);
        }

        if (errors != 0) {
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

            if (recordErrors != 0) {
                Long localEntryTime = 0L;
                Long remoteEntryTime = 0L;
                for (Record record : localRecordList) {
                    if (record.getEntryTime() > localEntryTime) localEntryTime = record.getEntryTime();
                }
                for (Record record : remoteRecordList) {
                    if (record.getEntryTime() > remoteEntryTime) remoteEntryTime = record.getEntryTime();
                }
                if (localEntryTime > remoteEntryTime) {
                    recordPopupText.setVisibility(View.VISIBLE);
                    recordIssueTxt.setVisibility(View.VISIBLE);
                    recordPopupText.setText("The local records are newer than the cloud records.");
                } else if (remoteEntryTime > localEntryTime) {
                    recordPopupText.setVisibility(View.VISIBLE);
                    recordIssueTxt.setVisibility(View.VISIBLE);
                    recordPopupText.setText("The cloud records are newer than the local records.");
                }
            }
            if (vehicleErrors != 0) {
                Long localEntryTime = 0L;
                Long remoteEntryTime = 0L;
                for (Vehicle vehicle : localVehicleList) {
                    if (vehicle.getEntryTime() > localEntryTime) localEntryTime = vehicle.getEntryTime();
                }
                for (Vehicle vehicle : remoteVehicleList) {
                    if (vehicle.getEntryTime() > remoteEntryTime) remoteEntryTime = vehicle.getEntryTime();
                }
                if (localEntryTime > remoteEntryTime) {
                    vehiclePopupText.setVisibility(View.VISIBLE);
                    vehicleIssueTxt.setVisibility(View.VISIBLE);
                    vehiclePopupText.setText("The local vehicles are newer than the cloud vehicles.");
                } else if (remoteEntryTime > localEntryTime) {
                    vehiclePopupText.setVisibility(View.VISIBLE);
                    vehicleIssueTxt.setVisibility(View.VISIBLE);
                    vehiclePopupText.setText("The cloud vehicles are newer than the local vehicles.");
                }
            }

            dialogBuilder.setView(compareDatabases);
            dialog = dialogBuilder.create();
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
            dialog.show();

            popupLocalBtn.setOnClickListener(view -> {
                dialog.dismiss();
                userRef.child(mUser.getUid()).child("Records").setValue("");
                userRef.child(mUser.getUid()).child("Vehicles").setValue("");
                userRef.child(mUser.getUid()).child("Records").setValue(localRecordList);
                userRef.child(mUser.getUid()).child("Vehicles").setValue(localVehicleList);
                continueToMainActivity();
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
                continueToMainActivity();
            });
            popupLogOutBtn.setOnClickListener(view -> {
                dialog.dismiss();
                mAuth.signOut();
                recreate();
            });
        } else continueToMainActivity();

         */
    }

    private void loadData() {
        Toast.makeText(this, "Welcome " + mUser.getEmail(), Toast.LENGTH_SHORT).show();
        normalView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        ProgressBar progressBar = loadingView.findViewById(R.id.progressBar);

        localVehicleList.clear();
        localRecordList.clear();
        remoteVehicleList.clear();
        remoteRecordList.clear();
        localVehicleList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
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

                final int[] i = {progressBar.getProgress()};
                Handler hdlr = new Handler();
                new Thread(new Runnable() {
                    public void run() {
                        while (i[0] < 100) {
                            i[0] += 1;
                            hdlr.post(new Runnable() {
                                public void run() {
                                    progressBar.setProgress(i[0]);
                                }
                            });
                            try {
                                Thread.sleep(25);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (i[0] == 40) loadingText.setText("Loading records...");
                            if (i[0] == 80) loadingText.setText("Loading settings...");
                        }
                        if (remoteRecordList.size() == 0 & localRecordList.size() > 0) {
                            userRef.child(mUser.getUid()).child("Records").setValue(localRecordList);
                        }
                        if (remoteVehicleList.size() == 0 & localVehicleList.size() > 0) {
                            userRef.child(mUser.getUid()).child("Vehicles").setValue(localVehicleList);
                        }
                        recordDatabase.recordDao().deleteAllRecords();
                        for (Record remoteRecord:remoteRecordList) {
                            recordDatabase.recordDao().addRecord(remoteRecord);
                        }
                        vehicleDatabase.vehicleDao().deleteAllVehicles();
                        for (Vehicle remoteVehicle:remoteVehicleList) {
                            vehicleDatabase.vehicleDao().addVehicle(remoteVehicle);
                        }
                        continueToMainActivity();
                    }
                }).start();
            }
        });
    }

    //Initializes Firebase Authentication
    private void initFirebase() {
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser!=null) {
            setPersistence();
            userRef = database.getReference("users");
            if (mUser.isEmailVerified()) {
                userRef.keepSynced(true);
                userRef.child(mUser.getUid()).child("User Info").child("Email Verified").setValue("True");
                userRef.child(mUser.getUid()).child("Settings").child("Dark Mode").setValue(sharedPref.getInt("dark_mode", 0));
                loadData();
            } else if (!mUser.isEmailVerified()){
                userRef.child(mUser.getUid()).child("User Info").child("Email Verified").setValue("False");
                Toast.makeText(this, "Your email is not verified yet. Check your email (Spam too!)", Toast.LENGTH_LONG).show();
                mUser.sendEmailVerification();
                mAuth.signOut();
            }
        }
    }

    private void setPersistence() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
            try {
                database.setPersistenceEnabled(true);
            } catch (Exception e) {
                Log.d("Exception", e.toString());
                Toast.makeText(this, "If you were offline, your data may not have saved. Please check to make sure.", Toast.LENGTH_SHORT).show();
            }
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
                            assert mUser != null;
                            if (!mUser.isEmailVerified()) {
                                userEmailInput.setText("");
                                userPasswordInput.setText("");
                                Toast.makeText(this, "Your email is not verified yet. Check your email (Spam too!)", Toast.LENGTH_LONG).show();
                                mUser.sendEmailVerification();
                                mAuth.signOut();
                                recreate();
                            } else {
                                Log.d("Users Size", String.valueOf(users.size()));
                                if (users.size() == 1) {
                                    user = users.get(0);
                                    Log.d("Local User ID", user.getFbUserId());
                                    Log.d("Cloud User ID", mUser.getUid());
                                    if (!mUser.getUid().equals(user.getFbUserId())) {
                                        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog))
                                                .setTitle("Warning")
                                                .setMessage("There is another user using this device. This will sign them out.")
                                                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        users.clear();
                                                        recordDatabase.recordDao().deleteAllRecords();
                                                        vehicleDatabase.vehicleDao().deleteAllVehicles();
                                                        userDatabase.userDao().deleteUser();
                                                        String[] userName = mUser.getDisplayName().split(" ");
                                                        user.setFbUserId(mUser.getUid());
                                                        user.setFirstName(userName[0]);
                                                        user.setLastName(userName[1]);
                                                        user.setEmail(mUser.getEmail());
                                                        Log.d("User", user.toString());
                                                        userDatabase.userDao().addUser(user);
                                                        loadData();
                                                    }
                                                })
                                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mAuth.signOut();
                                                        userEmailInput.setText("");
                                                        userPasswordInput.setText("");
                                                        recreate();
                                                    }
                                                })
                                                .setIcon(R.drawable.ic_round_warning_24)
                                                .show();
                                    } else {
                                        users.clear();
                                        String[] userName = mUser.getDisplayName().split(" ");
                                        user.setFbUserId(mUser.getUid());
                                        user.setFirstName(userName[0]);
                                        user.setLastName(userName[1]);
                                        user.setEmail(mUser.getEmail());
                                        Log.d("User", user.toString());
                                        userDatabase.userDao().updateUser(user);
                                        loadData();
                                    }
                                } else if (users.size() == 0) {
                                    users.clear();
                                    String[] userName = mUser.getDisplayName().split(" ");
                                    user.setFbUserId(mUser.getUid());
                                    user.setFirstName(userName[0]);
                                    user.setLastName(userName[1]);
                                    user.setEmail(mUser.getEmail());
                                    Log.d("User", user.toString());
                                    userDatabase.userDao().addUser(user);
                                    loadData();
                                }
                            }
                        } else Toast.makeText(this, "Login failed. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    //Takes user to registration page
    private void registerUser() {
        startActivity(new Intent(this, RegistrationActivity.class));
    }
}