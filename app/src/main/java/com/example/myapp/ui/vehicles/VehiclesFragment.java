package com.example.myapp.ui.vehicles;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myapp.R;
import com.example.myapp.VehicleAdapter;
import com.example.myapp.data.Record;
import com.example.myapp.data.Task;
import com.example.myapp.data.Vehicle;
import com.example.myapp.databinding.FragmentVehiclesBinding;
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
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class VehiclesFragment extends Fragment {

    private FragmentVehiclesBinding binding;
    private boolean shouldRefreshOnResume = false;
    private View root;
    private Vehicle vehicle = new Vehicle();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private final ArrayList<Vehicle> vehicles = new ArrayList<>();
    private final ArrayList<Record> recordArrayList = new ArrayList<>();
    private final ArrayList<Record> oldRecordArrayList = new ArrayList<>();
    private final ArrayList<Task> taskArrayList = new ArrayList<>();
    private final ArrayList<Task> oldTaskArrayList = new ArrayList<>();
    private RecyclerView vehiclesRecyclerView;
    private VehicleAdapter vehicleAdapter;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private String sortVehicles;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;
    private View editVehiclePopup;
    private ValueEventListener eventListener;

    //API Stuff
    private int year;
    private String make, model, submodel, engine;
    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    private String getMakes;
    private String getModels;
    private String getSubmodels;
    private String getEngine;
    private String model_engine_type, model_engine_cyl, model_engine_cc;
    private final ArrayList<String> makeOptions = new ArrayList<>();
    private final ArrayList<String> modelOptions = new ArrayList<>();
    private final ArrayList<String> submodelOptions = new ArrayList<>();
    private AutoCompleteTextView vehicleMakePicker, vehicleModelPicker, vehicleSubmodelPicker;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        VehiclesViewModel vehiclesViewModel =
                new ViewModelProvider(this).get(VehiclesViewModel.class);

        binding = FragmentVehiclesBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        root = binding.getRoot();
        sharedPref = getContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        editor = sharedPref.edit();
        sortVehicles = sharedPref.getString("sort_vehicles", "make_asc");

        vehiclesRecyclerView = root.findViewById(R.id.vehicles_recyclerview);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        vehiclesRecyclerView.setLayoutManager(layoutManager);
        vehiclesRecyclerView.setItemAnimator(new DefaultItemAnimator());
        vehicleAdapter = new VehicleAdapter(vehicleArrayList);
        vehiclesRecyclerView.setAdapter(vehicleAdapter);
        vehiclesRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);

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
                int vehiclePosition = viewHolder.getAdapterPosition();
                vehicle = vehicleArrayList.get(vehiclePosition);
                if (direction == 16){
                    //Swipe Left - Delete Vehicle
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Vehicle")
                            .setMessage("Are you sure you want to delete this vehicle?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteVehicle(vehicle, vehiclePosition);
                                    dialog.dismiss();
                                    Snackbar.make(requireActivity().findViewById(R.id.bottom_nav_view), "Vehicle deleted along with related records and tasks.", Snackbar.LENGTH_LONG)
                                            .setAnchorView(getView().getRootView().findViewById(R.id.bottom_nav_view))
                                            .setAction("Undo", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    undoVehicle(vehicle, vehiclePosition);
                                                }
                                            })
                                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                                            .show();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    vehicleAdapter.notifyItemRangeChanged(0, vehicleArrayList.size());
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(R.drawable.ic_round_warning_24)
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    vehicleAdapter.notifyItemRangeChanged(0, vehicleArrayList.size());
                                }
                            })
                            .show();
                } else if (direction == 32){
                    //Swipe Right - Edit Vehicle
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

        initFirebase();

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

    private void editVehicle(Vehicle oldVehicle, int vehiclePosition) {
        Vehicle newVehicle = new Vehicle();
        Log.d("Old Vehicle", oldVehicle.toString());
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext());
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

        //----------------------------------------------------------------------------------------------------------
        year = Integer.parseInt(editYear.getText().toString().trim());
        make = editMake.getText().toString().trim();
        model = editModel.getText().toString().trim();

        getMakes = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getMakes&year=" + year + "&sold_in_us=1";
        makeOptions.clear();
        // RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(requireContext());
        // String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, getMakes, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response", response.substring(2));
                try {
                    parseResponse(response.substring(2));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            private void parseResponse(String response) throws JSONException {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Makes");
                for (int i = 0; i < jsonArray.length(); i++) {
                    String make_id, make_display, make_is_common, make_country;
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    make_display = jsonObject1.getString("make_display");
                    make_is_common = jsonObject1.getString("make_is_common");

                    if (make_is_common.equals("1")) {
                        makeOptions.add(make_display);
                    }
                }
                int darkMode = sharedPref.getInt("dark_mode", 0);
                if (darkMode == 0) {
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_light, makeOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleMakePicker =
                            editVehiclePopup.findViewById(R.id.edit_make_options);
                    vehicleMakePicker.setAdapter(stringArrayAdapter);
                } else if (darkMode == 1){
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_dark, makeOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleMakePicker =
                            editVehiclePopup.findViewById(R.id.edit_make_options);
                    vehicleMakePicker.setAdapter(stringArrayAdapter);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error", error.toString());
            }
        });
        mRequestQueue.add(mStringRequest);
        getModels = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getModels&make=" + make + "&year=" + year + "&sold_in_us=1";
        modelOptions.clear();
        // RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(requireContext());
        // String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, getModels, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response", response.substring(2));
                try {
                    parseResponse(response.substring(2));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            private void parseResponse(String response) throws JSONException {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Models");
                for (int i = 0; i < jsonArray.length(); i++) {
                    String model_name;
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    model_name = jsonObject1.getString("model_name");
                    modelOptions.add(model_name);
                }
                int darkMode = sharedPref.getInt("dark_mode", 0);
                if (darkMode == 0) {
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_light, modelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleModelPicker =
                            editVehiclePopup.findViewById(R.id.edit_model_options);
                    vehicleModelPicker.setAdapter(stringArrayAdapter);
                } else if (darkMode == 1){
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_dark, modelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleModelPicker =
                            editVehiclePopup.findViewById(R.id.edit_model_options);
                    vehicleModelPicker.setAdapter(stringArrayAdapter);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error", error.toString());
            }
        });
        mRequestQueue.add(mStringRequest);
        getSubmodels = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getTrims&make=" + make + "&year=" + year + "&model=" + model + "&sold_in_us=1";
        submodelOptions.clear();
        // RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(requireContext());
        // String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, getSubmodels, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Response", response.substring(2));
                try {
                    parseResponse(response.substring(2));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            private void parseResponse(String response) throws JSONException {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray jsonArray = jsonObject.getJSONArray("Trims");
                for (int i = 0; i < jsonArray.length(); i++) {
                    String submodel_name;
                    JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                    submodel_name = jsonObject1.getString("model_trim");
                    submodelOptions.add(submodel_name);
                }
                int darkMode = sharedPref.getInt("dark_mode", 0);
                if (darkMode == 0) {
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_light, submodelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleSubmodelPicker =
                            editVehiclePopup.findViewById(R.id.edit_submodel_options);
                    vehicleSubmodelPicker.setAdapter(stringArrayAdapter);
                } else if (darkMode == 1){
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_dark, submodelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleSubmodelPicker =
                            editVehiclePopup.findViewById(R.id.edit_submodel_options);
                    vehicleSubmodelPicker.setAdapter(stringArrayAdapter);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error", error.toString());
            }
        });
        mRequestQueue.add(mStringRequest);
        //--------------------------------------------------------------------------------------------------

        editYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!editYear.getText().toString().trim().isEmpty() || !editYear.getText().toString().trim().equals("")) {
                    year = Integer.parseInt(editYear.getText().toString().trim());
                    if (year >= 1941 & year <= 2023) {
                        getMakes = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getMakes&year=" + year + "&sold_in_us=1";
                        getMakeOptions();
                    } else {
                        editYear.setError("Invalid year");
                    }
                }
            }

            private void getMakeOptions() {
                makeOptions.clear();

                // RequestQueue initialized
                mRequestQueue = Volley.newRequestQueue(requireContext());

                // String Request initialized
                mStringRequest = new StringRequest(Request.Method.GET, getMakes, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response.substring(2));
                        try {
                            parseResponse(response.substring(2));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private void parseResponse(String response) throws JSONException {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("Makes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            String make_id, make_display, make_is_common, make_country;
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            make_display = jsonObject1.getString("make_display");
                            make_is_common = jsonObject1.getString("make_is_common");

                            if (make_is_common.equals("1")) {
                                makeOptions.add(make_display);
                            }
                        }

                        int darkMode = sharedPref.getInt("dark_mode", 0);
                        if (darkMode == 0) {
                            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_light, makeOptions);
                            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                            vehicleMakePicker =
                                    editVehiclePopup.findViewById(R.id.edit_make_options);
                            vehicleMakePicker.setAdapter(stringArrayAdapter);
                        } else if (darkMode == 1){
                            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_item_dark, makeOptions);
                            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                            vehicleMakePicker =
                                    editVehiclePopup.findViewById(R.id.edit_make_options);
                            vehicleMakePicker.setAdapter(stringArrayAdapter);
                        }
                    }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error", error.toString());
                    }
                });

                mRequestQueue.add(mStringRequest);
            }
        });
        editMake.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                make = editMake.getText().toString().trim();
                if (!make.isEmpty()) {
                    getModels = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getModels&make=" + make + "&year=" + year + "&sold_in_us=1";
                    getModelOptions();
                }
            }

            private void getModelOptions() {
                modelOptions.clear();

                // RequestQueue initialized
                mRequestQueue = Volley.newRequestQueue(requireContext());

                // String Request initialized
                mStringRequest = new StringRequest(Request.Method.GET, getModels, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response.substring(2));
                        try {
                            parseResponse(response.substring(2));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private void parseResponse(String response) throws JSONException {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("Models");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            String model_name;
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            model_name = jsonObject1.getString("model_name");
                            modelOptions.add(model_name);
                        }

                        int darkMode = sharedPref.getInt("dark_mode", 0);
                        if (darkMode == 0) {
                            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_light, modelOptions);
                            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                            vehicleModelPicker =
                                    editVehiclePopup.findViewById(R.id.edit_model_options);
                            vehicleModelPicker.setAdapter(stringArrayAdapter);
                        } else if (darkMode == 1){
                            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_dark, modelOptions);
                            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                            vehicleModelPicker =
                                    editVehiclePopup.findViewById(R.id.edit_model_options);
                            vehicleModelPicker.setAdapter(stringArrayAdapter);
                        }
                    }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error", error.toString());
                    }
                });

                mRequestQueue.add(mStringRequest);
            }
        });
        editModel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!model.isEmpty()) {
                    model = editModel.getText().toString().trim();
                    getSubmodels = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getTrims&make=" + make + "&year=" + year + "&model=" + model + "&sold_in_us=1";
                    getSubmodelOptions();
                }
            }

            private void getSubmodelOptions() {
                submodelOptions.clear();

                // RequestQueue initialized
                mRequestQueue = Volley.newRequestQueue(requireContext());

                // String Request initialized
                mStringRequest = new StringRequest(Request.Method.GET, getSubmodels, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response.substring(2));
                        try {
                            parseResponse(response.substring(2));
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    private void parseResponse(String response) throws JSONException {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray jsonArray = jsonObject.getJSONArray("Trims");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            String submodel_name;
                            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                            submodel_name = jsonObject1.getString("model_trim");
                            submodelOptions.add(submodel_name);
                        }

                        int darkMode = sharedPref.getInt("dark_mode", 0);
                        if (darkMode == 0) {
                            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_light, submodelOptions);
                            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                            vehicleSubmodelPicker =
                                    editVehiclePopup.findViewById(R.id.edit_submodel_options);
                            vehicleSubmodelPicker.setAdapter(stringArrayAdapter);
                        } else if (darkMode == 1){
                            ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_item_dark, submodelOptions);
                            stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                            vehicleSubmodelPicker =
                                    editVehiclePopup.findViewById(R.id.edit_submodel_options);
                            vehicleSubmodelPicker.setAdapter(stringArrayAdapter);
                        }
                    }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error", error.toString());
                    }
                });

                mRequestQueue.add(mStringRequest);
            }
        });
        editSubmodel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String trimPick = editSubmodel.getText().toString().trim();
                if (!trimPick.isEmpty() & trimPick.contains("(")) {
                    String trim = trimPick.split("[(]")[0];
                    trim = trim.trim();
                    String engine = trimPick.split("[(]")[1];
                    engine = engine.substring(0, engine.length() - 1);

                    editSubmodel.setText(trim);
                    editEngine.setText(engine);
                }
            }
        });

        dialogBuilder.setView(editVehiclePopup);
        dialog = dialogBuilder.create();
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnim;
        dialog.show();
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                vehicleAdapter.notifyItemChanged(vehiclePosition);
                dialogInterface.cancel();
                dialog.dismiss();
            }
        });

        editVehicleCancelBtn.setOnClickListener(view -> {
            vehicleAdapter.notifyItemChanged(vehiclePosition);
            dialog.dismiss();
        });
        editVehicleFinishBtn.setOnClickListener(view -> {
            int year = Integer.parseInt(editYear.getText().toString().trim());
            int errors = 0;
            if (editYear.getText().toString().trim().isEmpty()) {
                editYear.setError("Enter the year of the vehicle");
                errors++;
            }
            if (year < 1941 || year > 2023) {
                editYear.setError("Invalid year");
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

                vehicleArrayList.remove(vehiclePosition);
                vehicleAdapter.notifyItemRemoved(vehiclePosition);
                vehicleArrayList.add(vehiclePosition, newVehicle);
                vehicleAdapter.notifyItemInserted(vehiclePosition);
                userRef.child("vehicles").setValue(vehicleArrayList);
                for (Record record:recordArrayList) {
                    if (record.getVehicle().equals(oldVehicle.vehicleTitle())) {
                        Record newRecord = record;
                        newRecord.setVehicle(newVehicle.vehicleTitle());
                        recordArrayList.set(recordArrayList.indexOf(record), newRecord);
                    }
                }
                userRef.child("records").setValue(recordArrayList);
                dialog.dismiss();
            }
        });
    }

    private void undoVehicle(Vehicle vehicle, int vehiclePosition) {
        vehicleArrayList.add(vehiclePosition, vehicle);
        vehicleAdapter.notifyItemInserted(vehiclePosition);
        taskArrayList.clear();
        taskArrayList.addAll(oldTaskArrayList);
        recordArrayList.clear();
        recordArrayList.addAll(oldRecordArrayList);
        userRef.child("vehicles").setValue(vehicleArrayList);
        userRef.child("records").setValue(recordArrayList);
        userRef.child("tasks").setValue(taskArrayList);
    }

    private void deleteVehicle(Vehicle vehicle, int vehiclePosition) {
        vehicleArrayList.remove(vehicle);

        oldRecordArrayList.addAll(recordArrayList);
        for (Record record:oldRecordArrayList) {
            if (Objects.equals(record.getVehicle(), String.valueOf(vehicle.getVehicleId()))) {
                recordArrayList.remove(record);
            }
        }

        oldTaskArrayList.addAll(taskArrayList);
        for (Task task:oldTaskArrayList) {
            if (Objects.equals(task.getTaskVehicle(), String.valueOf(vehicle.getVehicleId()))) {
                taskArrayList.remove(task);
            }
        }

        userRef.child("vehicles").setValue(vehicleArrayList);
        userRef.child("records").setValue(recordArrayList);
        userRef.child("tasks").setValue(taskArrayList);
        vehicleAdapter.notifyItemRemoved(vehiclePosition);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        userRef = database.getReference("users").child(mUser.getUid());
        if (vehicleArrayList.size() == 0) addEventListener(userRef);
    }

    private void addEventListener(DatabaseReference userRef) {
        eventListener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.child("tasks").getChildren()) {
                    taskArrayList.add(dataSnapshot.getValue(Task.class));
                }
                recordArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.child("records").getChildren()) {
                    recordArrayList.add(dataSnapshot.getValue(Record.class));
                }
                vehicleArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.child("vehicles").getChildren()) {
                    vehicleArrayList.add(dataSnapshot.getValue(Vehicle.class));
                }
                switch (sortVehicles) {
                    case "year_desc":
                        Collections.sort(vehicleArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Vehicle p1 = (Vehicle) o1;
                                Vehicle p2 = (Vehicle) o2;
                                c = p1.getYear().compareToIgnoreCase(p2.getYear());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        Collections.reverse(vehicleArrayList);
                        break;
                    case "year_asc":
                        Collections.sort(vehicleArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Vehicle p1 = (Vehicle) o1;
                                Vehicle p2 = (Vehicle) o2;
                                c = p1.getYear().compareToIgnoreCase(p2.getYear());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        break;
                    case "make_desc":
                        Collections.sort(vehicleArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Vehicle p1 = (Vehicle) o1;
                                Vehicle p2 = (Vehicle) o2;
                                c = p1.getMake().compareToIgnoreCase(p2.getMake());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        Collections.reverse(vehicleArrayList);
                        break;
                    case "make_asc":
                        Collections.sort(vehicleArrayList, new Comparator() {
                            @Override
                            public int compare(Object o1, Object o2) {
                                int c;
                                Vehicle p1 = (Vehicle) o1;
                                Vehicle p2 = (Vehicle) o2;
                                c = p1.getMake().compareToIgnoreCase(p2.getMake());
                                if (c == 0)
                                    c = p1.getEntryTime().compareTo(p2.getEntryTime());
                                return c;
                            }
                        });
                        break;
                }
                vehicleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ERROR", "loadEvent:onCancelled", error.toException());
            }
        };
        userRef.addValueEventListener(eventListener);
    }

    @Override
    public void onStart() {
        Log.d("Start", "Start");
        super.onStart();
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
            Log.d("Refresh", "Refresh");
            requireActivity().recreate();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        shouldRefreshOnResume = true;
    }
}