package com.example.myapp.ui.settings;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.LoginActivity;
import com.example.myapp.MainActivity;
import com.example.myapp.R;
import com.example.myapp.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private int themePref;
    private int darkMode;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch darkModeSwitch;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private View root;

    @SuppressLint("NonConstantResourceId")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        sharedPref = getActivity().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        themePref = sharedPref.getInt("theme_pref", 0);
        darkMode = sharedPref.getInt("dark_mode", 0);

        if (themePref == 0) getActivity().setTheme(R.style.DefaultTheme);
        else if (themePref == 1) getActivity().setTheme(R.style.RedTheme);
        else if (themePref == 2) getActivity().setTheme(R.style.BlueTheme);
        else if (themePref == 3) getActivity().setTheme(R.style.GreenTheme);
        else if (themePref == 4) getActivity().setTheme(R.style.GreyscaleTheme);
        Log.d("Theme", String.valueOf(themePref));

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        initFirebase();

        Button logout_user_button = root.findViewById(R.id.settings_logout_btn);
        logout_user_button.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });
        Button updateProfileButton = root.findViewById(R.id.update_profile_btn);
        updateProfileButton.setOnClickListener(v -> {
            updateUserProfile();
        });

        themeChooser();
        darkModeChooser();

        return root;
    }

    private void darkModeChooser() {
        darkModeSwitch = root.findViewById(R.id.dark_mode_switch);
        if (darkMode == 0) {
            darkModeSwitch.setChecked(false);
        } else if (darkMode == 1) {
            darkModeSwitch.setChecked(true);
        }
        if (darkModeSwitch.isChecked()) {
            editor.putInt("dark_mode", 1);
            editor.apply();
            Log.d("Switch", "On");
        } else {
            editor.putInt("dark_mode", 0);
            editor.apply();
            Log.d("Switch", "Off");
        }
        darkModeSwitch.setOnClickListener(view -> {
            if (darkModeSwitch.isChecked()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putInt("dark_mode", 1);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putInt("dark_mode", 0);
            }
            editor.apply();
            userRef.child(mUser.getUid()).child("Settings").child("Dark Mode").setValue(sharedPref.getInt("dark_mode", 0));
            getActivity().recreate();
        });
    }

    private void themeChooser() {
        RadioButton defaultRadioBtn = root.findViewById(R.id.default_theme);
        RadioButton redRadioBtn = root.findViewById(R.id.red_theme);
        RadioButton blueRadioBtn = root.findViewById(R.id.blue_theme);
        RadioButton greenRadioBtn = root.findViewById(R.id.green_theme);
        RadioButton greyscaleRadioBtn = root.findViewById(R.id.greyscale_theme);
        if (themePref == 0) defaultRadioBtn.setChecked(true);
        else if (themePref == 1) redRadioBtn.setChecked(true);
        else if (themePref == 2) blueRadioBtn.setChecked(true);
        else if (themePref == 3) greenRadioBtn.setChecked(true);
        else if (themePref == 4) greyscaleRadioBtn.setChecked(true);
        defaultRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt("theme_pref", 0);
                editor.apply();
                userRef.child(mUser.getUid()).child("Settings").child("Theme").setValue(sharedPref.getInt("theme_pref", 0));
                getActivity().recreate();
            }
        });
        redRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt("theme_pref", 1);
                editor.apply();
                userRef.child(mUser.getUid()).child("Settings").child("Theme").setValue(sharedPref.getInt("theme_pref", 0));
                getActivity().recreate();
            }
        });
        blueRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt("theme_pref", 2);
                editor.apply();
                userRef.child(mUser.getUid()).child("Settings").child("Theme").setValue(sharedPref.getInt("theme_pref", 0));
                getActivity().recreate();
            }
        });
        greenRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt("theme_pref", 3);
                editor.apply();
                userRef.child(mUser.getUid()).child("Settings").child("Theme").setValue(sharedPref.getInt("theme_pref", 0));
                getActivity().recreate();
            }
        });
        greyscaleRadioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt("theme_pref", 4);
                editor.apply();
                userRef.child(mUser.getUid()).child("Settings").child("Theme").setValue(sharedPref.getInt("theme_pref", 0));
                getActivity().recreate();
            }
        });
    }

    private void updateUserProfile() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View updateProfilePopup = getLayoutInflater().inflate(R.layout.popup_update_profile, null);

        EditText firstName = updateProfilePopup.findViewById(R.id.update_first_name);
        EditText lastName = updateProfilePopup.findViewById(R.id.update_last_name);
        EditText email = updateProfilePopup.findViewById(R.id.update_email);
        EditText password = updateProfilePopup.findViewById(R.id.update_password);
        EditText confirmPassword = updateProfilePopup.findViewById(R.id.update_confirm_password);
        Button updateBtn = updateProfilePopup.findViewById(R.id.update_btn);
        Button cancelBtn = updateProfilePopup.findViewById(R.id.update_cancel_btn);
        String[] userName = mUser.getDisplayName().split(" ");

        firstName.setText(userName[0]);
        lastName.setText(userName[1]);
        email.setText(mUser.getEmail());

        dialogBuilder.setView(updateProfilePopup);
        dialog = dialogBuilder.create();
        dialog.show();
        dialog.setCancelable(false);

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int errors = 0;
                if (firstName.getText().toString().trim().isEmpty()) {
                    firstName.setError("Enter your first name");
                    errors++;
                }
                if (lastName.getText().toString().trim().isEmpty()) {
                    lastName.setError("Enter your last name");
                    errors++;
                }
                if (email.getText().toString().trim().isEmpty()) {
                    email.setError("Enter a valid email address");
                    errors++;
                }
                if (password.getText().toString().trim().length() < 7 && password.getText().toString().trim().length() > 1) {
                    password.setError("Password must be 8 or more characters");
                    errors++;
                }
                if (!confirmPassword.getText().toString().trim().equals(password.getText().toString().trim())) {
                    password.setText("");
                    confirmPassword.setText("");
                    confirmPassword.setError("Passwords do not match");
                    errors++;
                }
                if (errors == 0) {
                    userRef.child(mUser.getUid()).child("User Info").child("First Name").setValue(firstName.getText().toString().trim());
                    userRef.child(mUser.getUid()).child("User Info").child("Last Name").setValue(lastName.getText().toString().trim());
                    userRef.child(mUser.getUid()).child("User Info").child("Email").setValue(email.getText().toString().trim());
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(firstName.getText().toString().trim() + " " + lastName.getText().toString().trim())
                            .build();
                    mUser.updateProfile(profileUpdates);
                    mUser.updateEmail(email.getText().toString().trim());
                    if (!password.getText().toString().trim().isEmpty()) mUser.updatePassword(password.getText().toString().trim());
                    getActivity().recreate();
                    dialog.dismiss();
                }
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }
}