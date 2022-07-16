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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.LoginActivity;
import com.example.myapp.MainActivity;
import com.example.myapp.R;
import com.example.myapp.databinding.FragmentSettingsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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
        EditText newEmail = updateProfilePopup.findViewById(R.id.update_email);
        EditText oldPassword = updateProfilePopup.findViewById(R.id.update_old_password);
        Button updateBtn = updateProfilePopup.findViewById(R.id.update_btn);
        Button cancelBtn = updateProfilePopup.findViewById(R.id.update_cancel_btn);
        Button confirmBtn = updateProfilePopup.findViewById(R.id.update_confirm_btn);
        Button cancelBtn2 = updateProfilePopup.findViewById(R.id.update_cancel_btn2);
        String[] userName = mUser.getDisplayName().split(" ");
        String currentEmail = mUser.getEmail();
        final String[] firstNameTxt = new String[1];
        final String[] lastNameTxt = new String[1];
        final String[] newEmailTxt = new String[1];
        final String[] oldPasswordTxt = new String[1];
        final int[] errors = {0};
        LinearLayout firstNames, lastNames, newEmails, oldPasswords, updateBtns, confirmBtns;
        firstNames = updateProfilePopup.findViewById(R.id.firstName);
        lastNames = updateProfilePopup.findViewById(R.id.lastName);
        newEmails = updateProfilePopup.findViewById(R.id.newEmail);
        oldPasswords = updateProfilePopup.findViewById(R.id.oldPassword);
        updateBtns = updateProfilePopup.findViewById(R.id.update_btns);
        confirmBtns = updateProfilePopup.findViewById(R.id.confirm_btns);

        firstName.setText(userName[0]);
        lastName.setText(userName[1]);
        firstNames.setVisibility(View.VISIBLE);
        lastNames.setVisibility(View.VISIBLE);
        newEmails.setVisibility(View.VISIBLE);
        updateBtns.setVisibility(View.VISIBLE);
        oldPasswords.setVisibility(View.GONE);
        confirmBtns.setVisibility(View.GONE);

        dialogBuilder.setView(updateProfilePopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(false);

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errors[0] = 0;
                firstNameTxt[0] = firstName.getText().toString().trim();
                lastNameTxt[0] = lastName.getText().toString().trim();
                newEmailTxt[0] = newEmail.getText().toString().trim();

                if (firstNameTxt[0].isEmpty()) {
                    firstName.setError("Enter your first name");
                    errors[0]++;
                }
                if (lastNameTxt[0].isEmpty()) {
                    lastName.setError("Enter your last name");
                    errors[0]++;
                }
                firstNames.setVisibility(View.GONE);
                lastNames.setVisibility(View.GONE);
                newEmails.setVisibility(View.GONE);
                updateBtns.setVisibility(View.GONE);
                oldPasswords.setVisibility(View.VISIBLE);
                confirmBtns.setVisibility(View.VISIBLE);
            }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oldPasswordTxt[0] = oldPassword.getText().toString().trim();
                if (oldPasswordTxt[0].isEmpty()) {
                    oldPassword.setError("Enter current password");
                    errors[0]++;
                }
                if (errors[0] == 0) {
                    AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, oldPasswordTxt[0]); // Current Login Credentials
                    mUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                if (!newEmailTxt[0].isEmpty()) {
                                    mUser.updateEmail(newEmailTxt[0]).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d("New Email", newEmailTxt[0]);
                                                userRef.child(mUser.getUid()).child("User Info").child("Email").setValue(newEmailTxt[0]);
                                                Toast.makeText(getActivity(), "Email changed", Toast.LENGTH_SHORT).show();
                                            } else Toast.makeText(getActivity(), "Email change failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                userRef.child(mUser.getUid()).child("User Info").child("First Name").setValue(firstNameTxt[0]);
                                userRef.child(mUser.getUid()).child("User Info").child("Last Name").setValue(lastNameTxt[0]);
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(firstNameTxt[0] + " " + lastNameTxt[0])
                                        .build();
                                mUser.updateProfile(profileUpdates);
                                mAuth.signOut();
                                startActivity(new Intent(getActivity(), LoginActivity.class));
                                getActivity().finish();
                                dialog.dismiss();
                            } else Toast.makeText(getContext(), "Old password is not valid", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        cancelBtn2.setOnClickListener(new View.OnClickListener() {
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