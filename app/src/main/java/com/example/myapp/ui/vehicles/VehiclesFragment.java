package com.example.myapp.ui.vehicles;

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
import com.example.myapp.VehicleAdapter;
import com.example.myapp.data.Record;
import com.example.myapp.data.RecordDatabase;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDatabase;
import com.example.myapp.databinding.FragmentVehiclesBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class VehiclesFragment extends Fragment {

    private FragmentVehiclesBinding binding;
    private View root;
    private Vehicle vehicle = new Vehicle();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private final ArrayList<Record> oldRecordArrayList = new ArrayList<>();
    private RecyclerView vehiclesRecyclerView;
    private VehicleAdapter vehicleAdapter;
    private VehicleDatabase vehicleDatabase;
    private RecordDatabase recordDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("users");
    private String sortVehicles;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        VehiclesViewModel vehiclesViewModel =
                new ViewModelProvider(this).get(VehiclesViewModel.class);

        binding = FragmentVehiclesBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        View root = binding.getRoot();
        sharedPref = getContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        sortVehicles = sharedPref.getString("sort_vehicles", "Year_Desc");
        Log.d("Sort Vehicles", sortVehicles);

        recordDatabase = Room.databaseBuilder(requireContext(), RecordDatabase.class, "records").allowMainThreadQueries().build();
        vehicleDatabase = Room.databaseBuilder(requireContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(root.getContext());
        vehiclesRecyclerView = root.findViewById(R.id.vehicles_recyclerview);
        vehicleAdapter = new VehicleAdapter(vehicleArrayList);
        vehiclesRecyclerView.setLayoutManager(layoutManager);
        vehiclesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        vehiclesRecyclerView.addItemDecoration(new DividerItemDecoration(root.getContext(), DividerItemDecoration.VERTICAL));
        vehiclesRecyclerView.setAdapter(vehicleAdapter);

        initFirebase();
        getVehicles();

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
                /*
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    assert viewHolder != null;
                    viewHolder.itemView.setBackgroundColor(Color.GRAY);
                }
                 */
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
                                getContext().getResources(), R.drawable.ic_edit_96);
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
                                getContext().getResources(), R.drawable.ic_delete_96);
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
                int vehiclePosition = viewHolder.getAdapterPosition();
                if (direction == 16){
                    //Swipe Left - Delete Vehicle
                    new AlertDialog.Builder(getContext())
                            .setTitle("Delete vehicle")
                            .setMessage("Are you sure you want to delete this vehicle?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    getRecords();
                                    vehicle = vehicleArrayList.get(vehiclePosition);
                                    deleteVehicle(vehicle, vehiclePosition);
                                    dialog.dismiss();
                                    Snackbar.make(getActivity().findViewById(R.id.bottom_nav_view), "Vehicle Deleted", Snackbar.LENGTH_LONG)
                                            .setAction("Undo", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    undoVehicle(vehicle, vehiclePosition);
                                                }
                                            })
                                            .show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    vehicleAdapter.notifyItemChanged(vehiclePosition);
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_round_warning_24)
                            .show();
                } else if (direction == 32){
                    //Swipe Right - Edit Vehicle
                    getRecords();
                    vehicle = vehicleArrayList.get(vehiclePosition);
                    vehicleAdapter.notifyItemChanged(vehiclePosition);
                    editVehicle(vehicle, vehiclePosition);
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(vehiclesRecyclerView);

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
        ArrayList<Vehicle> filteredList = new ArrayList<>();
        for (Vehicle item : vehicleArrayList) {
            if (item.getYear().toLowerCase().contains(text.toLowerCase())
                    || item.getMake().toLowerCase().contains(text.toLowerCase())
                    || item.getModel().toLowerCase().contains(text.toLowerCase())
                    || item.getSubmodel().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        vehicleAdapter.filterList(filteredList);
    }

    private void getRecords() {
        recordArrayList.clear();
        oldRecordArrayList.clear();
        recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
        oldRecordArrayList.addAll(recordArrayList);
    }

    private void editVehicle(Vehicle oldVehicle, int vehiclePosition) {
        Vehicle newVehicle = new Vehicle();
        Log.d("Old Vehicle", oldVehicle.toString());
        androidx.appcompat.app.AlertDialog.Builder dialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        androidx.appcompat.app.AlertDialog dialog;
        @SuppressLint("InflateParams") final View editVehiclePopup = getLayoutInflater().inflate(R.layout.popup_edit_vehicle, null);

        TextInputLayout vehicleYearLayout, vehicleMakeLayout, vehicleModelLayout, vehicleSubmodelLayout, vehicleEngineLayout, vehicleNotesLayout;

        vehicleYearLayout = editVehiclePopup.findViewById(R.id.edit_vehicle_year_input);
        vehicleMakeLayout = editVehiclePopup.findViewById(R.id.edit_vehicle_make_input);
        vehicleModelLayout = editVehiclePopup.findViewById(R.id.edit_vehicle_model_input);
        vehicleSubmodelLayout = editVehiclePopup.findViewById(R.id.edit_vehicle_submodel_input);
        vehicleEngineLayout = editVehiclePopup.findViewById(R.id.edit_vehicle_engine_input);
        vehicleNotesLayout = editVehiclePopup.findViewById(R.id.edit_vehicle_notes_input);

        EditText editYear, editMake, editModel, editSubmodel, editEngine, editNotes;
        editYear = vehicleYearLayout.getEditText();
        editYear.setText(oldVehicle.getYear());
        editMake = vehicleMakeLayout.getEditText();
        editMake.setText(oldVehicle.getMake());
        editModel = vehicleModelLayout.getEditText();
        editModel.setText(oldVehicle.getModel());
        editSubmodel = vehicleSubmodelLayout.getEditText();
        editSubmodel.setText(oldVehicle.getSubmodel());
        editEngine = vehicleEngineLayout.getEditText();
        editEngine.setText(oldVehicle.getEngine());
        editNotes = vehicleNotesLayout.getEditText();
        editNotes.setText(oldVehicle.getNotes());

        Button editVehicleCancelBtn = editVehiclePopup.findViewById(R.id.edit_vehicle_cancel_btn);
        Button editVehicleFinishBtn = editVehiclePopup.findViewById(R.id.edit_vehicle_finish_btn);

        dialogBuilder.setView(editVehiclePopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(false);

        editVehicleCancelBtn.setOnClickListener(view -> {
            dialog.dismiss();
        });
        editVehicleFinishBtn.setOnClickListener(view -> {
            int errors = 0;
            if (editYear.getText().toString().trim().isEmpty()) {
                editYear.setError("Enter the year of the vehicle");
                errors++;
            }
            if (editMake.getText().toString().trim().isEmpty()) {
                editMake.setError("Enter the make of the vehicle");
                errors++;
            }
            if (editModel.getText().toString().trim().isEmpty()) {
                editModel.setError("Enter the model of the vehicle");
                errors++;
            }
            if (editSubmodel.getText().toString().trim().isEmpty()) {
                editSubmodel.setError("Enter the submodel of the vehicle");
                errors++;
            }
            if (errors == 0) {
                newVehicle.setVehicleId(oldVehicle.getVehicleId());
                newVehicle.setYear(editYear.getText().toString().trim());
                newVehicle.setMake(editMake.getText().toString().trim());
                newVehicle.setModel(editModel.getText().toString().trim());
                newVehicle.setSubmodel(editSubmodel.getText().toString().trim());
                newVehicle.setEngine(editEngine.getText().toString().trim());
                newVehicle.setNotes(editNotes.getText().toString().trim());
                newVehicle.setEntryTime(Calendar.getInstance().getTimeInMillis());

                vehicleDatabase.vehicleDao().updateVehicle(newVehicle);
                vehicleArrayList.clear();
                vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
                userRef.child(mUser.getUid()).child("vehicles").setValue(vehicleArrayList);
                for (Record record:recordArrayList) {
                    if (record.getVehicle().equals(oldVehicle.vehicleTitle())) {
                        Record newRecord = record;
                        newRecord.setVehicle(newVehicle.vehicleTitle());
                        recordArrayList.set(recordArrayList.indexOf(record), newRecord);
                        recordDatabase.recordDao().updateRecord(newRecord);
                    }
                }
                userRef.child(mUser.getUid()).child("records").setValue(recordArrayList);
                vehicleAdapter.notifyItemChanged(vehiclePosition);
                vehiclesRecyclerView.setAdapter(vehicleAdapter);
                dialog.dismiss();
                getActivity().recreate();
            }
        });
    }

    private void undoVehicle(Vehicle vehicle, int vehiclePosition) {
        recordDatabase.recordDao().deleteAllRecords();
        for (Record oldRecord:oldRecordArrayList) {
            recordDatabase.recordDao().addRecord(oldRecord);
        }
        vehicleDatabase.vehicleDao().addVehicle(vehicle);
        vehicleArrayList.clear();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        userRef.child(mUser.getUid()).child("vehicles").setValue(vehicleArrayList);
        userRef.child(mUser.getUid()).child("records").setValue(oldRecordArrayList);
        vehicleAdapter.notifyItemInserted(vehiclePosition);
        vehiclesRecyclerView.setAdapter(vehicleAdapter);
        recordArrayList.addAll(oldRecordArrayList);
    }

    private void deleteVehicle(Vehicle vehicle, int vehiclePosition) {
        recordDatabase.recordDao().deleteRecordsOfVehicle(vehicle.vehicleTitle());
        recordArrayList.clear();
        recordArrayList.addAll(recordDatabase.recordDao().getAllRecords());
        vehicleDatabase.vehicleDao().deleteVehicle(vehicle);
        vehicleArrayList.clear();
        vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
        userRef.child(mUser.getUid()).child("vehicles").setValue(vehicleArrayList);
        userRef.child(mUser.getUid()).child("records").setValue(recordArrayList);
        vehicleAdapter.notifyItemRemoved(vehiclePosition);
        vehiclesRecyclerView.setAdapter(vehicleAdapter);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void getVehicles() {
        vehicleArrayList.clear();
        switch (sortVehicles) {
            case "Year_Desc":
                vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehicles());
                break;
            case "Year_Asc":
                vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehiclesYearAsc());
                break;
            case "Make_Asc":
                vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehiclesMakeAsc());
                break;
            case "Make_Desc":
                vehicleArrayList.addAll(vehicleDatabase.vehicleDao().getAllVehiclesMakeDesc());
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}