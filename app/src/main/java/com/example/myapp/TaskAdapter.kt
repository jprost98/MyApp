package com.example.myapp;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDao;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final ArrayList<Task> taskArrayList;
    private final ArrayList<Vehicle> vehicleArrayList;
    private final static int FADE_DURATION = 350;
    private final Context context;
    private final View parentView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;

    private Task task = new Task();

    public TaskAdapter(ArrayList<Task> taskArrayList, Context context, View parentView, ArrayList<Vehicle> vehicleArrayList) {
        this.taskArrayList = taskArrayList;
        this.context = context;
        this.parentView = parentView;
        this.vehicleArrayList = vehicleArrayList;
        initFirebase();
    }

    @Override
    public void onBindViewHolder(@NonNull TaskAdapter.TaskViewHolder holder, int position) {
        task = taskArrayList.get(holder.getAdapterPosition());
        Date displayDate = null;
        String taskDate = null;
        if (task.getTaskType().equals("single")) {
            holder.task_view_due_date_layout.setVisibility(View.VISIBLE);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                displayDate = format.parse(task.getTaskDueDate());
                assert displayDate != null;
                taskDate = SimpleDateFormat.getDateInstance().format(displayDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            holder.task_view_frequency_layout.setVisibility(View.VISIBLE);
            if (task.getTaskLastDone() != null){
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    displayDate = format.parse(task.getTaskLastDone());
                    assert displayDate != null;
                    taskDate = SimpleDateFormat.getDateInstance().format(displayDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                holder.taskLastDone.setText(taskDate);
            } else if (task.getTaskLastDone() == null) {
                holder.taskLastDone.setText("Never");
            }
        }

        holder.taskView.setOnCheckedChangeListener(new MaterialCardView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MaterialCardView card, boolean isChecked) {
                task = taskArrayList.get(holder.getAdapterPosition());
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Record record = new Record();
                RecordDatabase recordDatabase = Room.databaseBuilder(holder.itemView.getContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
                RecordDao recordDao = recordDatabase.recordDao();
                ArrayList<Record> recordArrayList = new ArrayList<>(recordDao.getAllRecords());

                if (holder.taskView.isChecked()) {
                    if (task.getTaskType().equals("recurring")) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(holder.itemView.getContext());

                        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        final View view = li.inflate(R.layout.mileage_input, null);
                        TextInputLayout textInputLayout = view.findViewById(R.id.task_complete_mileage);
                        EditText odometerReading = textInputLayout.getEditText();

                        builder.setView(view);
                        builder.setPositiveButton("Set Mileage", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if (odometerReading.getText().toString().equals("") || odometerReading.getText().toString().isEmpty()) {
                                    holder.taskView.setChecked(false);
                                    Snackbar.make(context, holder.itemView, "Odometer reading cannot be empty.", Snackbar.LENGTH_SHORT)
                                            .setAnchorView(holder.itemView.getRootView().findViewById(R.id.bottom_nav_view))
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .setAction("Try Again", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    holder.taskView.setChecked(true);
                                                }
                                            })
                                            .show();
                                    dialog.cancel();
                                } else {
                                    Snackbar.make(context, holder.itemView, "Task completed! Task added to records.", Snackbar.LENGTH_SHORT)
                                            .setAnchorView(holder.itemView.getRootView().findViewById(R.id.bottom_nav_view))
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .show();
                                    Log.d("Completed Task", task.toString());
                                    record.setTitle(task.getTaskName());
                                    record.setDate(format.format(Calendar.getInstance().getTime()));
                                    record.setOdometer(odometerReading.getText().toString());
                                    record.setVehicle(task.getTaskVehicle());
                                    record.setDescription(task.getTaskNotes());
                                    record.setEntryTime(Calendar.getInstance().getTimeInMillis());

                                    recordDao.addRecord(record);
                                    recordArrayList.clear();
                                    recordArrayList.addAll(recordDao.getAllRecords());
                                    task.setTaskLastDone(format.format(Calendar.getInstance().getTime()));
                                    Log.d("Tasks", taskArrayList.toString());
                                    Log.d("Records", recordArrayList.toString());
                                    userRef.child("tasks").setValue(taskArrayList);
                                    userRef.child("records").setValue(recordArrayList);
                                    holder.taskView.setChecked(false);
                                }
                            }
                        });

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                holder.taskView.setChecked(false);
                                dialog.cancel();
                            }
                        });
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                holder.taskView.setChecked(false);
                            }
                        });
                        builder.show();
                    } else if (task.getTaskType().equals("single")) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(holder.itemView.getContext());

                        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        final View view = li.inflate(R.layout.mileage_input, null);
                        TextInputLayout textInputLayout = view.findViewById(R.id.task_complete_mileage);
                        EditText odometerReading = textInputLayout.getEditText();

                        builder.setView(view);
                        builder.setPositiveButton("Set Mileage", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (odometerReading.getText().toString().equals("") || odometerReading.getText().toString().isEmpty()) {
                                    holder.taskView.setChecked(false);
                                    Snackbar.make(context, holder.itemView, "Odometer reading cannot be empty.", Snackbar.LENGTH_SHORT)
                                            .setAnchorView(holder.itemView.getRootView().findViewById(R.id.bottom_nav_view))
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .setAction("Try Again", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    holder.taskView.setChecked(true);
                                                }
                                            })
                                            .show();
                                } else {
                                    Snackbar.make(context, holder.itemView, "Task completed! Task added to records.", Snackbar.LENGTH_SHORT)
                                            .setAnchorView(holder.itemView.getRootView().findViewById(R.id.bottom_nav_view))
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .show();
                                    record.setTitle(task.getTaskName());
                                    record.setDate(format.format(Calendar.getInstance().getTime()));
                                    record.setOdometer(odometerReading.getText().toString());
                                    record.setVehicle(task.getTaskVehicle());
                                    record.setDescription(task.getTaskNotes());
                                    record.setEntryTime(Calendar.getInstance().getTimeInMillis());

                                    recordDao.addRecord(record);
                                    recordArrayList.clear();
                                    recordArrayList.addAll(recordDao.getAllRecords());
                                    taskArrayList.remove(task);
                                    userRef.child("records").setValue(recordArrayList);
                                    userRef.child("tasks").setValue(taskArrayList);
                                    holder.taskView.setChecked(false);
                                    dialog.dismiss();
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                holder.taskView.setChecked(false);
                                dialog.cancel();
                            }
                        });
                        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                holder.taskView.setChecked(false);
                            }
                        });
                        builder.show();
                    }
                }
            }
        });

        String vehicleTitle = null;
        for (Vehicle vehicle:vehicleArrayList) {
            if (String.valueOf(vehicle.getVehicleId()).equals(taskArrayList.get(position).getTaskVehicle())) {
                vehicleTitle = vehicle.vehicleTitle();
            }
        }

        holder.taskName.setText(task.getTaskName());
        holder.taskVehicle.setText(vehicleTitle);
        holder.taskDueDate.setText(taskDate);
        holder.taskFrequency.setText(task.getTaskFrequency());
        holder.taskNotes.setText(task.getTaskNotes());

        if (task.getTaskNotes().equals("") || task.getTaskNotes().isEmpty()) {
            holder.task_view_notes_layout.setVisibility(View.GONE);
        } else {
            holder.task_view_notes_layout.setVisibility(View.VISIBLE);
        }

        if (task.getTaskType().equals("single")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate;
            try {
                currentDate = format.parse(format.format(Calendar.getInstance().getTime()));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            assert displayDate != null;
            assert currentDate != null;
            long different = displayDate.getTime() - currentDate.getTime();
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;
            long elapsedDays = different / daysInMilli;

            if (elapsedDays == 0) {
                holder.taskDueDate.setText(taskDate + " - DUE TODAY");
                holder.taskDueDate.setTextColor(context.getResources().getColor(R.color.task_due_today));
            } else if (elapsedDays < 0) {
                if (elapsedDays == -1) {
                    holder.taskDueDate.setText(taskDate + " - OVERDUE BY " + Math.abs(elapsedDays) + " DAY");
                    holder.taskDueDate.setTextColor(context.getResources().getColor(R.color.task_overdue));
                } else {
                    holder.taskDueDate.setText(taskDate + " - OVERDUE BY " + Math.abs(elapsedDays) + " DAYS");
                    holder.taskDueDate.setTextColor(context.getResources().getColor(R.color.task_overdue));
                }
            } else if (elapsedDays < 6) {
                if (elapsedDays == 1) {
                    holder.taskDueDate.setText(taskDate + " - DUE IN " + elapsedDays + " DAY");
                    holder.taskDueDate.setTextColor(context.getResources().getColor(R.color.task_due_today));
                } else {
                    holder.taskDueDate.setText(taskDate + " - DUE IN " + elapsedDays + " DAYS");
                    holder.taskDueDate.setTextColor(context.getResources().getColor(R.color.task_due_today));
                }
            }
        } else if (task.getTaskType().equals("recurring") & !task.getTaskFrequency().contains("miles")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate;
            String frequencyAmount = task.getTaskFrequency().split(" ")[0];
            String frequencyType = task.getTaskFrequency().split(" ")[1];
            Calendar calendar = Calendar.getInstance();
            try {
                currentDate = format.parse(format.format(Calendar.getInstance().getTime()));
                if (task.getTaskLastDone() == null) {
                    displayDate = format.parse(format.format(task.getEntryTime()));
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            assert currentDate != null;
            assert displayDate != null;
            calendar.setTime(displayDate);
            calendar.setFirstDayOfWeek(Calendar.SUNDAY);
            if (frequencyType.equals("days")) calendar.add(Calendar.DATE, Integer.parseInt(frequencyAmount));
            if (frequencyType.equals("weeks")) calendar.add(Calendar.WEEK_OF_YEAR, Integer.parseInt(frequencyAmount));
            if (frequencyType.equals("months")) calendar.add(Calendar.MONTH, Integer.parseInt(frequencyAmount));
            if (frequencyType.equals("years")) calendar.add(Calendar.YEAR, Integer.parseInt(frequencyAmount));
            if (task.getTaskDueDate() == null) {
                task.setTaskDueDate(format.format(calendar.getTime()));
                userRef.child("tasks").setValue(taskArrayList);
            } else if (!task.getTaskDueDate().equals(format.format(calendar.getTime()))){
                task.setTaskDueDate(format.format(calendar.getTime()));
                userRef.child("tasks").setValue(taskArrayList);
            }
            long different = calendar.getTimeInMillis() - currentDate.getTime();
            long secondsInMilli = 1000;
            long minutesInMilli = secondsInMilli * 60;
            long hoursInMilli = minutesInMilli * 60;
            long daysInMilli = hoursInMilli * 24;
            long elapsedDays = different / daysInMilli;

            if (elapsedDays == 0) {
                if (task.getTaskLastDone() == null) holder.taskLastDone.setText("Never - DUE TODAY");
                else holder.taskLastDone.setText(taskDate + " - DUE TODAY");
                holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_due_today));
            } else if (elapsedDays < 0) {
                if (elapsedDays == -1) {
                    if (task.getTaskLastDone() == null) holder.taskLastDone.setText("Never - - OVERDUE BY " + Math.abs(elapsedDays) + " DAY");
                    else holder.taskLastDone.setText(taskDate + " - OVERDUE BY " + Math.abs(elapsedDays) + " DAY");
                    holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_overdue));
                } else {
                    if (task.getTaskLastDone() == null) holder.taskLastDone.setText("Never - - OVERDUE BY " + Math.abs(elapsedDays) + " DAYS");
                    else holder.taskLastDone.setText(taskDate + " - OVERDUE BY " + Math.abs(elapsedDays) + " DAYS");
                    holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_overdue));
                }
            } else if (elapsedDays < 6) {
                if (elapsedDays == 1) {
                    if (task.getTaskLastDone() == null) holder.taskLastDone.setText("Never - DUE IN " + elapsedDays + " DAY");
                    else holder.taskLastDone.setText(taskDate + " - DUE IN " + elapsedDays + " DAY");
                    holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_due_today));
                } else {
                    if (task.getTaskLastDone() == null) holder.taskLastDone.setText("Never - DUE IN " + elapsedDays + " DAYS");
                    else holder.taskLastDone.setText(taskDate + " - DUE IN " + elapsedDays + " DAYS");
                    holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_due_today));
                }
            } else {
                if (task.getTaskLastDone() == null) holder.taskLastDone.setText("Never - Due in " + elapsedDays + " days");
            }
        } else if (task.getTaskType().equals("recurring") & task.getTaskFrequency().contains("miles") & task.getTaskLastDone() != null) {
            Task thisTask = task;
            String finalTaskDate = taskDate;

            Query query = userRef.child("records").orderByChild("odometer");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String mileageFrequency = thisTask.getTaskFrequency().split(" ")[0];
                    String lastRecordMileage = null;
                    String mostRecentMileage = null;
                    String dueMileage;
                    int diffMiles;
                    ArrayList<Record> records = new ArrayList<>();

                    for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        records.add(snapshot.getValue(Record.class));
                    }
                    Collections.reverse(records);

                    for (Record record:records) {
                        if (record.getVehicle().equals(thisTask.getTaskVehicle())) {
                            if (record.getDate().equals(thisTask.getTaskLastDone())) {
                                lastRecordMileage = record.getOdometer();
                            } else if (record.getTitle().equals(thisTask.getTaskName())) {
                                lastRecordMileage = record.getOdometer();
                            } else lastRecordMileage = record.getOdometer();
                            break;
                        }
                    }
                    for (Record record:records) {
                        if (record.getVehicle().equals(thisTask.getTaskVehicle())) {
                            mostRecentMileage = record.getOdometer();
                            break;
                        }
                    }

                    assert lastRecordMileage != null;
                    dueMileage = String.valueOf(Integer.parseInt(lastRecordMileage) + Integer.parseInt(mileageFrequency));
                    assert mostRecentMileage != null;
                    diffMiles = Integer.parseInt(dueMileage) - Integer.parseInt(mostRecentMileage);

                    if (diffMiles == 0) {
                        if (thisTask.getTaskLastDone() == null) holder.taskLastDone.setText("Never - DUE NOW");
                        else holder.taskLastDone.setText(finalTaskDate + " - DUE NOW");
                        holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_due_today));
                    } else if (diffMiles < 0) {
                        if (thisTask.getTaskLastDone() == null) holder.taskLastDone.setText("Never - OVERDUE BY " + Math.abs(diffMiles)  + " miles");
                        else holder.taskLastDone.setText(finalTaskDate + " - OVERDUE BY " + Math.abs(diffMiles)  + " miles");
                        holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_overdue));
                    } else if (diffMiles <= 1000) {
                        if (thisTask.getTaskLastDone() == null) holder.taskLastDone.setText("Never - DUE IN " + diffMiles + " MILES");
                        else holder.taskLastDone.setText(finalTaskDate + " - DUE IN " + diffMiles + " MILES");
                        holder.taskLastDone.setTextColor(context.getResources().getColor(R.color.task_due_today));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("Error", "loadPost:onCancelled", databaseError.toException());
                }

            });
        }
        setFadeAnimation(holder.itemView);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.task_view, parent, false);
        MaterialCardView taskView = view.findViewById(R.id.task_material_card);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!taskView.isChecked()) {
                    new MaterialAlertDialogBuilder(parent.getContext())
                            .setTitle("Complete Task?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    taskView.setChecked(true);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else {
                    Snackbar.make(context, parent.getRootView(), "Task already completed.", Snackbar.LENGTH_SHORT)
                            .setAnchorView(parent.getRootView().findViewById(R.id.bottom_nav_view))
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .show();
                }
                return true;
            }
        });
        return new TaskViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return taskArrayList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView taskView;
        TextView taskName, taskVehicle, taskLastDone, taskDueDate, taskFrequency, taskNotes;
        LinearLayout task_view_frequency_layout, task_view_due_date_layout, task_view_notes_layout;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskView = itemView.findViewById(R.id.task_material_card);
            taskName = itemView.findViewById(R.id.task_view_name);
            taskVehicle = itemView.findViewById(R.id.task_view_vehicle);
            taskLastDone = itemView.findViewById(R.id.task_view_last_done_date);
            taskDueDate = itemView.findViewById(R.id.task_view_due_date);
            taskFrequency = itemView.findViewById(R.id.task_view_frequency);
            taskNotes = itemView.findViewById(R.id.task_view_notes);
            task_view_frequency_layout = itemView.findViewById(R.id.task_view_frequency_layout);
            task_view_due_date_layout = itemView.findViewById(R.id.task_view_due_date_layout);
            task_view_notes_layout = itemView.findViewById(R.id.task_view_notes_layout);
        }
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference("users/" + mUser.getUid());
    }

    private void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(FADE_DURATION);
        view.startAnimation(anim);
    }
}
