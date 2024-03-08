package com.example.myapp.ui.checkup;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.R;
import com.example.myapp.TaskAdapter;
import com.example.myapp.data.Record;
import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.example.myapp.databinding.FragmentCheckupBinding;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
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
import java.util.Objects;
import java.util.TimeZone;

public class CheckupFragment extends Fragment {

    private boolean shouldRefreshOnResume = false;
    private CheckupViewModel mViewModel;
    private SharedPreferences sharedPref;
    private FragmentCheckupBinding binding;
    private View root;
    private final Vehicle vehicle = new Vehicle();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private final ArrayList<String> vehicleOptions = new ArrayList<>();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private int vehicleCount;
    private RecyclerView taskRecyclerView;
    private TaskAdapter taskAdapter;
    private final ArrayList<Task> taskArrayList = new ArrayList<>();
    private Task task = new Task();
    private String taskDateString, taskFilter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        CheckupViewModel checkupViewModel =
                new ViewModelProvider(this).get(CheckupViewModel.class);
        sharedPref = requireActivity().getSharedPreferences("SAVED_PREFERENCES", 0);
        taskFilter = sharedPref.getString("task_filter", "All");
        binding = FragmentCheckupBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        taskRecyclerView = root.findViewById(R.id.tasks_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        taskRecyclerView.setLayoutManager(layoutManager);
        taskRecyclerView.setItemAnimator(new DefaultItemAnimator());
        taskAdapter = new TaskAdapter(taskArrayList, getContext(), getView(), vehicleArrayList);
        taskRecyclerView.setAdapter(taskAdapter);
        taskRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

        ItemTouchHelper.Callback callback = new ItemTouchHelper.Callback() {

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(0, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                viewHolder.itemView.setBackgroundColor(0);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // Get RecyclerView item from the ViewHolder
                View itemView = viewHolder.itemView;
                Bitmap icon;

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    Paint p = new Paint();
                    if (dX > 0) {
                        /* Set your color for positive displacement */
                        p.setARGB(255, 255, 255, 0);

                        // Draw Rect with varying right side, equal to displacement dX
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                                (float) itemView.getBottom(), p);

                        // Set the image icon for Right swipe
                        icon = BitmapFactory.decodeResource(
                                requireContext().getResources(), R.drawable.ic_edit_96);
                        c.drawBitmap(icon,
                                (float) itemView.getLeft() + convertDpToPx(20),
                                (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight())/2,
                                p);
                    } else {
                        /* Set your color for negative displacement */
                        p.setARGB(255, 255, 0, 0);

                        // Draw Rect with varying left side, equal to the item's right side plus negative displacement dX
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom(), p);

                        //Set the image icon for Left swipe
                        icon = BitmapFactory.decodeResource(
                                requireContext().getResources(), R.drawable.ic_delete_96);
                        c.drawBitmap(icon,
                                (float) itemView.getRight() - convertDpToPx(20) - icon.getWidth(),
                                (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight())/2,
                                p);
                    }
                    // Fade out the view as it is swiped out of the parent's bounds
                    final float alpha = 1.0f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            private int convertDpToPx(int dp){
                return Math.round(dp * (getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int taskPosition = viewHolder.getAdapterPosition();
                task = taskArrayList.get(taskPosition);
                if (direction == 16){
                    //Swipe Left - Delete Task
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Task")
                            .setMessage("Are you sure you want to delete this task?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteTask(task, taskPosition);
                                    dialog.dismiss();
                                    Snackbar.make(requireActivity().findViewById(R.id.bottom_nav_view), "Task Deleted", Snackbar.LENGTH_LONG)
                                            .setAction("Undo", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    undoTask(task, taskPosition);
                                                }
                                            })
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    taskAdapter.notifyItemChanged(taskPosition);
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_round_warning_24)
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    taskAdapter.notifyItemChanged(taskPosition);
                                }
                            })
                            .show();
                } else if (direction == 32){
                    //Swipe Right - Edit Task
                    try {
                        editTask(task, taskPosition);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(taskRecyclerView);

        initFirebase();
        initVars();

        return root;
    }

    private void initVars() {

    }

    private void editTask(Task task, int taskPosition) throws ParseException {
        Task newTask = new Task();
        final int[] vehicleSelection = new int[1];
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View editSingleTaskView = getLayoutInflater().inflate(R.layout.edit_single_checkup, null);
        @SuppressLint("InflateParams") final View editRecurringTaskView = getLayoutInflater().inflate(R.layout.edit_recurring_checkup, null);

        TextInputLayout rcTaskNameLayout, rcVehicleLayout, rcMileageInputLayout, rcTimeInputLayout, rcTimeFrequencyLayout, rcNotesLayout, rcDoneBeforeLayout;
        EditText rcTaskName = null, rcVehicle, rcMileage = null, rcTime = null, rcTimeFrequency, rcNotes = null, rcDoneBeforeDate = null;
        RadioButton rcMileageRB = null, rcTimeRB = null;
        MaterialCheckBox rcDoneBeforeBox = null;
        LinearLayout rcMileageLL, rcTimeLL, rcDoneBeforeLL;
        AutoCompleteTextView rcVehiclePicker = null, rcFrequencyPicker = null;

        TextInputLayout scTaskNameLayout, scVehicleLayout, scDateLayout, scNotesLayout;
        EditText scTaskName = null, scVehicle, scDate = null, scNotes = null;
        AutoCompleteTextView scVehiclePicker = null;

        if (task.getTaskType().equals("single")) {
            scTaskNameLayout = editSingleTaskView.findViewById(R.id.sc_edit_task_name);
            scVehicleLayout = editSingleTaskView.findViewById(R.id.sc_edit_vehicle_picker);
            scDateLayout = editSingleTaskView.findViewById(R.id.sc_edit_date);
            scNotesLayout = editSingleTaskView.findViewById(R.id.sc_edit_notes);

            scTaskName = scTaskNameLayout.getEditText();
            scVehicle = scVehicleLayout.getEditText();
            scDate = scDateLayout.getEditText();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            taskDateString = task.getTaskDueDate();
            scDate.setText(SimpleDateFormat.getDateInstance().format(Objects.requireNonNull(format.parse(task.getTaskDueDate()))));
            scNotes = scNotesLayout.getEditText();
            scTaskName.setText(task.getTaskName());
            String vehicleTitle = null;
            for (Vehicle vehicle:vehicleArrayList) {
                if (String.valueOf(vehicle.getVehicleId()).equals(task.getTaskVehicle())) {
                    vehicleTitle = vehicle.vehicleTitle();
                    vehicleSelection[0] = vehicleArrayList.indexOf(vehicle);
                }
            }
            scVehicle.setText(vehicleTitle);
            scNotes.setText(task.getTaskNotes());

            int darkMode = sharedPref.getInt("dark_mode", 0);
            ArrayList<String> vehicleOptions = new ArrayList<>();
            for (Vehicle vehicle: vehicleArrayList) {
                vehicleOptions.add(vehicle.vehicleTitle());
            }
            if (darkMode == 0) {
                ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_light, vehicleOptions);
                stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                scVehiclePicker =
                        editSingleTaskView.findViewById(R.id.sc_edit_vehicle_options);
                scVehiclePicker.setAdapter(stringArrayAdapter);
            } else if (darkMode == 1){
                ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_dark, vehicleOptions);
                stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                scVehiclePicker =
                        editSingleTaskView.findViewById(R.id.sc_edit_vehicle_options);
                scVehiclePicker.setAdapter(stringArrayAdapter);
            }
            assert scVehiclePicker != null;
            scVehiclePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    vehicleSelection[0] = i;
                }
            });

            EditText finalScDate2 = scDate;
            scDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    final Calendar myCalendar = Calendar.getInstance();

                    DatePickerDialog.OnDateSetListener datePicker = (dateView, year, monthOfYear, dayOfMonth) -> {
                        myCalendar.set(Calendar.YEAR, year);
                        myCalendar.set(Calendar.MONTH, monthOfYear);
                        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        taskDateString = sdf.format(myCalendar.getTime());
                        finalScDate2.setText(SimpleDateFormat.getDateInstance().format(myCalendar.getTime()));
                    };

                    if (!finalScDate2.getText().toString().isEmpty()) {
                        Date date;
                        try {
                            date = SimpleDateFormat.getDateInstance().parse(finalScDate2.getText().toString());
                            assert date != null;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        myCalendar.setTime(date);
                        new DatePickerDialog(getContext(), datePicker, myCalendar
                                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    } else {
                        new DatePickerDialog(getContext(), datePicker, myCalendar
                                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    }

                     */

                    Date displayDate = null;
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    try {
                        displayDate = format.parse(taskDateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    assert displayDate != null;
                    MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Due Date")
                            .setSelection(displayDate.getTime())
                            .build();
                    materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                        @Override
                        public void onPositiveButtonClick(Long selection) {
                            TimeZone timeZoneUTC = TimeZone.getDefault();
                            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
                            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date date = new Date(selection + offsetFromUTC);
                            taskDateString = simpleFormat.format(date);
                            finalScDate2.setText(SimpleDateFormat.getDateInstance().format(date));
                        }
                    });
                    materialDatePicker.show(getChildFragmentManager(), "date");
                }
            });

            dialogBuilder.setView(editSingleTaskView);
        } else if (task.getTaskType().equals("recurring")) {
            rcMileageLL = editRecurringTaskView.findViewById(R.id.rc_edit_mileage_layout);
            rcTimeLL = editRecurringTaskView.findViewById(R.id.rc_edit_time_layout);
            rcDoneBeforeLL = editRecurringTaskView.findViewById(R.id.rc_edit_done_before_layout);
            rcTaskNameLayout = editRecurringTaskView.findViewById(R.id.rc_edit_task_name);
            rcVehicleLayout = editRecurringTaskView.findViewById(R.id.rc_edit_vehicle_picker);
            rcMileageInputLayout = editRecurringTaskView.findViewById(R.id.rc_edit_mileage_input);
            rcTimeInputLayout = editRecurringTaskView.findViewById(R.id.rc_edit_time_input);
            rcTimeFrequencyLayout = editRecurringTaskView.findViewById(R.id.rc_edit_time_frequency_picker);
            rcNotesLayout = editRecurringTaskView.findViewById(R.id.rc_edit_notes);
            rcDoneBeforeLayout = editRecurringTaskView.findViewById(R.id.rc_edit_done_before_date);
            rcTaskName = rcTaskNameLayout.getEditText();
            rcVehicle = rcVehicleLayout.getEditText();
            rcMileage = rcMileageInputLayout.getEditText();
            rcTime = rcTimeInputLayout.getEditText();
            rcTimeFrequency = rcTimeFrequencyLayout.getEditText();
            rcNotes = rcNotesLayout.getEditText();
            rcDoneBeforeDate = rcDoneBeforeLayout.getEditText();
            rcMileageRB = editRecurringTaskView.findViewById(R.id.rc_edit_mileage_rb);
            rcTimeRB = editRecurringTaskView.findViewById(R.id.rc_edit_time_rb);
            rcDoneBeforeBox = editRecurringTaskView.findViewById(R.id.rc_edit_done_before_box);
            taskDateString = task.getTaskDueDate();
            rcTaskName.setText(task.getTaskName());
            String vehicleTitle = null;
            for (Vehicle vehicle:vehicleArrayList) {
                if (String.valueOf(vehicle.getVehicleId()).equals(task.getTaskVehicle())) {
                    vehicleTitle = vehicle.vehicleTitle();
                    vehicleSelection[0] = vehicleArrayList.indexOf(vehicle);
                }
            }
            rcVehicle.setText(vehicleTitle);
            rcNotes.setText(task.getTaskNotes());
            if (task.getTaskLastDone() != null) {
                taskDateString = task.getTaskLastDone();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                rcDoneBeforeDate.setText(SimpleDateFormat.getDateInstance().format(Objects.requireNonNull(format.parse(task.getTaskLastDone()))));
                rcDoneBeforeBox.setChecked(true);
                rcDoneBeforeLL.setVisibility(View.VISIBLE);
            }

            if (task.getTaskFrequency().contains("miles")) {
                rcMileageRB.setChecked(true);
                rcMileageLL.setVisibility(View.VISIBLE);
                rcTimeRB.setChecked(false);
                rcTimeLL.setVisibility(View.GONE);

                rcMileage.setText(task.getTaskFrequency().split(" ")[0]);
            } else {
                rcTimeRB.setChecked(true);
                rcTimeLL.setVisibility(View.VISIBLE);
                rcMileageRB.setChecked(false);
                rcMileageLL.setVisibility(View.GONE);

                rcTime.setText(task.getTaskFrequency().split(" ")[0]);
                rcTimeFrequency.setText(task.getTaskFrequency().split(" ")[1]);
            }

            int darkMode = sharedPref.getInt("dark_mode", 0);
            ArrayList<String> vehicleOptions = new ArrayList<>();
            for (Vehicle vehicle: vehicleArrayList) {
                vehicleOptions.add(vehicle.vehicleTitle());
            }
            if (darkMode == 0) {
                ArrayAdapter<String> vehicleStringAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_light, vehicleOptions);
                vehicleStringAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                rcVehiclePicker =
                        editRecurringTaskView.findViewById(R.id.rc_edit_vehicle_options);
                rcVehiclePicker.setAdapter(vehicleStringAdapter);

                ArrayAdapter<String> frequencyStringAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.time_frequencies));
                frequencyStringAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                rcFrequencyPicker =
                        editRecurringTaskView.findViewById(R.id.rc_edit_time_frequency_options);
                rcFrequencyPicker.setAdapter(frequencyStringAdapter);
            } else if (darkMode == 1){
                ArrayAdapter<String> vehicleStringAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_dark, vehicleOptions);
                vehicleStringAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                rcVehiclePicker =
                        editRecurringTaskView.findViewById(R.id.rc_edit_vehicle_options);
                rcVehiclePicker.setAdapter(vehicleStringAdapter);

                ArrayAdapter<String> frequencyStringAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.time_frequencies));
                frequencyStringAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                rcFrequencyPicker =
                        editRecurringTaskView.findViewById(R.id.rc_edit_time_frequency_options);
                rcFrequencyPicker.setAdapter(frequencyStringAdapter);
            }
            assert rcVehiclePicker != null;
            rcVehiclePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    vehicleSelection[0] = i;
                }
            });

            RadioButton finalRcMileageRB = rcMileageRB;
            RadioButton finalRcTimeRB = rcTimeRB;
            rcMileageRB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finalRcMileageRB.setChecked(true);
                    rcMileageLL.setVisibility(View.VISIBLE);
                    finalRcTimeRB.setChecked(false);
                    rcTimeLL.setVisibility(View.GONE);
                }
            });
            RadioButton finalRcMileageRB1 = rcMileageRB;
            RadioButton finalRcTimeRB1 = rcTimeRB;
            rcTimeRB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finalRcTimeRB1.setChecked(true);
                    rcTimeLL.setVisibility(View.VISIBLE);
                    finalRcMileageRB1.setChecked(false);
                    rcMileageLL.setVisibility(View.GONE);
                }
            });
            MaterialCheckBox finalRcDoneBeforeBox1 = rcDoneBeforeBox;
            rcDoneBeforeBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (finalRcDoneBeforeBox1.isChecked()) {
                        rcDoneBeforeLL.setVisibility(View.VISIBLE);
                        taskDateString = task.getTaskLastDone();
                    }
                    else {
                        rcDoneBeforeLL.setVisibility(View.GONE);
                        taskDateString = null;
                    }
                }
            });
            EditText finalRcDoneBeforeDate = rcDoneBeforeDate;
            assert rcDoneBeforeDate != null;
            rcDoneBeforeDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    final Calendar myCalendar = Calendar.getInstance();

                    DatePickerDialog.OnDateSetListener datePicker = (dateView, year, monthOfYear, dayOfMonth) -> {
                        myCalendar.set(Calendar.YEAR, year);
                        myCalendar.set(Calendar.MONTH, monthOfYear);
                        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        taskDateString = sdf.format(myCalendar.getTime());
                        finalRcDoneBeforeDate.setText(SimpleDateFormat.getDateInstance().format(myCalendar.getTime()));
                    };

                    if (!finalRcDoneBeforeDate.getText().toString().isEmpty()) {
                        Date date;
                        try {
                            date = SimpleDateFormat.getDateInstance().parse(finalRcDoneBeforeDate.getText().toString());
                            assert date != null;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        myCalendar.setTime(date);
                        new DatePickerDialog(requireContext(), datePicker, myCalendar
                                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    } else {
                        new DatePickerDialog(requireContext(), datePicker, myCalendar
                                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    }

                     */

                    Date displayDate = null;
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    try {
                        displayDate = format.parse(taskDateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    assert displayDate != null;
                    MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Due Date")
                            .setSelection(displayDate.getTime())
                            .build();
                    materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                        @Override
                        public void onPositiveButtonClick(Long selection) {
                            TimeZone timeZoneUTC = TimeZone.getDefault();
                            int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
                            SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Date date = new Date(selection + offsetFromUTC);
                            taskDateString = simpleFormat.format(date);
                            finalRcDoneBeforeDate.setText(SimpleDateFormat.getDateInstance().format(date));
                        }
                    });
                    materialDatePicker.show(getChildFragmentManager(), "date");
                }
            });

            dialogBuilder.setView(editRecurringTaskView);
        }
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(true);
        dialog.setOnCancelListener(dialogInterface -> {
            taskAdapter.notifyItemChanged(taskPosition);
            taskRecyclerView.setAdapter(taskAdapter);
            dialogInterface.cancel();
            dialog.dismiss();
        });

        Button rcFinishBtn = editRecurringTaskView.findViewById(R.id.rc_edit_finish_btn);
        Button scFinishBtn = editSingleTaskView.findViewById(R.id.sc_edit_finish_btn);
        Button rcCancelBtn = editRecurringTaskView.findViewById(R.id.rc_edit_cancel_btn);
        Button scCancelBtn = editSingleTaskView.findViewById(R.id.sc_edit_cancel_btn);
        AutoCompleteTextView finalRcVehiclePicker = rcVehiclePicker, finalRcFrequencyPicker = rcFrequencyPicker,
                finalScVehiclePicker = scVehiclePicker;
        RadioButton finalRcMileageRB2 = rcMileageRB, finalRcTimeRB2 = rcTimeRB;
        EditText finalRcMileage = rcMileage, finalRcTime = rcTime, finalRcTaskName = rcTaskName,
                finalRcNotes = rcNotes, finalScTaskName = scTaskName, finalScDate1 = scDate, finalScNotes = scNotes;
        EditText finalRcDoneBeforeDate1 = rcDoneBeforeDate;
        MaterialCheckBox finalRcDoneBeforeBox = rcDoneBeforeBox;
        rcFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String frequency = null;
                int errors = 0;

                if (finalRcMileageRB2.isChecked()) {
                    frequency = finalRcMileage.getText().toString().trim() + " miles";
                    if (finalRcMileage.getText().toString().trim().equals("")) {
                        finalRcMileage.setError("Cannot be blank");
                        errors++;
                    }
                } else if (finalRcTimeRB2.isChecked()) {
                    frequency = finalRcTime.getText().toString().trim() + " " + finalRcFrequencyPicker.getText().toString().trim();
                    if (finalRcTime.getText().toString().trim().equals("")) {
                        finalRcTime.setError("Cannot be blank");
                        errors++;
                    }
                    if (finalRcFrequencyPicker.getText().toString().trim().equals("")) {
                        finalRcFrequencyPicker.setError("Cannot be blank");
                        errors++;
                    }
                }

                if (finalRcTaskName.getText().toString().trim().equals("")) {
                    finalRcTaskName.setError("Cannot be blank");
                    errors++;
                }
                if (finalRcVehiclePicker.getText().toString().trim().equals("")) {
                    finalRcVehiclePicker.setError("Cannot be blank");
                }
                if (errors == 0) {
                    newTask.setTaskName(finalRcTaskName.getText().toString().trim());
                    newTask.setTaskVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection[0]).getVehicleId()));
                    newTask.setTaskLastDone(task.getTaskLastDone());
                    newTask.setTaskFrequency(frequency);
                    newTask.setTaskNotes(finalRcNotes.getText().toString().trim());
                    assert finalRcDoneBeforeBox != null;
                    if (finalRcDoneBeforeBox.isChecked()) newTask.setTaskLastDone(taskDateString);
                    else newTask.setTaskLastDone(null);
                    newTask.setTaskType("recurring");
                    newTask.setEntryTime(Calendar.getInstance().getTimeInMillis());

                    taskArrayList.remove(taskPosition);
                    taskAdapter.notifyItemRemoved(taskPosition);
                    taskArrayList.add(taskPosition, newTask);
                    taskAdapter.notifyItemInserted(taskPosition);
                    userRef.child("tasks").setValue(taskArrayList);
                    dialog.dismiss();
                }
            }
        });
        scFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int errors = 0;
                if (finalScTaskName.getText().toString().trim().equals("")) {
                    finalScTaskName.setError("Cannot be blank");
                    errors++;
                }
                if (finalScVehiclePicker.getText().toString().trim().equals("")) {
                    finalScVehiclePicker.setError("Cannot be blank");
                }
                if (taskDateString.equals("")) {
                    finalScDate1.setError("Cannot be blank");
                    errors++;
                }
                if (errors == 0) {
                    newTask.setTaskName(finalScTaskName.getText().toString().trim());
                    newTask.setTaskVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection[0]).getVehicleId()));
                    newTask.setTaskDueDate(taskDateString);
                    newTask.setTaskNotes(finalScNotes.getText().toString().trim());
                    newTask.setTaskType("single");
                    newTask.setEntryTime(Calendar.getInstance().getTimeInMillis());

                    taskArrayList.remove(taskPosition);
                    taskAdapter.notifyItemRemoved(taskPosition);
                    taskArrayList.add(taskPosition, newTask);
                    taskAdapter.notifyItemInserted(taskPosition);
                    userRef.child("tasks").setValue(taskArrayList);
                    dialog.dismiss();
                }
            }
        });
        rcCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskAdapter.notifyItemChanged(taskPosition);
                dialog.dismiss();
            }
        });
        scCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskAdapter.notifyItemChanged(taskPosition);
                dialog.dismiss();
            }
        });
    }

    private void undoTask(Task task, int taskPosition) {
        taskArrayList.add(taskPosition, task);
        userRef.child("tasks").setValue(taskArrayList);
        taskAdapter.notifyItemInserted(taskPosition);
    }

    private void deleteTask(Task task, int taskPosition) {
        taskArrayList.remove(taskPosition);
        userRef.child("tasks").setValue(taskArrayList);
        taskAdapter.notifyItemRemoved(taskPosition);
    }

    private void addEventListener(DatabaseReference userRef) {
        ValueEventListener eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                vehicleArrayList.clear();
                recordArrayList.clear();

                vehicleCount = Integer.parseInt(String.valueOf(snapshot.child("vehicles").getChildrenCount()));
                for (DataSnapshot dataSnapshot : snapshot.child("vehicles").getChildren()) {
                    vehicleArrayList.add(dataSnapshot.getValue(Vehicle.class));
                }
                for (DataSnapshot dataSnapshot : snapshot.child("records").getChildren()) {
                    recordArrayList.add(dataSnapshot.getValue(Record.class));
                }
                getTasks();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ERROR", "loadEvent:onCancelled", error.toException());
            }
        };
        userRef.addValueEventListener(eventListener);
    }

    private void getTasks() {
        boolean desc = false;
        String sortType = null, sortTasks = sharedPref.getString("sort_tasks", "date_desc");
        switch (sortTasks) {
            case "date_desc":
                sortType = "taskDueDate";
                desc = true;
                break;
            case "date_asc":
                sortType = "taskDueDate";
                break;
            case "title_desc":
                sortType = "taskName";
                desc = true;
                break;
            case "title_asc":
                sortType = "taskName";
                break;
        }

        assert sortType != null;
        Query query = userRef.child("tasks").orderByChild(sortType);
        boolean finalDesc = desc;

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                taskArrayList.clear();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    taskArrayList.add(snapshot.getValue(Task.class));
                }
                if (finalDesc) {
                    Collections.reverse(taskArrayList);
                }
                if (!taskFilter.equals("All")) {
                    ArrayList<Task> dummyTasks = new ArrayList<>(taskArrayList);
                    for (Task task1:dummyTasks) {
                        if (!task1.getTaskVehicle().equals(taskFilter)) taskArrayList.remove(task1);
                    }
                }
                taskAdapter.notifyItemRangeChanged(0, taskArrayList.size());

                initVars();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("Error", "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(CheckupViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(shouldRefreshOnResume){
            requireActivity().recreate();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        shouldRefreshOnResume = true;
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        assert mUser != null;
        userRef = database.getReference("users/" + mUser.getUid());
        if (taskArrayList.size() == 0) addEventListener(userRef);
    }

}