package com.example.myapp.ui.home;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

import com.example.myapp.AddRecord;
import com.example.myapp.R;
import com.example.myapp.RecordAdapter;
import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EventListener;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private boolean shouldRefreshOnResume = false;
    private View root;
    private Record record = new Record();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private final ArrayList<String> vehicleOptions = new ArrayList<>();
    private RecyclerView recordsRecyclerView;
    private RecordAdapter recordAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private final Calendar myCalendar = Calendar.getInstance();
    private String filterBy, sortRecords;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;
    private AutoCompleteTextView recordVehiclePicker;
    private String recordDateString;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        sharedPref = getContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        filterBy = sharedPref.getString("filter_by_value", "All");
        sortRecords = sharedPref.getString("sort_records", "date_desc");

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        root = binding.getRoot();

        recordsRecyclerView = root.findViewById(R.id.records_recyclerview);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recordsRecyclerView.setLayoutManager(layoutManager);
        recordsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        recordAdapter = new RecordAdapter(recordArrayList, vehicleArrayList);
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
                record = recordArrayList.get(recordPosition);
                if (direction == 16){
                    //Swipe Left - Delete Record
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Record")
                            .setMessage("Are you sure you want to delete this record?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteRecord(record, recordPosition);
                                    dialog.dismiss();
                                    Snackbar.make(requireActivity().findViewById(R.id.bottom_nav_view), "Record Deleted", Snackbar.LENGTH_LONG)
                                            .setAnchorView(getView().getRootView().findViewById(R.id.bottom_nav_view))
                                            .setAction("Undo", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    undoRecord(record, recordPosition);
                                                }
                                            })
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    recordAdapter.notifyItemRangeChanged(0, recordArrayList.size());
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_round_warning_24)
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    recordAdapter.notifyItemRangeChanged(0, recordArrayList.size());
                                }
                            })
                            .show();
                } else if (direction == 32){
                    //Swipe Right - Edit Record
                    try {
                        editRecord(record, recordPosition);
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
        itemTouchHelper.attachToRecyclerView(recordsRecyclerView);

        initFirebase();
        addEventListener(userRef);

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

    private void editRecord(Record editRecord, int recordPosition) throws ParseException {
        Record newRecord = new Record();
        final MaterialAlertDialogBuilder[] dialogBuilder = {new MaterialAlertDialogBuilder(requireContext())};
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
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        recordDateString = editRecord.getDate();
        editDate.setText(SimpleDateFormat.getDateInstance().format(Objects.requireNonNull(format.parse(editRecord.getDate()))));
        editRecordVehicle = recordVehicleLayout.getEditText();
        String vehicleTitle = null;
        for (Vehicle vehicle:vehicleArrayList) {
            if (String.valueOf(vehicle.getVehicleId()).equals(editRecord.getVehicle())) {
                vehicleTitle = vehicle.vehicleTitle();
            }
        }
        editRecordVehicle.setText(vehicleTitle);
        editOdometer = recordOdometerLayout.getEditText();
        editOdometer.setText(editRecord.getOdometer());
        editDescription = recordNotesLayout.getEditText();
        editDescription.setText(editRecord.getDescription());

        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar myCalendar = Calendar.getInstance();

                DatePickerDialog.OnDateSetListener datePicker = (dateView, year, monthOfYear, dayOfMonth) -> {
                    myCalendar.set(Calendar.YEAR, year);
                    myCalendar.set(Calendar.MONTH, monthOfYear);
                    myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    recordDateString = sdf.format(myCalendar.getTime());
                    editDate.setText(SimpleDateFormat.getDateInstance().format(myCalendar.getTime()));
                };

                if (!editDate.getText().toString().isEmpty()) {
                    Date date;
                    try {
                        date = SimpleDateFormat.getDateInstance().parse(editDate.getText().toString());
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

                /*
                Date displayDate = null;
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    displayDate = format.parse(recordDateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                assert displayDate != null;
                MaterialDatePicker<Long> materialDatePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Date of Work")
                        .setSelection(displayDate.getTime())
                        .build();
                materialDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                    @Override
                    public void onPositiveButtonClick(Long selection) {
                        TimeZone timeZoneUTC = TimeZone.getDefault();
                        int offsetFromUTC = timeZoneUTC.getOffset(new Date().getTime()) * -1;
                        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date date = new Date(selection + offsetFromUTC);
                        recordDateString = simpleFormat.format(date);
                        editDate.setText(SimpleDateFormat.getDateInstance().format(date));
                    }
                });
                materialDatePicker.show(getChildFragmentManager(), "date");

                 */
            }
        });

        vehicleOptions.clear();
        int darkMode = sharedPref.getInt("dark_mode", 0);
        for (Vehicle vehicle: vehicleArrayList) {
            vehicleOptions.add(vehicle.vehicleTitle());
        }
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
        final int[] vehicleSelection = new int[1];
        recordVehiclePicker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                vehicleSelection[0] = i;
            }
        });

        Button editRecordCancelBtn = editRecordPopup.findViewById(R.id.edit_record_cancel_btn);
        Button editRecordFinishBtn = editRecordPopup.findViewById(R.id.edit_record_finish_btn);

        dialogBuilder[0].setView(editRecordPopup);
        dialog = dialogBuilder[0].create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                recordAdapter.notifyItemChanged(recordPosition);
                dialogInterface.cancel();
                dialog.dismiss();
            }
        });

        editRecordCancelBtn.setOnClickListener(view -> {
            recordAdapter.notifyItemChanged(recordPosition);
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
                newRecord.setDate(recordDateString);
                newRecord.setVehicle(String.valueOf(vehicleArrayList.get(vehicleSelection[0]).getVehicleId()));
                newRecord.setOdometer(editOdometer.getText().toString().trim());
                newRecord.setDescription(editDescription.getText().toString().trim());
                newRecord.setEntryTime(editRecord.getEntryTime());
                recordArrayList.remove(recordPosition);
                recordAdapter.notifyItemRemoved(recordPosition);
                recordArrayList.add(recordPosition, newRecord);
                recordAdapter.notifyItemInserted(recordPosition);
                userRef.child("records").setValue(recordArrayList);
                dialog.dismiss();
            }
        });
    }

    private void undoRecord(Record record, int recordPosition) {
        recordArrayList.add(recordPosition, record);
        userRef.child("records").setValue(recordArrayList);
        recordAdapter.notifyItemInserted(recordPosition);
    }

    private void deleteRecord(Record record, int recordPosition) {
        recordArrayList.remove(recordPosition);
        userRef.child("records").setValue(recordArrayList);
        recordAdapter.notifyItemRemoved(recordPosition);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        userRef = database.getReference("users").child(mUser.getUid());
    }

    private void addEventListener(DatabaseReference userRef) {
        ValueEventListener eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                vehicleArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.child("vehicles").getChildren()) {
                    vehicleArrayList.add(dataSnapshot.getValue(Vehicle.class));
                }
                recordArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.child("records").getChildren()) {
                    recordArrayList.add(dataSnapshot.getValue(Record.class));
                }
                switch (sortRecords) {
                    case "date_desc":
                        Collections.sort(recordArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Record p1 = (Record) o1;
                                Record p2 = (Record) o2;
                                c = p1.getDate().compareToIgnoreCase(p2.getDate());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        Collections.reverse(recordArrayList);
                        break;
                    case "date_asc":
                        Collections.sort(recordArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Record p1 = (Record) o1;
                                Record p2 = (Record) o2;
                                c = p1.getDate().compareToIgnoreCase(p2.getDate());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        break;
                    case "miles_desc":
                        Collections.sort(recordArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c = 0;
                                Record p1 = (Record) o1;
                                Record p2 = (Record) o2;
                                if (Integer.parseInt(p1.getOdometer()) > Integer.parseInt(p2.getOdometer())) c = -1;
                                else if (Integer.parseInt(p1.getOdometer()) < Integer.parseInt(p2.getOdometer())) c = 1;
                                else if (Integer.parseInt(p1.getOdometer()) == Integer.parseInt(p2.getOdometer())) c = 0;
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        break;
                    case "miles_asc":
                        Collections.sort(recordArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c = 0;
                                Record p1 = (Record) o1;
                                Record p2 = (Record) o2;
                                if (Integer.parseInt(p1.getOdometer()) > Integer.parseInt(p2.getOdometer())) c = -1;
                                else if (Integer.parseInt(p1.getOdometer()) < Integer.parseInt(p2.getOdometer())) c = 1;
                                else if (Integer.parseInt(p1.getOdometer()) == Integer.parseInt(p2.getOdometer())) c = 0;
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        Collections.reverse(recordArrayList);
                        break;
                    case "title_desc":
                        Collections.sort(recordArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Record p1 = (Record) o1;
                                Record p2 = (Record) o2;
                                c = p1.getTitle().compareToIgnoreCase(p2.getTitle());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        Collections.reverse(recordArrayList);
                        break;
                    case "title_asc":
                        Collections.sort(recordArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Record p1 = (Record) o1;
                                Record p2 = (Record) o2;
                                c = p1.getTitle().compareToIgnoreCase(p2.getTitle());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        break;
                }
                if (!filterBy.equals("All")) {
                    ArrayList<Record> dummyRecords = new ArrayList<>(recordArrayList);
                    for (Record record:dummyRecords) {
                        if (!record.getVehicle().equals(filterBy)) recordArrayList.remove(record);
                    }
                }
                recordAdapter.notifyItemRangeChanged(0, recordArrayList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ERROR", "loadEvent:onCancelled", error.toException());
            }
        };
        userRef.addValueEventListener(eventListener);
    }
}