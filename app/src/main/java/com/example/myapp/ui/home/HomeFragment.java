package com.example.myapp.ui.home;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private View root;
    private Record record = new Record();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private final ArrayList<String> vehicleOptions = new ArrayList<>();
    private RecyclerView recordsRecyclerView;
    private RecordAdapter recordAdapter;
    private RecordDatabase recordDatabase;
    private VehicleDatabase vehicleDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("users");
    private final Calendar myCalendar = Calendar.getInstance();
    private String filterBy, sortRecords;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;
    private AutoCompleteTextView recordVehiclePicker;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        sharedPref = getContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        filterBy = sharedPref.getString("filter_by_value", "All");
        sortRecords = sharedPref.getString("sort_records", "Date_Desc");
        Log.d("Filter value", filterBy);
        Log.d("Sort records", sortRecords);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        root = binding.getRoot();

        recordDatabase = Room.databaseBuilder(requireContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        vehicleDatabase = Room.databaseBuilder(requireContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();

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
                int recordPosition = viewHolder.getAdapterPosition();
                if (direction == 16){
                    //Swipe Left - Delete Record
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete record")
                            .setMessage("Are you sure you want to delete this record?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    record = recordArrayList.get(recordPosition);
                                    Log.d("Record Position", String.valueOf(viewHolder.getAdapterPosition()));
                                    deleteRecord(record, recordPosition);
                                    dialog.dismiss();
                                    Snackbar.make(requireActivity().findViewById(R.id.bottom_nav_view), "Record Deleted", Snackbar.LENGTH_LONG)
                                            .setAction("Undo", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    undoRecord(record, recordPosition);
                                                }
                                            })
                                            .show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    recordAdapter.notifyItemChanged(recordPosition);
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_round_warning_24)
                            .show();
                } else if (direction == 32){
                    //Swipe Right - Edit Record
                    record = recordArrayList.get(recordPosition);
                    recordAdapter.notifyItemChanged(recordPosition);
                    editRecord(record, recordPosition);
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem searchItem  = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filter(s);
                return false;
            }
        });
    }

    private void filter(String text) {
        ArrayList<Record> filteredList = new ArrayList<>();
        for (Record item : recordArrayList) {
            if (item.getDate().toLowerCase().contains(text.toLowerCase())
                    || item.getTitle().toLowerCase().contains(text.toLowerCase())
                    || item.getVehicle().toLowerCase().contains(text.toLowerCase())
                    || item.getDescription().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        recordAdapter.filterList(filteredList);
    }

    private void editRecord(Record editRecord, int recordPosition) {
        Record newRecord = new Record();
        Log.d("Edit Record", editRecord.toString());
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        AlertDialog dialog;
        @SuppressLint("InflateParams") final View editRecordPopup = getLayoutInflater().inflate(R.layout.popup_edit_record, null);

        TextInputLayout recordTitleLayout, recordDateLayout, recordVehicleLayout, recordOdometerLayout, recordNotesLayout;

        recordTitleLayout = editRecordPopup.findViewById(R.id.edit_record_title_input);
        recordDateLayout = editRecordPopup.findViewById(R.id.edit_record_date_input);
        recordVehicleLayout = editRecordPopup.findViewById(R.id.edit_record_vehicle_picker);
        recordOdometerLayout = editRecordPopup.findViewById(R.id.edit_record_odometer_input);
        recordNotesLayout = editRecordPopup.findViewById(R.id.edit_record_description_input);

        EditText editTitle, editDate, editOdometer, editDescription, editRecordVehicle;
        editTitle = recordTitleLayout.getEditText();
        editTitle.setText(editRecord.getTitle());
        editDate = recordDateLayout.getEditText();
        editDate.setText(editRecord.getDate());
        editRecordVehicle = recordVehicleLayout.getEditText();
        editRecordVehicle.setText(editRecord.getVehicle());
        editOdometer = recordOdometerLayout.getEditText();
        editOdometer.setText(editRecord.getOdometer());
        editDescription = recordNotesLayout.getEditText();
        editDescription.setText(editRecord.getDescription());

        Button dateButton = editRecordPopup.findViewById(R.id.edit_date_button);
        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Date of Work")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                    @Override
                    public void onPositiveButtonClick(Long selection) {
                        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selection));
                        editDate.setText(date);
                    }
                });
                materialDatePicker.show(getChildFragmentManager(), "tag");
            }
        });

        vehicleArrayList.clear();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        vehicleOptions.clear();
        int darkMode = sharedPref.getInt("dark_mode", 0);
        for (Vehicle vehicle: vehicleArrayList) {
            vehicleOptions.add(vehicle.vehicleTitle());
        }
        Log.d("Dark Mode", String.valueOf(darkMode));
        if (darkMode == 0) {
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_light, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            recordVehiclePicker =
                    editRecordPopup.findViewById(R.id.edit_outlined_exposed_dropdown_editable);
            recordVehiclePicker.setAdapter(stringArrayAdapter);
        } else if (darkMode == 1){
            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_dark, vehicleOptions);
            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
            recordVehiclePicker =
                    editRecordPopup.findViewById(R.id.edit_outlined_exposed_dropdown_editable);
            recordVehiclePicker.setAdapter(stringArrayAdapter);
        }

        Button editRecordCancelBtn = editRecordPopup.findViewById(R.id.edit_record_cancel_btn);
        Button editRecordFinishBtn = editRecordPopup.findViewById(R.id.edit_record_finish_btn);

        dialogBuilder.setView(editRecordPopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(false);

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
                newRecord.setVehicle(editRecordVehicle.getText().toString());
                newRecord.setOdometer(editOdometer.getText().toString().trim());
                newRecord.setDescription(editDescription.getText().toString().trim());
                newRecord.setEntryTime(Calendar.getInstance().getTimeInMillis());

                Log.d("New Record", newRecord.toString());

                recordDatabase.recordDao().updateRecord(newRecord);
                recordArrayList.clear();
                recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
                userRef.child(mUser.getUid()).child("records").setValue(recordArrayList);
                recordAdapter.notifyItemChanged(recordPosition);
                recordsRecyclerView.setAdapter(recordAdapter);
                dialog.dismiss();
                getActivity().recreate();
            }
        });
    }

    private void undoRecord(Record record, int recordPosition) {
        recordDatabase.recordDao().addRecord(record);
        recordArrayList.clear();
        recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
        userRef.child(mUser.getUid()).child("records").setValue(recordArrayList);
        recordAdapter.notifyItemInserted(recordPosition);
        recordsRecyclerView.setAdapter(recordAdapter);
    }

    private void deleteRecord(Record record, int recordPosition) {
        recordDatabase.recordDao().deleteRecord(record);
        recordArrayList.clear();
        recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
        userRef.child(mUser.getUid()).child("records").setValue(recordArrayList);
        recordAdapter.notifyItemRemoved(recordPosition);
        recordsRecyclerView.setAdapter(recordAdapter);
    }

    private void getRecords() {
        recordArrayList.clear();
        if (filterBy.equals("All")) {
            switch (sortRecords) {
                case "Date_Desc":
                    recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
                    break;
                case "Date_Asc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsDateAsc());
                    break;
                case "Miles_Desc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsMilesDesc());
                    break;
                case "Miles_Asc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsMilesASC());
                    break;
                case "Title_Asc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsTitleAsc());
                    break;
                case "Title_Desc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsTitleDesc());
                    break;
            }
        } else {
            String filteredVehicle = "";
            vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
            for (Vehicle vehicle:vehicleArrayList) {
                if (filterBy.equals(vehicle.vehicleTitle())) {
                    filteredVehicle = vehicle.vehicleTitle();
                }
            }
            switch (sortRecords) {
                case "Date_Desc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicle(filteredVehicle));
                    break;
                case "Date_Asc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicleDateASC(filteredVehicle));
                    break;
                case "Miles_Desc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicleMilesDesc(filteredVehicle));
                    break;
                case "Miles_Asc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicleMilesAsc(filteredVehicle));
                    break;
                case "Title_Asc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicleTitleAsc(filteredVehicle));
                    break;
                case "Title_Desc":
                    recordArrayList.addAll(recordDatabase.recordDao().getRecordsByVehicleTitleDesc(filteredVehicle));
                    break;
            }
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
}