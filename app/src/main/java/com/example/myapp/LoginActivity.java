package com.example.myapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDao;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.User;
import com.example.myapp.data.UserDao;
import com.example.myapp.data.UserDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.ui.checkup.CheckupFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final ArrayList<Vehicle> localVehicleList = new ArrayList<>();
    private final ArrayList<Record> localRecordList = new ArrayList<>();
    private final ArrayList<Vehicle> remoteVehicleList = new ArrayList<>();
    private final ArrayList<Record> remoteRecordList = new ArrayList<>();
    private final ArrayList<Vehicle> oldRemoteVehicleList = new ArrayList<>();
    private final ArrayList<Record> oldRemoteRecordList = new ArrayList<>();
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private RecordDatabase recordDatabase;
    private RecordDao recordDao;
    private UserDatabase userDatabase;
    private UserDao userDao;
    private User user = new User();
    private final ArrayList<User> users = new ArrayList<>();
    private ActionBar actionBar;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private LinearLayout loadingView;
    private LinearLayout normalView;
    private EditText userEmailInput, userPasswordInput;
    private TextInputLayout userEmailLayout, userPasswordLayout;
    private TextView loadingText;
    private Button dummy_login_button, login_user_button, register_user_button, dummy_fp_button, forgot_password_button;
    private String filterRecordsBy, filterTasksBy, sortRecordsBy, sortVehiclesBy, sortTasksBy, theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Login");

        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        editor.putString("filter_by_value", "All");
        editor.putString("task_filter", "All");
        int darkMode = sharedPref.getInt("dark_mode", 0);
        if (darkMode == 0) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (darkMode == 1) {
            Log.d("Dark Mode", String.valueOf(darkMode));
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        /*
        theme = sharedPref.getString("theme_selection", "Default");
        if (theme.equals("Default")) setTheme(R.style.MyAppTheme);
        else if (theme.equals("Blue")) setTheme(com.google.android.material.R.style.Theme_Design);
        */

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_login);
        actionBar = getSupportActionBar();

        userEmailLayout = findViewById(R.id.login_email_input);
        userPasswordLayout = findViewById(R.id.login_password_input);

        normalView = findViewById(R.id.normal_login_view);
        loadingView = findViewById(R.id.loading_user_view);
        dummy_login_button = findViewById(R.id.dummy_login_btn);
        login_user_button = findViewById(R.id.login_btn);
        register_user_button = findViewById(R.id.register_btn);
        dummy_fp_button = findViewById(R.id.dummy_fp_btn);
        forgot_password_button = findViewById(R.id.forgot_password_btn);
        userEmailInput = userEmailLayout.getEditText();
        userPasswordInput = userPasswordLayout.getEditText();
        loadingText = findViewById(R.id.loading_text);

        normalView.setVisibility(View.VISIBLE);
        loadingView.setVisibility(View.GONE);
        userEmailInput.setVisibility(View.GONE);
        userPasswordInput.setVisibility(View.GONE);
        dummy_login_button.setVisibility(View.VISIBLE);
        login_user_button.setVisibility(View.GONE);
        register_user_button.setVisibility(View.VISIBLE);
        dummy_fp_button.setVisibility(View.VISIBLE);
        forgot_password_button.setVisibility(View.GONE);

        recordDatabase = Room.databaseBuilder(getApplicationContext(), RecordDatabase.class, "records").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        userDatabase = Room.databaseBuilder(getApplicationContext(), UserDatabase.class, "users").allowMainThreadQueries().fallbackToDestructiveMigration().build();
        users.clear();
        users.addAll(userDatabase.userDao().getUser());
        if (users.size() > 0) user = users.get(0);

        initFirebase();

        dummy_login_button.setOnClickListener(view -> {
            dummy_fp_button.setVisibility(View.GONE);
            register_user_button.setVisibility(View.GONE);
            dummy_login_button.setVisibility(View.GONE);
            userEmailInput.setVisibility(View.VISIBLE);
            userPasswordInput.setVisibility(View.VISIBLE);
            login_user_button.setVisibility(View.VISIBLE);
        });
        login_user_button.setOnClickListener(v -> {
            loginUser();
        });
        register_user_button.setOnClickListener(v -> {
            registerUser();
        });
        dummy_fp_button.setOnClickListener(view -> {
            dummy_login_button.setVisibility(View.GONE);
            register_user_button.setVisibility(View.GONE);
            dummy_fp_button.setVisibility(View.GONE);
            userEmailInput.setVisibility(View.VISIBLE);
            forgot_password_button.setVisibility(View.VISIBLE);
        });
        forgot_password_button.setOnClickListener(view -> {
            forgotPassword();
        });
    }

    @Override
    public void onBackPressed() {
        recreate();
    }

    private void forgotPassword() {
        String userEmail;
        userEmailInput = userEmailLayout.getEditText();
        userEmail = userEmailInput.getText().toString().trim();

        if (userEmail.isEmpty()) {
            userEmailInput.setError("Please enter a valid email address");
        } else {
            mAuth.sendPasswordResetEmail(userEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    userEmailInput.setText("");
                    Toast.makeText(LoginActivity.this, "A reset link has been sent to the email you provided (Check Spam)", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void continueToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void loadData() {
        filterRecordsBy = sharedPref.getString("filter_by_value", "All");
        filterTasksBy = sharedPref.getString("task_filter", "All");
        sortRecordsBy = sharedPref.getString("sort_records", "date_desc");
        sortVehiclesBy = sharedPref.getString("sort_vehicles", "year_desc");
        sortTasksBy = sharedPref.getString("sort_tasks", "date_desc");

        userRef.child(mUser.getUid()).child("user_info").child("first_name").setValue(user.getFirstName());
        userRef.child(mUser.getUid()).child("user_info").child("last_name").setValue(user.getLastName());
        userRef.child(mUser.getUid()).child("user_info").child("email").setValue(user.getEmail());
        userRef.child(mUser.getUid()).child("user_info").child("uid").setValue(user.getFbUserId());
        userRef.child(mUser.getUid()).child("user_info").child("last_login").setValue(SimpleDateFormat.getDateInstance().format(Calendar.getInstance().getTime()));

        userRef.child(mUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().child("settings").child("dark_mode").exists()) {
                        if (Objects.requireNonNull(task.getResult().child("settings").child("dark_mode").getValue()).toString().equals("0")) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            editor.putInt("dark_mode", 0);
                            editor.apply();
                        }
                        if (Objects.requireNonNull(task.getResult().child("settings").child("dark_mode").getValue()).toString().equals("1")) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            editor.putInt("dark_mode", 1);
                            editor.apply();
                        }
                    }
                    if (task.getResult().child("settings").child("filter_records_by").exists()) editor.putString("filter_by_value", task.getResult().child("settings").child("filter_records_by").getValue(String.class));
                    else {
                        userRef.child(mUser.getUid()).child("settings").child("filter_records_by").setValue("All");
                        editor.putString("filter_by_value", "All");
                    }
                    if (task.getResult().child("settings").child("filter_tasks_by").exists()) editor.putString("task_filter", task.getResult().child("settings").child("filter_tasks_by").getValue(String.class));
                    else {
                        userRef.child(mUser.getUid()).child("settings").child("filter_tasks_by").setValue("All");
                        editor.putString("task_filter", "All");
                    }
                    if (task.getResult().child("settings").child("sort_records_by").exists()) editor.putString("sort_records", task.getResult().child("settings").child("sort_records_by").getValue(String.class));
                    else {
                        userRef.child(mUser.getUid()).child("settings").child("sort_records_by").setValue("date_desc");
                        editor.putString("sort_records", "date_desc");
                    }
                    if (task.getResult().child("settings").child("sort_vehicles_by").exists()) editor.putString("sort_vehicles", task.getResult().child("settings").child("sort_vehicles_by").getValue(String.class));
                    else {
                        userRef.child(mUser.getUid()).child("settings").child("sort_vehicles_by").setValue("year_desc");
                        editor.putString("sort_vehicles", "year_desc");
                    }
                    if (task.getResult().child("settings").child("sort_tasks_by").exists()) editor.putString("sort_tasks", task.getResult().child("sort_tasks_by").child("settings").getValue(String.class));
                    else {
                        userRef.child(mUser.getUid()).child("settings").child("sort_tasks_by").setValue("date_desc");
                        editor.putString("sort_tasks", "date_desc");
                    }
                }
            }
        });

        Toast.makeText(this, "Welcome " + Objects.requireNonNull(mUser.getDisplayName()).split(" ")[0], Toast.LENGTH_SHORT).show();
        normalView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);
        ProgressBar progressBar = loadingView.findViewById(R.id.progressBar);

        oldRemoteVehicleList.clear();
        oldRemoteRecordList.clear();
        localVehicleList.clear();
        localRecordList.clear();
        remoteVehicleList.clear();
        remoteRecordList.clear();
        localVehicleList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        localRecordList.addAll(recordDatabase.recordDao().getAllRecords());

        userRef.child(mUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().child("user_info").child("updated").exists()) {
                        for (DataSnapshot dataSnapshot : task.getResult().child("Vehicles").getChildren()) {
                            oldRemoteVehicleList.add(dataSnapshot.getValue(Vehicle.class));
                        }
                        for (DataSnapshot dataSnapshot : task.getResult().child("Records").getChildren()) {
                            oldRemoteRecordList.add(dataSnapshot.getValue(Record.class));
                        }
                        for (Record record:oldRemoteRecordList) {
                            String oldDate;
                            String newDate;
                            SimpleDateFormat oldFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                            SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                            oldDate = record.getDate();
                            try {
                                newDate = newFormat.format(Objects.requireNonNull(oldFormat.parse(oldDate)));
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            record.setDate(newDate);
                        }
                        if (oldRemoteVehicleList.size() > 0) userRef.child(mUser.getUid()).child("vehicles").setValue(oldRemoteVehicleList);
                        if (oldRemoteRecordList.size() > 0) userRef.child(mUser.getUid()).child("records").setValue(oldRemoteRecordList);
                        userRef.child(mUser.getUid()).child("user_info").child("updated").setValue("yes");
                    }
                    if (!task.getResult().child("user_info").child("records_tasks_updated").exists()) {
                        ArrayList<com.example.myapp.data.Task> tasks = new ArrayList<>();
                        for (DataSnapshot dataSnapshot : task.getResult().child("vehicles").getChildren()) {
                            remoteVehicleList.add(dataSnapshot.getValue(Vehicle.class));
                        }
                        for (DataSnapshot dataSnapshot : task.getResult().child("records").getChildren()) {
                            remoteRecordList.add(dataSnapshot.getValue(Record.class));
                        }
                        for (DataSnapshot dataSnapshot : task.getResult().child("tasks").getChildren()) {
                            tasks.add(dataSnapshot.getValue(com.example.myapp.data.Task.class));
                        }

                        for (Vehicle vehicle:remoteVehicleList) {
                            for (Record record:remoteRecordList) {
                                if (record.getVehicle().equals(vehicle.vehicleTitle())) {
                                    record.setVehicle(String.valueOf(vehicle.getVehicleId()));
                                }
                            }
                            for (com.example.myapp.data.Task task1:tasks) {
                                if (task1.getTaskVehicle().equals(vehicle.vehicleTitle())) {
                                    task1.setTaskVehicle(String.valueOf(vehicle.getVehicleId()));
                                }
                            }
                        }
                        userRef.child(mUser.getUid()).child("records").setValue(remoteRecordList);
                        userRef.child(mUser.getUid()).child("tasks").setValue(tasks);
                        userRef.child(mUser.getUid()).child("user_info").child("records_tasks_updated").setValue("yes");
                    }

                    userRef.child(mUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            Long backupAmount = task.getResult().child("backups").getChildrenCount();
                            String date = String.valueOf(Calendar.getInstance().getTime());
                            ArrayList<Object> backups = new ArrayList<>();
                            ArrayList<Record> recordBackups = new ArrayList<>();
                            ArrayList<Vehicle> vehicleBackups = new ArrayList<>();
                            ArrayList<com.example.myapp.data.Task> taskBackups = new ArrayList<>();
                            ArrayList<com.example.myapp.data.Task> taskArrayList = new ArrayList<>();

                            for (DataSnapshot dataSnapshot : task.getResult().child("vehicles").getChildren()) {
                                remoteVehicleList.add(dataSnapshot.getValue(Vehicle.class));
                            }
                            for (DataSnapshot dataSnapshot : task.getResult().child("records").getChildren()) {
                                remoteRecordList.add(dataSnapshot.getValue(Record.class));
                            }
                            for (DataSnapshot dataSnapshot : task.getResult().child("backups").child(String.valueOf(backupAmount - 1)).child("records").getChildren()) {
                                recordBackups.add(dataSnapshot.getValue(Record.class));
                            }
                            for (DataSnapshot dataSnapshot : task.getResult().child("backups").child(String.valueOf(backupAmount - 1)).child("vehicles").getChildren()) {
                                vehicleBackups.add(dataSnapshot.getValue(Vehicle.class));
                            }
                            for (DataSnapshot dataSnapshot : task.getResult().child("backups").child(String.valueOf(backupAmount - 1)).child("tasks").getChildren()) {
                                taskBackups.add(dataSnapshot.getValue(com.example.myapp.data.Task.class));
                            }
                            for (DataSnapshot dataSnapshot : task.getResult().child("backups").getChildren()) {
                                backups.add(dataSnapshot.getValue());
                            }
                            for (DataSnapshot dataSnapshot : task.getResult().child("tasks").getChildren()) {
                                taskArrayList.add(dataSnapshot.getValue(com.example.myapp.data.Task.class));
                            }

                            /*
                            try {
                                checkNotifications(taskArrayList);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            */

                            if (!remoteVehicleList.toString().equals(vehicleBackups.toString()) || !remoteRecordList.toString().equals(recordBackups.toString()) || !taskArrayList.toString().equals(taskBackups.toString())) {
                                if (backupAmount > 9) {
                                    backups.remove(0);
                                    userRef.child(mUser.getUid()).child("backups").setValue(backups);
                                    backupAmount = Long.parseLong(String.valueOf(backups.size()));
                                }

                                userRef.child(mUser.getUid()).child("backups").child(String.valueOf(backupAmount)).child("date").setValue(date);
                                userRef.child(mUser.getUid()).child("backups").child(String.valueOf(backupAmount)).child("records").setValue(remoteRecordList);
                                userRef.child(mUser.getUid()).child("backups").child(String.valueOf(backupAmount)).child("vehicles").setValue(remoteVehicleList);
                                userRef.child(mUser.getUid()).child("backups").child(String.valueOf(backupAmount)).child("tasks").setValue(taskArrayList);
                            }

                            recordDatabase.recordDao().deleteAllRecords();
                            for (Record remoteRecord:remoteRecordList) {
                                recordDatabase.recordDao().addRecord(remoteRecord);
                            }
                            vehicleDatabase.vehicleDao().deleteAllVehicles();
                            for (Vehicle remoteVehicle:remoteVehicleList) {
                                vehicleDatabase.vehicleDao().addVehicle(remoteVehicle);
                            }
                            continueToMainActivity();
                        }
                    });
                } else {
                    Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    recreate();
                }
            }
        });
    }

    private void checkNotifications(ArrayList<com.example.myapp.data.Task> taskArrayList) throws ParseException {
        ArrayList<com.example.myapp.data.Task> tasksDueToday = new ArrayList<>();
        ArrayList<com.example.myapp.data.Task> tasksDueSoon = new ArrayList<>();
        ArrayList<com.example.myapp.data.Task> tasksOverdue = new ArrayList<>();

        if (!taskArrayList.isEmpty()) {
            createNotificationChannel();
            for (com.example.myapp.data.Task task:taskArrayList) {
                if (task.getTaskType().equals("single")) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date taskDate = format.parse(task.getTaskDueDate());
                    Date currentDate;
                    try {
                        currentDate = format.parse(format.format(Calendar.getInstance().getTime()));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    assert currentDate != null;
                    assert taskDate != null;
                    long different = taskDate.getTime() - currentDate.getTime();
                    long secondsInMilli = 1000;
                    long minutesInMilli = secondsInMilli * 60;
                    long hoursInMilli = minutesInMilli * 60;
                    long daysInMilli = hoursInMilli * 24;
                    long elapsedDays = different / daysInMilli;

                    if (elapsedDays == 0) tasksDueToday.add(task);
                    else if (elapsedDays < 0) tasksOverdue.add(task);
                    else if (elapsedDays < 6) tasksDueSoon.add(task);

                } else if (task.getTaskType().equals("recurring") & !task.getTaskFrequency().contains("miles")) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date currentDate;
                    Date taskDate;
                    String frequencyAmount = task.getTaskFrequency().split(" ")[0];
                    String frequencyType = task.getTaskFrequency().split(" ")[1];
                    Calendar calendar = Calendar.getInstance();
                    try {
                        currentDate = format.parse(format.format(Calendar.getInstance().getTime()));
                        if (task.getTaskLastDone() == null) {
                            taskDate = format.parse(format.format(task.getEntryTime()));
                        } else {
                            taskDate = format.parse(task.getTaskLastDone());
                        }
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    assert currentDate != null;
                    assert taskDate != null;
                    calendar.setTime(taskDate);
                    calendar.setFirstDayOfWeek(Calendar.SUNDAY);
                    if (frequencyType.equals("days")) calendar.add(Calendar.DATE, Integer.parseInt(frequencyAmount));
                    if (frequencyType.equals("weeks")) calendar.add(Calendar.WEEK_OF_YEAR, Integer.parseInt(frequencyAmount));
                    if (frequencyType.equals("months")) calendar.add(Calendar.MONTH, Integer.parseInt(frequencyAmount));
                    if (frequencyType.equals("years")) calendar.add(Calendar.YEAR, Integer.parseInt(frequencyAmount));
                    long different = calendar.getTimeInMillis() - currentDate.getTime();
                    long secondsInMilli = 1000;
                    long minutesInMilli = secondsInMilli * 60;
                    long hoursInMilli = minutesInMilli * 60;
                    long daysInMilli = hoursInMilli * 24;
                    long elapsedDays = different / daysInMilli;

                    if (elapsedDays == 0) tasksDueToday.add(task);
                    else if (elapsedDays < 0) tasksOverdue.add(task);
                    else if (elapsedDays < 6) tasksDueSoon.add(task);

                } else if (task.getTaskType().equals("recurring") & task.getTaskFrequency().contains("miles")) {
                    com.example.myapp.data.Task thisTask = task;
                    userRef.child(mUser.getUid()).child("records").orderByChild("odometer").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<DataSnapshot> task) {
                            if (task.isSuccessful()) {
                                String mileageFrequency = thisTask.getTaskFrequency().split(" ")[0];
                                String lastRecordMileage = null;
                                String dueMileage;
                                int diffMiles;
                                ArrayList<Record> records = new ArrayList<>();

                                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                                    records.add(snapshot.getValue(Record.class));
                                }
                                Collections.reverse(records);

                                for (Record record : records) {
                                    if (record.getVehicle().equals(thisTask.getTaskVehicle())) {
                                        lastRecordMileage = record.getOdometer();
                                        break;
                                    }
                                }

                                assert lastRecordMileage != null;
                                dueMileage = String.valueOf(Integer.parseInt(lastRecordMileage) + Integer.parseInt(mileageFrequency));
                                diffMiles = Integer.parseInt(dueMileage) - Integer.parseInt(lastRecordMileage);

                                if (diffMiles == 0) tasksDueToday.add(thisTask);
                                else if (diffMiles < 0) tasksOverdue.add(thisTask);
                                else if (diffMiles <= 1000) tasksDueSoon.add(thisTask);
                            }
                        }
                    });
                }
            }
            Log.d("Tasks Due Now", tasksDueToday.toString());
            Log.d("Tasks Overdue", tasksOverdue.toString());
            Log.d("Tasks Due Soon", tasksDueSoon.toString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!tasksDueToday.isEmpty()) {
                    if (tasksDueToday.size() == 1) createNotification("You have " + tasksDueToday.size() + " task due today.", 0);
                    else createNotification("You have " + tasksDueToday.size() + " tasks due today.", 0);
                }
                if (!tasksOverdue.isEmpty()) {
                    if (tasksOverdue.size() == 1) createNotification("You have " + tasksOverdue.size() + " task overdue.", 1);
                    else createNotification("You have " + tasksOverdue.size() + " tasks overdue.", 1);
                }
                if (!tasksDueSoon.isEmpty()) {
                    if (tasksDueSoon.size() == 1) createNotification("You have " + tasksDueSoon.size() + " task due soon.", 2);
                    else createNotification("You have " + tasksDueSoon.size() + " tasks due soon.", 2);
                }
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.task_channel_name);
            String description = getString(R.string.task_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("Tasks", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createNotification(String description, int id) {
        Intent intent = new Intent(this, CheckupFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Tasks")
                .setSmallIcon(R.drawable.ic_task_notification_lt)
                .setContentTitle("Tasks")
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //.setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    //Initializes Firebase Authentication
    private void initFirebase() {
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser!=null) {
            setPersistence();
            userRef = database.getReference("users");
            if (mUser.isEmailVerified()) {
                userRef.keepSynced(true);
                userRef.child(mUser.getUid()).child("user_info").child("email_verified").setValue("true");
                loadData();
            } else if (!mUser.isEmailVerified()){
                userRef.child(mUser.getUid()).child("user_info").child("email_verified").setValue("false");
                Toast.makeText(this, "Your email is not verified. Check your email (Spam too!)", Toast.LENGTH_LONG).show();
                mUser.sendEmailVerification();
                mAuth.signOut();
            }
        }
    }

    private void setPersistence() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
            try {
                database.setPersistenceEnabled(true);
            } catch (Exception e) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Log in user
    private void loginUser() {
        String userEmail, userPassword;
        userEmailInput = userEmailLayout.getEditText();
        userPasswordInput = userPasswordLayout.getEditText();
        userEmail = userEmailInput.getText().toString().trim();
        userPassword = userPasswordInput.getText().toString().trim();
        int errors = 0;

        if (userEmail.isEmpty()) {
            userEmailInput.setError("Please enter a valid email address");
            errors++;
        }
        if (userPassword.isEmpty()) {
            userPasswordInput.setError("Please enter a valid password");
            errors++;
        }
        if (errors == 0) {
            mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            mUser = mAuth.getCurrentUser();
                            assert mUser != null;
                            if (!mUser.isEmailVerified()) {
                                userEmailInput.setText("");
                                userPasswordInput.setText("");
                                Toast.makeText(this, "Your email is not verified yet. Check your email (Spam too!)", Toast.LENGTH_LONG).show();
                                mUser.sendEmailVerification();
                                mAuth.signOut();
                                recreate();
                            } else {
                                if (users.size() == 1) {
                                    user = users.get(0);
                                    if (!mUser.getUid().equals(user.getFbUserId())) {
                                        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog))
                                                .setTitle("Warning")
                                                .setMessage("There is another user using this device. This will sign them out.")
                                                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        users.clear();
                                                        recordDatabase.recordDao().deleteAllRecords();
                                                        vehicleDatabase.vehicleDao().deleteAllVehicles();
                                                        userDatabase.userDao().deleteUser();
                                                        String[] userName = Objects.requireNonNull(mUser.getDisplayName()).split(" ");
                                                        user.setFbUserId(mUser.getUid());
                                                        user.setFirstName(userName[0]);
                                                        user.setLastName(userName[1]);
                                                        user.setEmail(mUser.getEmail());
                                                        userDatabase.userDao().addUser(user);
                                                        loadData();
                                                    }
                                                })
                                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mAuth.signOut();
                                                        userEmailInput.setText("");
                                                        userPasswordInput.setText("");
                                                        recreate();
                                                    }
                                                })
                                                .setIcon(R.drawable.ic_round_warning_24)
                                                .show();
                                    } else {
                                        users.clear();
                                        String[] userName = Objects.requireNonNull(mUser.getDisplayName()).split(" ");
                                        user.setFbUserId(mUser.getUid());
                                        user.setFirstName(userName[0]);
                                        user.setLastName(userName[1]);
                                        user.setEmail(mUser.getEmail());
                                        userDatabase.userDao().updateUser(user);
                                        loadData();
                                    }
                                } else if (users.size() == 0) {
                                    users.clear();
                                    String[] userName = Objects.requireNonNull(mUser.getDisplayName()).split(" ");
                                    user.setFbUserId(mUser.getUid());
                                    user.setFirstName(userName[0]);
                                    user.setLastName(userName[1]);
                                    user.setEmail(mUser.getEmail());
                                    userDatabase.userDao().addUser(user);
                                    loadData();
                                }
                            }
                        } else {
                            Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            //Toast.makeText(this, "Login failed. Please check your connection and try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    //Takes user to registration page
    private void registerUser() {
        startActivity(new Intent(this, RegistrationActivity.class));
    }
}