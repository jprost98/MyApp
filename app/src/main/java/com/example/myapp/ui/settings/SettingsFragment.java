package com.example.myapp.ui.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.myapp.LoginActivity;
import com.example.myapp.R;
import com.example.myapp.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String theme;
    private int savedPref;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        savedPref = sharedPref.getInt(getString(R.string.saved_value), 0);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initFirebase();

        Button logout_user_button = root.findViewById(R.id.settings_logout_btn2);
        logout_user_button.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch themeSwitch = root.findViewById(R.id.theme_switch);
        if (savedPref == 0) {
            themeSwitch.setChecked(false);
            Log.d("Theme", "Light Theme");
        } else if (savedPref == 1) {
            themeSwitch.setChecked(true);
            Log.d("Theme", "Dark Theme");
        }
        if (themeSwitch.isChecked()) {
            editor.putInt(getString(R.string.saved_value), 1);
            editor.apply();
            Log.d("Switch", "On");
        } else {
            editor.putInt(getString(R.string.saved_value), 0);
            editor.apply();
            Log.d("Switch", "Off");
        }
        themeSwitch.setOnClickListener(view -> {
            if (themeSwitch.isChecked()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putInt(getString(R.string.saved_value), 1);
                editor.apply();
                Log.d("Switch", "On");
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putInt(getString(R.string.saved_value), 0);
                editor.apply();
                Log.d("Switch", "Off");
            }
        });

        return root;
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