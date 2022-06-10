package com.example.myapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initFirebase();

        Button login_user_button = findViewById(R.id.login_btn);
        login_user_button.setOnClickListener(v -> {
            loginUser();
        });
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    private void loginUser() {
        EditText userEmailInput, userPasswordInput;
        String userEmail, userPassword;
        userEmailInput = findViewById(R.id.login_email_input);
        userPasswordInput = findViewById(R.id.login_password_input);
        userEmail = userEmailInput.getText().toString().trim();
        userPassword = userPasswordInput.getText().toString().trim();

        if (userEmail.isEmpty() & userPassword.isEmpty()) Toast.makeText(this, "Please enter an email address and password", Toast.LENGTH_SHORT).show();
        else if (userEmail.isEmpty()) Toast.makeText(this, "Please enter an email address", Toast.LENGTH_SHORT).show();
        else if (userPassword.isEmpty()) Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
        else {
            mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
//                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}