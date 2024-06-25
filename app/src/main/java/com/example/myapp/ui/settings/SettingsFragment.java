package com.example.myapp.ui.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.Room;

import com.example.myapp.LoginActivity;
import com.example.myapp.R;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.UserDatabase;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.databinding.FragmentSettingsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private final boolean shouldRefreshOnResume = false;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private ValueEventListener eventListener;
    private int themePref;
    private int darkMode;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private MaterialSwitch darkModeSwitch;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private View root;
    private VehicleDatabase vehicleDatabase;
    private RecordDatabase recordDatabase;
    private UserDatabase userDatabase;
    public Context context;
    private AutoCompleteTextView themePicker;
    private TextInputLayout themePickerLayout;
    private EditText themePickerET;
    private String themeSelection;


    @SuppressLint("NonConstantResourceId")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        sharedPref = requireActivity().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        darkMode = sharedPref.getInt("dark_mode", 0);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        initFirebase();

        Button logout_user_button = root.findViewById(R.id.settings_logout_btn);
        logout_user_button.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Warning")
                    .setMessage("This will sign you out. If you are offline, any changes made may not sync.")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mAuth.signOut();
                            startActivity(new Intent(getActivity(), LoginActivity.class));
                            requireActivity().finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //
                        }
                    })
                    .setIcon(R.drawable.ic_round_warning_24)
                    .show();
        });
        Button resetPasswordButton = root.findViewById(R.id.reset_pswd_btn);
        resetPasswordButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(new ContextThemeWrapper(getActivity(), R.style.myDialog))
                    .setTitle("Warning")
                    .setMessage("This will send you a password reset link then sign you out.")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            resetPassword();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //
                        }
                    })
                    .setIcon(R.drawable.ic_round_warning_24)
                    .show();
        });
        Button deleteAccountButton = root.findViewById(R.id.delete_account_btn);
        deleteAccountButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(new ContextThemeWrapper(getActivity(), R.style.myDialog))
                    .setTitle("Warning")
                    .setMessage("This will delete your account and all associated data.")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            deleteAccount();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(R.drawable.ic_round_warning_24)
                    .show();
        });

        darkModeChooser();

        return root;
    }

    private void initVars() {
        themePickerLayout = root.findViewById(R.id.settings_theme_picker);
        themePickerET = themePickerLayout.getEditText();
        assert themePickerET != null;
        themePickerET.setText(themeSelection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        this.context = context;
        super.onAttach(context);
    }

    private void deleteAccount() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View deleteAccountPopup = getLayoutInflater().inflate(R.layout.popup_delete_auth, null);

        TextInputLayout currentPasswordLayout, confirmPasswordLayout;

        currentPasswordLayout = deleteAccountPopup.findViewById(R.id.delete_current_pswd);
        confirmPasswordLayout = deleteAccountPopup.findViewById(R.id.delete_confirm_pswd);

        EditText currentPassword = currentPasswordLayout.getEditText();
        EditText confirmPassword = confirmPasswordLayout.getEditText();

        Button deleteCancelBtn = deleteAccountPopup.findViewById(R.id.delete_cancel_btn);
        Button deleteConfirmBtn = deleteAccountPopup.findViewById(R.id.delete_confirm_btn);

        dialogBuilder.setView(deleteAccountPopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(true);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialog.dismiss();
            }
        });

        deleteCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        deleteConfirmBtn.setOnClickListener(new View.OnClickListener() {
            int errors = 0;
            @Override
            public void onClick(View view) {
                assert currentPassword != null;
                if (currentPassword.getText().toString().trim().equals("")) {
                    currentPassword.setError("Enter your current password");
                    errors++;
                }
                assert confirmPassword != null;
                if (confirmPassword.getText().toString().trim().equals("")) {
                    confirmPassword.setError("Re-enter your password");
                    errors++;
                }
                if (!currentPassword.getText().toString().trim().equals(confirmPassword.getText().toString().trim())) {
                    currentPassword.setError("Passwords don't match");
                    confirmPassword.setError("Passwords don't match");
                    errors++;
                }
                if (!confirmPassword.getText().toString().trim().equals(currentPassword.getText().toString().trim())) {
                    currentPassword.setError("Passwords don't match");
                    confirmPassword.setError("Passwords don't match");
                    errors++;
                }
                if (errors == 0) {
                    AuthCredential credentials = EmailAuthProvider.getCredential(Objects.requireNonNull(mUser.getEmail()), confirmPassword.getText().toString().trim());

                    mUser.reauthenticate(credentials).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                mUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            recordDatabase = Room.databaseBuilder(requireActivity(), RecordDatabase.class, "records").allowMainThreadQueries().fallbackToDestructiveMigration().build();
                                            vehicleDatabase = Room.databaseBuilder(requireActivity(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().fallbackToDestructiveMigration().build();
                                            userDatabase = Room.databaseBuilder(requireActivity(), UserDatabase.class, "users").allowMainThreadQueries().fallbackToDestructiveMigration().build();

                                            vehicleDatabase.vehicleDao().deleteAllVehicles();
                                            recordDatabase.recordDao().deleteAllRecords();
                                            userDatabase.userDao().deleteUser();

                                            Toast.makeText(getActivity(), "Your account has been deleted", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                            startActivity(new Intent(getActivity(), LoginActivity.class));
                                            requireActivity().finish();
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(getActivity(), Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "Correct errors and try again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void resetPassword() {
        mAuth.sendPasswordResetEmail(Objects.requireNonNull(mUser.getEmail())).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(getContext(), "A reset link has been sent to your email. (Check spam)", Toast.LENGTH_LONG).show();
                mAuth.signOut();
                startActivity(new Intent(getActivity(), LoginActivity.class));
                requireActivity().finish();
            }
        });
    }

    private void darkModeChooser() {
        darkModeSwitch = root.findViewById(R.id.theme_switch);
        if (darkMode == 0) {
            darkModeSwitch.setChecked(false);
        } else if (darkMode == 1) {
            darkModeSwitch.setChecked(true);
        }
        if (darkModeSwitch.isChecked()) {
            editor.putInt("dark_mode", 1);
            editor.apply();
        } else {
            editor.putInt("dark_mode", 0);
            editor.apply();
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
            userRef.child("settings").child("dark_mode").setValue(sharedPref.getInt("dark_mode", 0));
            requireActivity().recreate();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        userRef.removeEventListener(eventListener);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        userRef = database.getReference("users").child(mUser.getUid());

        eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.child("settings").child("theme").exists()) userRef.child("settings").child("theme").setValue("Default");
                themeSelection = snapshot.child("settings").child("theme").getValue(String.class);
                initVars();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Error", error.toString());
            }
        };

        userRef.addValueEventListener(eventListener);
    }
}