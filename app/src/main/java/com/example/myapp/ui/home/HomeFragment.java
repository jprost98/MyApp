package com.example.myapp.ui.home;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.myapp.R;
import com.example.myapp.RecordAdapter;
import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.databinding.FragmentHomeBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private View root;
    private Record record = new Record();
    private ArrayList<Record> recordArrayList = new ArrayList<>();
    private ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private ArrayList<String> spinnerOptions = new ArrayList<>();
    private RecyclerView recordsRecyclerView;
    private RecordAdapter recordAdapter;
    private RecordDatabase recordDatabase;
    private VehicleDatabase vehicleDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef = database.getReference("users");
    private final Calendar myCalendar = Calendar.getInstance();
    private String filterBy;
    private SharedPreferences.Editor editor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        SharedPreferences sharedPref = getContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        filterBy = sharedPref.getString("filter_by_value", "All");
        Log.d("Filter value", filterBy);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        initFirebase();
        getRecords();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recordsRecyclerView = root.findViewById(R.id.records_recyclerview);
        recordAdapter = new RecordAdapter(recordArrayList);
        recordsRecyclerView.setLayoutManager(layoutManager);
        recordsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        recordsRecyclerView.addItemDecoration(new DividerItemDecoration(root.getContext(), DividerItemDecoration.VERTICAL));
        recordsRecyclerView.setAdapter(recordAdapter);

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
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    assert viewHolder != null;
                    viewHolder.itemView.setBackgroundColor(Color.GRAY);
                }
                super.onSelectedChanged(viewHolder, actionState);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                viewHolder.itemView.setBackgroundColor(0);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == 16){
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("Delete record")
                            .setMessage("Are you sure you want to delete this record?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    record = recordArrayList.get(viewHolder.getAdapterPosition());
                                    deleteRecord(record);
                                    Snackbar.make(getActivity().findViewById(R.id.content_constraint), "Record Deleted", Snackbar.LENGTH_LONG)
                                            .setAction("Undo", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    undoRecord(record, viewHolder.getAdapterPosition());
                                                }
                                            })
                                            .show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    recordAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                                }
                            })
                            .setIcon(R.drawable.ic_round_warning_24)
                            .show();
                } else if (direction == 32){
                    record = recordArrayList.get(viewHolder.getAdapterPosition());
                    recordAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    editRecord(record);
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recordsRecyclerView);

        return root;
    }

    private void editRecord(Record editRecord) {
        Record newRecord = new Record();
        Log.d("Edit Record", editRecord.toString());
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View editRecordPopup = getLayoutInflater().inflate(R.layout.popup_edit_record, null);

        EditText editTitle, editDate, editOdometer, editDescription;
        Spinner editRecordVehicle;
        editTitle = editRecordPopup.findViewById(R.id.edit_record_title_input);
        editTitle.setText(editRecord.getTitle());
        editDate = editRecordPopup.findViewById(R.id.edit_record_date_input);
        editDate.setText(editRecord.getDate());
        createCalender(editDate);
        editOdometer = editRecordPopup.findViewById(R.id.edit_record_odometer_input);
        editOdometer.setText(editRecord.getOdometer());
        editDescription = editRecordPopup.findViewById(R.id.edit_record_description_input);
        editDescription.setText(editRecord.getDescription());
        vehicleArrayList.clear();
        vehicleDatabase = Room.databaseBuilder(requireContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        editRecordVehicle = editRecordPopup.findViewById(R.id.edit_record_vehicle_input);
        for (Vehicle vehicle: vehicleArrayList) {
            spinnerOptions.add(vehicle.vehicleTitle());
        }
        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_dropdown_item, spinnerOptions);
        stringArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editRecordVehicle.setAdapter(stringArrayAdapter);
        for (Vehicle vehicle:vehicleArrayList) {
            if (vehicle.vehicleTitle().equals(editRecord.getVehicle())) editRecordVehicle.setSelection(vehicleArrayList.indexOf(vehicle));
        }

        Button editRecordCancelBtn = editRecordPopup.findViewById(R.id.edit_record_cancel_btn);
        Button editRecordFinishBtn = editRecordPopup.findViewById(R.id.edit_record_finish_btn);

        dialogBuilder.setView(editRecordPopup);
        dialog = dialogBuilder.create();
        dialog.show();

        editRecordCancelBtn.setOnClickListener(view -> {
            dialog.dismiss();
        });
        editRecordFinishBtn.setOnClickListener(view -> {
            int errors = 0;
            if (editTitle.getText().toString().trim().isEmpty()) {
                editTitle.setError("Enter a title for the record");
                errors++;
            }
            if (editDate.getText().toString().trim().isEmpty()) {
                editDate.setError("Enter the date of the maintenance");
                errors++;
            }
            if (editOdometer.getText().toString().trim().isEmpty()) {
                editOdometer.setError("Enter the odometer reading for the record");
                errors++;
            }
            if (errors == 0) {
                newRecord.setRecordId(editRecord.getRecordId());
                newRecord.setTitle(editTitle.getText().toString().trim());
                newRecord.setDate(editDate.getText().toString().trim());
                newRecord.setVehicle(editRecordVehicle.getSelectedItem().toString());
                newRecord.setOdometer(editOdometer.getText().toString().trim());
                newRecord.setDescription(editDescription.getText().toString().trim());
                newRecord.setEntryTime(Calendar.getInstance().getTimeInMillis());

                Log.d("New Record", newRecord.toString());

                recordArrayList.set(recordArrayList.indexOf(editRecord), newRecord);
                recordDatabase.recordDao().updateRecord(newRecord);
                Log.d("Local Records", recordDatabase.recordDao().getAllRecords().toString());
                userRef.child(mUser.getDisplayName()).child("Records").setValue(recordArrayList);
                stringArrayAdapter.notifyDataSetChanged();
                recordsRecyclerView.setAdapter(recordAdapter);
                dialog.dismiss();
            }
        });
    }

    private void undoRecord(Record record, int adapterPosition) {
        recordDatabase.recordDao().addRecord(record);
        recordArrayList.add(record);
        userRef.child(mUser.getDisplayName()).child("Records").setValue(recordArrayList);
        recordAdapter.notifyDataSetChanged();
        recordsRecyclerView.setAdapter(recordAdapter);
    }

    private void deleteRecord(Record record) {
        recordDatabase.recordDao().deleteRecord(record);
        recordArrayList.remove(record);
        userRef.child(mUser.getDisplayName()).child("Records").setValue(recordArrayList);
        recordAdapter.notifyDataSetChanged();
        recordsRecyclerView.setAdapter(recordAdapter);
    }

    private void getRecords() {
        recordArrayList.clear();
        recordDatabase = Room.databaseBuilder(requireContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        if (filterBy.equals("All")) {
            recordArrayList.clear();
            recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
            userRef.child(mUser.getDisplayName()).child("Records").setValue(recordArrayList);
        } else {
            Vehicle filteredVehicle = new Vehicle();
            vehicleDatabase = Room.databaseBuilder(requireContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
            vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
            for (Vehicle vehicle:vehicleArrayList) {
                if (filterBy.equals(vehicle.vehicleTitle())) {
                    filteredVehicle = vehicle;
                }
            }
            recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicle(filteredVehicle.vehicleTitle()));
        }
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

    private void createCalender(EditText editDate) {
        DatePickerDialog.OnDateSetListener datePicker = (view, year, monthOfYear, dayOfMonth) -> {
            // TODO Auto-generated method stub
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, monthOfYear);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String myFormat = "MM/dd/yy"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);

            editDate.setText(sdf.format(myCalendar.getTime()));
        };

        editDate.setOnClickListener(v -> {
            // TODO Auto-generated method stub
            new DatePickerDialog(getContext(), datePicker, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }
}