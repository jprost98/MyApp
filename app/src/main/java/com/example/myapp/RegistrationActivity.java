package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapp.data.RecordDao;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.User;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private final User newUser = new User();
    private EditText firstNameInput, lastNameInput, emailInput, passwordInput, confirmPasswordInput;
    private VehicleDatabase vehicleDatabase;
    private RecordDatabase recordDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        int darkMode = sharedPref.getInt("dark_mode", 0);
        if (darkMode == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        int themePref = sharedPref.getInt("theme_pref", 0);
        if (themePref == 0) this.setTheme(R.style.DefaultTheme);
        else if (themePref == 1) this.setTheme(R.style.RedTheme);
        else if (themePref == 2) this.setTheme(R.style.BlueTheme);
        else if (themePref == 3) this.setTheme(R.style.GreenTheme);
        else if (themePref == 4) this.setTheme(R.style.GreyscaleTheme);
        Log.d("Theme", String.valueOf(themePref));
        Log.d("Dark Mode", String.valueOf(darkMode));
        setTitle("Registration");
        setContentView(R.layout.activity_registration);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initFirebase();
        initInputs();

        Button finish_registration = findViewById(R.id.complete_register_btn);
        finish_registration.setOnClickListener(v -> {
            registerUser();
        });
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

    //Initializes Firebase Authentication
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    private void initInputs() {
        firstNameInput = findViewById(R.id.register_first_name);
        lastNameInput = findViewById(R.id.register_last_name);
        emailInput = findViewById(R.id.register_email);
        passwordInput = findViewById(R.id.register_password);
        confirmPasswordInput = findViewById(R.id.register_confirm_password);
    }

    private void registerUser() {
        String password, confirmPassword;
        Integer errors = 0;
        password = passwordInput.getText().toString().trim();
        confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (firstNameInput.getText().toString().trim().isEmpty()) {
            firstNameInput.setError("Enter your first name");
            errors++;
        }
        if (lastNameInput.getText().toString().trim().isEmpty()) {
            lastNameInput.setError("Enter your last name");
            errors++;
        }
        if (emailInput.getText().toString().trim().isEmpty()) {
            emailInput.setError("Enter a valid email address");
            errors++;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Enter a password");
            errors++;
        }
        if (password.length() < 7) {
            passwordInput.setError("Password must be 8 or more characters");
            errors++;
        }
        if (confirmPasswordInput.getText().toString().trim().isEmpty()) {
            confirmPasswordInput.setError("Retype your password");
            errors++;
        }
        if (!confirmPassword.equals(password)) {
            passwordInput.setText("");
            confirmPasswordInput.setText("");
            confirmPasswordInput.setError("Passwords do not match");
            errors++;
        }
        if (errors == 0) {
            newUser.setFirstName(firstNameInput.getText().toString().trim());
            newUser.setLastName(lastNameInput.getText().toString().trim());
            newUser.setEmail(emailInput.getText().toString().trim());
            newUser.setPassword(passwordInput.getText().toString().trim());

            mAuth.createUserWithEmailAndPassword(newUser.getEmail(), newUser.getPassword())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(newUser.getFirstName() + " " + newUser.getLastName())
                                        .build();
                                mAuth.getCurrentUser().updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d("Update Profile", "User profile updated.");
                                                }
                                            }
                                        });
                                mUser = mAuth.getCurrentUser();
                                createDatabase();
                                Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().fallbackToDestructiveMigration().build();
                                recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().fallbackToDestructiveMigration().build();
                                vehicleDatabase.vehicleDao().deleteAllVehicles();
                                recordDatabase.recordDao().deleteAllRecords();
                                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(RegistrationActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void createDatabase() {
        userRef.child(mUser.getUid()).child("User Info");
        userRef.child(mUser.getUid()).child("User Info").child("Email").setValue(newUser.getEmail());
        userRef.child(mUser.getUid()).child("User Info").child("First Name").setValue(newUser.getFirstName());
        userRef.child(mUser.getUid()).child("User Info").child("Last Name").setValue(newUser.getLastName());
        userRef.child(mUser.getUid()).child("User Info").child("UID").setValue(mUser.getUid());
    }
}