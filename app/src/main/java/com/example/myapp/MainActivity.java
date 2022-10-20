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
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
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
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        int darkMode = sharedPref.getInt("dark_mode", 0);
        int themePref = sharedPref.getInt("theme_pref", 0);
        filterBy = sharedPref.getString("filter_by_value", "All");
        Log.d("Filter Value", filterBy);
        if (darkMode == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        if (themePref == 0) this.setTheme(R.style.DefaultTheme);
        else if (themePref == 1) this.setTheme(R.style.RedTheme);
        else if (themePref == 2) this.setTheme(R.style.BlueTheme);
        else if (themePref == 3) this.setTheme(R.style.GreenTheme);
        else if (themePref == 4) this.setTheme(R.style.GreyscaleTheme);
        Log.d("Theme", String.valueOf(themePref));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        TextView navUsername = navigationView.findViewById(R.id.nav_view_username);
        TextView navEmail = navigationView.findViewById(R.id.nav_view_email);
        navUsername.setText(mUser.getDisplayName());
        navEmail.setText(mUser.getEmail());
        ImageView headerBackground = headerView.findViewById(R.id.imageView);

        if (themePref == 0) headerBackground.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.default_theme_img));
        else if (themePref == 1) headerBackground.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.red_theme_img));
        else if (themePref == 2) headerBackground.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.blue_theme_img));
        else if (themePref == 3) headerBackground.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.green_theme_img));
        else if (themePref == 4) headerBackground.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.greyscale_theme_img));

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == R.id.nav_settings) {
                binding.appBarMain.fab.hide();
            } else binding.appBarMain.fab.show();
        });

        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (navController.getCurrentDestination().getId() == R.id.nav_home) {
                    vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
                    vehicleArrayList.clear();
                    vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
                    if (!vehicleArrayList.isEmpty()) {
                        startActivity(new Intent(MainActivity.this, AddRecord.class));
                    } else Snackbar.make(MainActivity.this.findViewById(R.id.content_constraint), "Need to add vehicles before making maintenance records.", Snackbar.LENGTH_LONG).show();
                } else if (navController.getCurrentDestination().getId() == R.id.nav_vehicles) {
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
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
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

        MenuItem searchItem = menu.findItem(R.id.app_bar_search);

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == R.id.nav_home) {
                menu.getItem(0).setVisible(true);
                menu.getItem(1).setVisible(true);
                getSupportActionBar().setTitle("Maintenance");
            } else if (navDestination.getId() == R.id.nav_vehicles){
                menu.getItem(0).setVisible(true);
                menu.getItem(1).setVisible(false);
                getSupportActionBar().setTitle("Vehicles");
            } else if (navDestination.getId() == R.id.nav_settings) {
                menu.getItem(0).setVisible(false);
                menu.getItem(1).setVisible(false);
                getSupportActionBar().setTitle("Settings");
            }
        });
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
                if (navController.getCurrentDestination().getId() == R.id.nav_home) {
                    if (!vehicleDatabase.vehicleDao().getAllVehicles().isEmpty()) filterRecords();
                    else Toast.makeText(this, "Nothing to filter", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return false;
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