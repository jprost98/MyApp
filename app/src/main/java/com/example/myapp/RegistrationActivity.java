package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.User;
import com.example.myapp.data.UserDatabase;
import com.example.myapp.data.VehicleDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("users");
    private final User newUser = new User();
    private EditText firstNameInput, lastNameInput, emailInput, passwordInput, confirmPasswordInput;

    private TextInputLayout firstNameLayout, lastNameLayout, emailLayout, passwordLayout, confirmPasswordLayout;
    private VehicleDatabase vehicleDatabase;
    private RecordDatabase recordDatabase;
    private UserDatabase userDatabase;
    private final ArrayList<User> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_registration);

        setSupportActionBar(findViewById(R.id.registration_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Registration");
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
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Initializes Firebase Authentication
    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void initInputs() {
        firstNameLayout = findViewById(R.id.register_first_name);
        lastNameLayout = findViewById(R.id.register_last_name);
        emailLayout = findViewById(R.id.register_email);
        passwordLayout = findViewById(R.id.register_password);
        confirmPasswordLayout = findViewById(R.id.register_confirm_password);

        firstNameInput = firstNameLayout.getEditText();
        lastNameInput = lastNameLayout.getEditText();
        emailInput = emailLayout.getEditText();
        passwordInput = passwordLayout.getEditText();
        confirmPasswordInput = confirmPasswordLayout.getEditText();
    }

    private void registerUser() {
        String password, confirmPassword;
        int errors = 0;
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
                                mUser = mAuth.getCurrentUser();
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(newUser.getFirstName() + " " + newUser.getLastName())
                                        .build();
                                mUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    newUser.setFbUserId(mUser.getUid());
                                                    userRef.child(mUser.getUid()).child("user_info").child("first_name").setValue(newUser.getFirstName());
                                                    userRef.child(mUser.getUid()).child("user_info").child("last_name").setValue(newUser.getLastName());
                                                    userRef.child(mUser.getUid()).child("user_info").child("email").setValue(newUser.getEmail());
                                                    userRef.child(mUser.getUid()).child("user_info").child("last_login").setValue(SimpleDateFormat.getDateInstance().format(Calendar.getInstance().getTime()));

                                                    Toast.makeText(RegistrationActivity.this, "Registration successful! Please verify email first. Check your email (Spam too!)", Toast.LENGTH_SHORT).show();
                                                    mUser.sendEmailVerification();
                                                    mAuth.signOut();
                                                    startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                                                    finish();
                                                }
                                            }
                                        });
                            } else {
                                Toast.makeText(RegistrationActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}