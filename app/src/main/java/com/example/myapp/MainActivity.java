package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String filterBy, sortRecords, sortVehicles;
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
    private SharedPreferences sharedPref;
    private int darkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        darkMode = sharedPref.getInt("dark_mode", 0);
        filterBy = sharedPref.getString("filter_by_value", "All");
        sortRecords = sharedPref.getString("sort_records", "Date_Desc");
        sortVehicles = sharedPref.getString("sort_vehicles", "Year_Desc");
        Log.d("Filter Value", filterBy);
        Log.d("Sort Records", sortRecords);
        Log.d("Sort Vehicles", sortVehicles);
        if (darkMode == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initFirebase();

        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());

        setSupportActionBar(binding.toolbar);

        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_vehicles, R.id.navigation_settings)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavView, navController);

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == R.id.navigation_settings) {
                binding.fab.hide();
            } else {
                binding.fab.show();
            }
        });

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (navController.getCurrentDestination().getId() == R.id.navigation_home) {
                    vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
                    vehicleArrayList.clear();
                    vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
                    if (!vehicleArrayList.isEmpty()) {
                        startActivity(new Intent(MainActivity.this, AddRecord.class));
                    } else Snackbar.make(MainActivity.this.findViewById(R.id.bottom_nav_view), "Need to add vehicles before making maintenance records.", Snackbar.LENGTH_LONG).show();
                } else if (navController.getCurrentDestination().getId() == R.id.navigation_vehicles) {
                    startActivity(new Intent(MainActivity.this, AddVehicle.class));
                }
            }
        });
    }

    public void onBackPressed() {
        recreate();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == R.id.navigation_home) {
                menu.getItem(0).setVisible(true);
                menu.getItem(1).setVisible(true);
                menu.getItem(2).setVisible(true);
            } else if (navDestination.getId() == R.id.navigation_vehicles){
                menu.getItem(0).setVisible(true);
                menu.getItem(1).setVisible(false);
                menu.getItem(2).setVisible(true);
            } else if (navDestination.getId() == R.id.navigation_settings) {
                menu.getItem(0).setVisible(false);
                menu.getItem(1).setVisible(false);
                menu.getItem(2).setVisible(false);
            }
        });
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_records:
                if (navController.getCurrentDestination().getId() == R.id.navigation_home) {
                    if (!vehicleDatabase.vehicleDao().getAllVehicles().isEmpty()) filterRecords();
                    else Toast.makeText(this, "Nothing to filter", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.sort_records:
                if (navController.getCurrentDestination().getId() == R.id.navigation_home) {
                    if (!vehicleDatabase.vehicleDao().getAllVehicles().isEmpty()) sortRecords();
                    else Toast.makeText(this, "Nothing to sort", Toast.LENGTH_SHORT).show();
                } else if (navController.getCurrentDestination().getId() == R.id.navigation_vehicles) {
                    if (!vehicleDatabase.vehicleDao().getAllVehicles().isEmpty()) sortVehicles();
                    else Toast.makeText(this, "Nothing to sort", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return false;
        }
    }

    private void sortVehicles() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View sortVehiclesPopup = getLayoutInflater().inflate(R.layout.popup_sort, null);
        sortVehiclesPopup.findViewById(R.id.sort_vehicles_group).setVisibility(View.VISIBLE);
        sortVehiclesPopup.findViewById(R.id.sort_vehicles_title).setVisibility(View.VISIBLE);

        Button cancelBtn = sortVehiclesPopup.findViewById(R.id.sort_cancel_button);
        RadioGroup sortGroup = sortVehiclesPopup.findViewById(R.id.sort_vehicles_group);
        RadioButton yearDesc = sortVehiclesPopup.findViewById(R.id.year_desc);
        RadioButton yearAsc = sortVehiclesPopup.findViewById(R.id.year_asc);
        RadioButton makeAsc = sortVehiclesPopup.findViewById(R.id.make_asc);
        RadioButton makeDesc = sortVehiclesPopup.findViewById(R.id.make_desc);

        if (sortVehicles.equals("Year_Desc")) yearDesc.setChecked(true);
        if (sortVehicles.equals("Year_Asc")) yearAsc.setChecked(true);
        if (sortVehicles.equals("Make_Asc")) makeAsc.setChecked(true);
        if (sortVehicles.equals("Make_Desc")) makeDesc.setChecked(true);

        dialogBuilder.setView(sortVehiclesPopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(false);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                sortVehiclesPopup.findViewById(R.id.sort_vehicles_group).setVisibility(View.GONE);
                sortVehiclesPopup.findViewById(R.id.sort_vehicles_title).setVisibility(View.GONE);
            }
        });

        sortGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioButton = sortVehiclesPopup.findViewById(radioGroup.getCheckedRadioButtonId());
                if (radioButton.getText().toString().equals(yearDesc.getText().toString())) sortVehicles = "Year_Desc";
                if (radioButton.getText().toString().equals(yearAsc.getText().toString())) sortVehicles = "Year_Asc";
                if (radioButton.getText().toString().equals(makeAsc.getText().toString())) sortVehicles = "Make_Asc";
                if (radioButton.getText().toString().equals(makeDesc.getText().toString())) sortVehicles = "Make_Desc";
                Log.d("Sort vehicles", sortVehicles);
                editor.putString("sort_vehicles", sortVehicles);
                editor.apply();
                dialog.dismiss();
                sortVehiclesPopup.findViewById(R.id.sort_vehicles_group).setVisibility(View.GONE);
                sortVehiclesPopup.findViewById(R.id.sort_vehicles_title).setVisibility(View.GONE);
                recreate();
            }
        });
    }

    private void sortRecords() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View sortRecordsPopup = getLayoutInflater().inflate(R.layout.popup_sort, null);
        sortRecordsPopup.findViewById(R.id.sort_records_group).setVisibility(View.VISIBLE);
        sortRecordsPopup.findViewById(R.id.sort_records_title).setVisibility(View.VISIBLE);

        Button cancelBtn = sortRecordsPopup.findViewById(R.id.sort_cancel_button);
        RadioGroup sortGroup = sortRecordsPopup.findViewById(R.id.sort_records_group);
        RadioButton dateDesc = sortRecordsPopup.findViewById(R.id.date_desc);
        RadioButton dateAsc = sortRecordsPopup.findViewById(R.id.date_asc);
        RadioButton milesDesc = sortRecordsPopup.findViewById(R.id.miles_desc);
        RadioButton milesAsc = sortRecordsPopup.findViewById(R.id.miles_asc);
        RadioButton titleAsc = sortRecordsPopup.findViewById(R.id.title_asc);
        RadioButton titleDesc = sortRecordsPopup.findViewById(R.id.title_desc);

        if (sortRecords.equals("Date_Desc")) dateDesc.setChecked(true);
        if (sortRecords.equals("Date_Asc")) dateAsc.setChecked(true);
        if (sortRecords.equals("Miles_Desc")) milesDesc.setChecked(true);
        if (sortRecords.equals("Miles_Asc")) milesAsc.setChecked(true);
        if (sortRecords.equals("Title_Asc")) titleAsc.setChecked(true);
        if (sortRecords.equals("Title_Desc")) titleDesc.setChecked(true);

        dialogBuilder.setView(sortRecordsPopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(false);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                sortRecordsPopup.findViewById(R.id.sort_records_group).setVisibility(View.GONE);
                sortRecordsPopup.findViewById(R.id.sort_records_title).setVisibility(View.GONE);
            }
        });

        sortGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioButton = sortRecordsPopup.findViewById(radioGroup.getCheckedRadioButtonId());
                if (radioButton.getText().toString().equals(dateDesc.getText().toString())) sortRecords = "Date_Desc";
                if (radioButton.getText().toString().equals(dateAsc.getText().toString())) sortRecords = "Date_Asc";
                if (radioButton.getText().toString().equals(milesDesc.getText().toString())) sortRecords = "Miles_Desc";
                if (radioButton.getText().toString().equals(milesAsc.getText().toString())) sortRecords = "Miles_Asc";
                if (radioButton.getText().toString().equals(titleAsc.getText().toString())) sortRecords = "Title_Asc";
                if (radioButton.getText().toString().equals(titleDesc.getText().toString())) sortRecords = "Title_Desc";
                Log.d("Sort by value", sortRecords);
                editor.putString("sort_records", sortRecords);
                editor.apply();
                dialog.dismiss();
                sortRecordsPopup.findViewById(R.id.sort_records_group).setVisibility(View.GONE);
                sortRecordsPopup.findViewById(R.id.sort_records_title).setVisibility(View.GONE);
                recreate();
            }
        });
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
        Button filterCancelButton = filterRecordsPopup.findViewById(R.id.filter_cancel_button);

        dialogBuilder.setView(filterRecordsPopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(false);

        filterCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

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