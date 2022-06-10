package com.example.myapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final UserInfo user = new UserInfo();
    private EditText firstNameInput, lastNameInput, emailInput, passwordInput, confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        if (firstNameInput.getText().toString().equals("")) {
            firstNameInput.setError("Enter your first name");
            errors++;
        }
        if (lastNameInput.getText().toString().equals("")) {
            lastNameInput.setError("Enter your last name");
            errors++;
        }
        if (emailInput.getText().toString().equals("")) {
            emailInput.setError("Enter a valid email address");
            errors++;
        }
        if (passwordInput.getText().toString().equals("")) {
            passwordInput.setError("Enter a password");
            errors++;
        }
        if (confirmPasswordInput.getText().toString().equals("")) {
            confirmPasswordInput.setError("Retype your password");
            errors++;
        }
        if (!password.equals(confirmPassword)) {
            passwordInput.setText("");
            confirmPasswordInput.setText("");
            confirmPasswordInput.setError("Passwords do not match");
            errors++;
        }
        if (errors == 0) {
            user.setFirstName(firstNameInput.getText().toString().trim());
            user.setLastName(lastNameInput.getText().toString().trim());
            user.setEmail(emailInput.getText().toString().trim());
            user.setPassword(passwordInput.getText().toString().trim());

            mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(user.getFirstName() + " " + user.getLastName())
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
                                Toast.makeText(RegistrationActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(RegistrationActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}