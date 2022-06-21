package com.example.myapp;

import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.databinding.ActivityMainBinding;
import com.example.myapp.ui.home.HomeFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private Integer themePref;
    private String filterBy;
    private Record record = new Record();
    private Vehicle vehicle = new Vehicle();
    private ArrayList<Record> recordArrayList = new ArrayList<>();
    private ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private RecyclerView vehiclesRecyclerView, recordsRecyclerView;
    private RecordAdapter recordAdapter;
    private VehicleAdapter vehicleAdapter;
    private VehicleDatabase vehicleDatabase;
    private RecordDatabase recordDatabase;
    private SharedPreferences.Editor editor;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        themePref = sharedPref.getInt("theme_pref_value", 0);
        filterBy = sharedPref.getString("filter_by_value", "All");
        Log.d("Filter Value", filterBy);
        Log.d("Theme Pref", themePref.toString());
        if (themePref == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Log.d("Theme", "Light Theme");
        } else if (themePref == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Log.d("Theme", "Dark Theme");
        }
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initFirebase();

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;

        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_vehicles, R.id.nav_settings)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        View headerView = navigationView.getHeaderView(0);
        TextView navUsername = headerView.findViewById(R.id.nav_header_username);
        navUsername.setText(mUser.getDisplayName());

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == R.id.nav_settings) {
                binding.appBarMain.fab.hide();
            } else binding.appBarMain.fab.show();
        });

        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (navigationView.getCheckedItem().toString().equals("Home")) {
                    startActivity(new Intent(MainActivity.this, AddRecord.class));
                } else if (navigationView.getCheckedItem().toString().equals("Vehicles")) {
                    startActivity(new Intent(MainActivity.this, AddVehicle.class));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        super.onStart();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_records_by_vehicle:
                navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
                    if (navDestination.getId() == R.id.nav_home) {
                        filterRecords();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void filterRecords() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View filterRecordsPopup = getLayoutInflater().inflate(R.layout.popup_filter_records, null);

        ArrayList<String> filterOptions = new ArrayList<>();
        filterOptions.add("All");
        vehicleArrayList.clear();
        vehicleDatabase = Room.databaseBuilder(this, VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        for (Vehicle vehicle:vehicleArrayList) {
            filterOptions.add(vehicle.vehicleTitle());
        }
        int i = 100;
        RadioGroup radioGroup = filterRecordsPopup.findViewById(R.id.record_filter_rg);
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 5, 0, 5);
        for (String option:filterOptions) {
            i++;
            RadioButton radioButton = new RadioButton(filterRecordsPopup.getContext());
            radioButton.setText(option);
            radioButton.setId(i);
            radioButton.setLayoutParams(params);
            radioButton.setTextSize(18);
            radioGroup.addView(radioButton);
            if (radioButton.getText().toString().equals(filterBy)) radioButton.setChecked(true);
        }

        dialogBuilder.setView(filterRecordsPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioButton = filterRecordsPopup.findViewById(radioGroup.getCheckedRadioButtonId());
                filterBy = radioButton.getText().toString();
                editor.putString("filter_by_value", filterBy);
                editor.apply();
                dialog.dismiss();
                recreate();
            }
        });
    }
}