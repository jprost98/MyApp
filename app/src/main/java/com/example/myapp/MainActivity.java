package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private DatabaseReference rankingReference;
    private DatabaseReference achievementReference;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private String filterBy, sortRecords, sortVehicles;
    private final Record record = new Record();
    private final Vehicle vehicle = new Vehicle();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private RecyclerView vehiclesRecyclerView, recordsRecyclerView;
    private RecordAdapter recordAdapter;
    private VehicleAdapter vehicleAdapter;
    private VehicleDatabase vehicleDatabase;
    private RecordDatabase recordDatabase;

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;
    private int darkMode;
    private NavController navController;

    private int gained_exp;
    private int level;
    private int levelProgress;
    private int ach_count;
    private int records_count;
    private int vehicles_count;
    private final int ach_xp = 25;
    private final int record_xp = 5;
    private final int vehicle_xp = 15;
    private String username;
    private String email;

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

        addRankingEventListener(userRef);

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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        assert mUser != null;
        username = mUser.getDisplayName();
        email = mUser.getEmail();

        database = FirebaseDatabase.getInstance();
        rankingReference = database.getReference("users/" + mUser.getUid() + "/ranking");
        achievementReference = database.getReference("users/" + mUser.getUid() + "/achievements");
        userRef = database.getReference("users/" + mUser.getUid());

        userRef.child("user_info").child("version").setValue(getResources().getString(R.string.version));
    }

    private void addRankingEventListener(DatabaseReference userRef) {
        ValueEventListener expListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ach_count = Integer.parseInt(String.valueOf(dataSnapshot.child("achievements").getChildrenCount()));
                records_count = Integer.parseInt(String.valueOf(dataSnapshot.child("records").getChildrenCount()));
                vehicles_count = Integer.parseInt(String.valueOf(dataSnapshot.child("vehicles").getChildrenCount()));
                gained_exp = ach_count * ach_xp;
                gained_exp = gained_exp + (records_count * record_xp);
                gained_exp = gained_exp + (vehicles_count * vehicle_xp);
                rankingReference.child("experience").setValue(gained_exp);

                if (gained_exp >= 0 & gained_exp <= 100) {
                    rankingReference.child("level").setValue(0);
                    level = 0;
                    levelProgress = 100 - gained_exp;
                } else if (gained_exp > 100 & gained_exp <= 200) {
                    rankingReference.child("level").setValue(1);
                    level = 1;
                    levelProgress = 200 - gained_exp;
                } else if (gained_exp > 200 & gained_exp <= 300) {
                    rankingReference.child("level").setValue(2);
                    level = 2;
                    levelProgress = 300 - gained_exp;
                } else if (gained_exp > 300 & gained_exp <= 400) {
                    rankingReference.child("level").setValue(3);
                    level = 3;
                    levelProgress = 400 - gained_exp;
                } else if (gained_exp > 400 & gained_exp <= 500) {
                    rankingReference.child("level").setValue(4);
                    level = 4;
                    levelProgress = 500 - gained_exp;
                } else if (gained_exp > 500 & gained_exp <= 600) {
                    rankingReference.child("level").setValue(5);
                    level = 5;
                    levelProgress = 600 - gained_exp;
                } else if (gained_exp > 600 & gained_exp <= 700) {
                    rankingReference.child("level").setValue(6);
                    level = 6;
                    levelProgress = 700 - gained_exp;
                } else if (gained_exp > 700 & gained_exp <= 800) {
                    rankingReference.child("level").setValue(7);
                    level = 7;
                    levelProgress = 800 - gained_exp;
                } else if (gained_exp > 800 & gained_exp <= 900) {
                    rankingReference.child("level").setValue(8);
                    level = 8;
                    levelProgress = 900 - gained_exp;
                } else if (gained_exp > 900 & gained_exp < 1000) {
                    rankingReference.child("level").setValue(9);
                    level = 9;
                    levelProgress = 1000 - gained_exp;
                } else if (gained_exp > 1000) {
                    rankingReference.child("level").setValue(10);
                    level = 10;
                    levelProgress = 100;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ERROR", "loadExp:onCancelled", databaseError.toException());
            }
        };
        userRef.addValueEventListener(expListener);
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
            case R.id.profile:
                displayProfile();
                return true;
            default:
                return false;
        }
    }

    private void dimBackground(PopupWindow popupWindow) {
        View container = popupWindow.getContentView().getRootView();
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.6f;
        wm.updateViewLayout(container, p);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void displayProfile() {
        View view = View.inflate(this, R.layout.popup_profile, null);
        PopupWindow popupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.showAtLocation(findViewById(R.id.main_container), Gravity.CENTER, 0, 0);

        dimBackground(popupWindow);

        ImageView pEditProfile = view.findViewById(R.id.edit_profile);
        TextView pUsername = view.findViewById(R.id.profile_username);
        TextView pEmail = view.findViewById(R.id.profile_email);
        TextView pLevel = view.findViewById(R.id.profile_level);
        TextView pXP = view.findViewById(R.id.profile_xp);
        TextView pCurrentLevel = view.findViewById(R.id.current_level);
        TextView pNxtLevel = view.findViewById(R.id.nxt_level);
        TextView pAchEarned = view.findViewById(R.id.ach_earned);
        TextView pRecordsAdded = view.findViewById(R.id.records_added);
        TextView pVehiclesAdded = view.findViewById(R.id.vehicles_added);
        ProgressBar pProgress = view.findViewById(R.id.profile_xp_progress);
        pProgress.setMax(100);

        String levelText = "Level: " + level;
        String xpText = "XP: " + gained_exp;
        String currentLevelText = String.valueOf(level);
        String nxtLevelText = "10";
        String achEarnedTxt = "Achievements: " + ach_count;
        String recordsAddedTxt = "Records: " + records_count;
        String vehiclesAddedTxt = "Vehicles: " + vehicles_count;
        if (level < 10) nxtLevelText = String.valueOf(level + 1);
        if (level == 10) levelText = "Max Level";
        if (darkMode == 0) {
            pEditProfile.setImageDrawable(getDrawable(R.drawable.ic_edit_light));
        } else if (darkMode == 1) {
            pEditProfile.setImageDrawable(getDrawable(R.drawable.ic_edit_dark));
        }

        pUsername.setText(username);
        pEmail.setText(email);
        pLevel.setText(levelText);
        pXP.setText(xpText);
        pCurrentLevel.setText(currentLevelText);
        pNxtLevel.setText(nxtLevelText);
        pAchEarned.setText(achEarnedTxt);
        pRecordsAdded.setText(recordsAddedTxt);
        pVehiclesAdded.setText(vehiclesAddedTxt);
        pProgress.setProgress(100 - levelProgress);

        pEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateUserProfile();
            }
        });

        pAchEarned.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AchievementsActivity.class));
            }
        });
    }

    private void updateUserProfile() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View updateProfilePopup = getLayoutInflater().inflate(R.layout.popup_update_profile, null);

        TextInputLayout firstNameLayout, lastNameLayout, emailLayout, passwordLayout;

        firstNameLayout = updateProfilePopup.findViewById(R.id.update_first_name);
        lastNameLayout = updateProfilePopup.findViewById(R.id.update_last_name);
        emailLayout = updateProfilePopup.findViewById(R.id.update_email);
        passwordLayout = updateProfilePopup.findViewById(R.id.update_old_password);

        EditText firstName = firstNameLayout.getEditText();
        EditText lastName = lastNameLayout.getEditText();
        EditText newEmail = emailLayout.getEditText();
        EditText oldPassword = passwordLayout.getEditText();

        Button updateBtn = updateProfilePopup.findViewById(R.id.update_btn);
        Button cancelBtn = updateProfilePopup.findViewById(R.id.update_cancel_btn);

        String[] userName = mUser.getDisplayName().split(" ");
        String currentEmail = mUser.getEmail();
        final String[] firstNameTxt = new String[1];
        final String[] lastNameTxt = new String[1];
        final String[] newEmailTxt = new String[1];
        final String[] oldPasswordTxt = new String[1];
        final int[] errors = {0};

        firstName.setText(userName[0]);
        lastName.setText(userName[1]);
        newEmail.setText(currentEmail);

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

                oldPasswordTxt[0] = oldPassword.getText().toString().trim();
                if (oldPasswordTxt[0].isEmpty()) {
                    oldPassword.setError("Enter current password");
                    errors[0]++;
                } else {
                    assert currentEmail != null;
                    AuthCredential credential = EmailAuthProvider.getCredential(currentEmail, oldPasswordTxt[0]); // Current Login Credentials
                    mUser.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                if (!newEmailTxt[0].isEmpty()) {
                                    mUser.updateEmail(newEmailTxt[0]).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (!task.isSuccessful()) {
                                                Toast.makeText(dialogBuilder.getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(firstNameTxt[0] + " " + lastNameTxt[0])
                                        .build();
                                mUser.updateProfile(profileUpdates);
                                mAuth.signOut();
                                dialog.dismiss();
                                Toast.makeText(dialogBuilder.getContext(), "Profile successfully updated!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                            } else Toast.makeText(dialogBuilder.getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
    }

    private void sortVehicles() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
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
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
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
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
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