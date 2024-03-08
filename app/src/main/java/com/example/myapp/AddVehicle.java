package com.example.myapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.myapp.data.Vehicle;
import com.example.myapp.data.VehicleDao;
import com.example.myapp.data.VehicleDatabase;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

public class AddVehicle extends AppCompatActivity {

    private int darkMode;
    private SharedPreferences sharedPref;
    private final Vehicle vehicle = new Vehicle();
    private final ArrayList<Vehicle> vehicleArrayList = new ArrayList<>();
    private EditText vehicleYear, vehicleMake, vehicleModel, vehicleSubmodel, vehicleEngine, vehicleNotes;
    private TextInputLayout vehicleYearLayout, vehicleMakeLayout, vehicleModelLayout, vehicleSubmodelLayout, vehicleEngineLayout, vehicleNotesLayout;
    private VehicleDatabase vehicleDatabase;
    private VehicleDao vehicleDao;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference userRef = database.getReference("users");

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = getApplicationContext().getSharedPreferences("SAVED_PREFERENCES", 0);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_add_vehicle);

        setSupportActionBar(findViewById(R.id.add_vehicle_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Add Vehicle");
        }

        initFirebase();
        initVars();

        vehicleYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!vehicleYear.getText().toString().trim().isEmpty() || !vehicleYear.getText().toString().trim().equals("")) {
                    year = Integer.parseInt(vehicleYear.getText().toString().trim());
                    if (year >= 1941 & year <= 2024) {
                        getMakes = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getMakes&year=" + year + "&sold_in_us=1";
                        getMakeOptions();
                    } else {
                        vehicleYear.setError("Invalid year");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        vehicleMake.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                make = vehicleMake.getText().toString().trim();
                if (!make.isEmpty()) {
                    getModels = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getModels&make=" + make + "&year=" + year + "&sold_in_us=1";
                    getModelOptions();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        vehicleModel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                model = vehicleModel.getText().toString().trim();
                if (!model.isEmpty()) {
                    getSubmodels = "https://www.carqueryapi.com/api/0.3/?callback=?&cmd=getTrims&make=" + make + "&year=" + year + "&model=" + model + "&sold_in_us=1";
                    getSubmodelOptions();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        vehicleSubmodel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String trimPick = vehicleSubmodel.getText().toString().trim();
                if (!trimPick.isEmpty() & trimPick.contains("(")) {
                    String trim = trimPick.split("[(]")[0];
                    trim = trim.trim();
                    String engine = trimPick.split("[(]")[1];
                    engine = engine.substring(0, engine.length() - 1);

                    vehicleSubmodel.setText(trim);
                    vehicleEngine.setText(engine);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        Button addVehicleBtn = findViewById(R.id.add_vehicle_btn);
        addVehicleBtn.setOnClickListener(view -> {
            addVehicle();
            finish();
        });
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void initVars() {
        vehicleDatabase = Room.databaseBuilder(getApplicationContext(), VehicleDatabase.class, "vehicles").allowMainThreadQueries().build();
        vehicleDao = vehicleDatabase.vehicleDao();

        vehicleYearLayout = findViewById(R.id.vehicle_year_input);
        vehicleMakeLayout = findViewById(R.id.vehicle_make_input);
        vehicleModelLayout = findViewById(R.id.vehicle_model_input);
        vehicleSubmodelLayout = findViewById(R.id.vehicle_submodel_input);
        vehicleEngineLayout = findViewById(R.id.vehicle_engine_input);
        vehicleNotesLayout = findViewById(R.id.vehicle_notes_input);

        vehicleYear = vehicleYearLayout.getEditText();
        vehicleMake = vehicleMakeLayout.getEditText();
        vehicleModel = vehicleModelLayout.getEditText();
        vehicleSubmodel = vehicleSubmodelLayout.getEditText();
        vehicleEngine = vehicleEngineLayout.getEditText();
        vehicleNotes = vehicleNotesLayout.getEditText();
    }

    private void addVehicle() {
        int errors = checkVehicleReqs();
        if (errors == 0) {
            vehicle.setYear(vehicleYear.getText().toString().trim());
            vehicle.setMake(vehicleMake.getText().toString().trim());
            vehicle.setModel(vehicleModel.getText().toString().trim());
            vehicle.setSubmodel(vehicleSubmodel.getText().toString().trim());
            vehicle.setEngine(vehicleEngine.getText().toString().trim());
            vehicle.setNotes(vehicleNotes.getText().toString().trim());
            vehicle.setEntryTime(Calendar.getInstance().getTimeInMillis());

            vehicleDao.addVehicle(vehicle);
            vehicleArrayList.clear();
            vehicleArrayList.addAll(vehicleDao.getAllVehicles());
            userRef.child(mUser.getUid()).child("vehicles").setValue(vehicleArrayList);
        }
    }

    private int checkVehicleReqs() {
        year = Integer.parseInt(vehicleYear.getText().toString().trim());
        int errors = 0;
        if (vehicleYear.getText().toString().trim().isEmpty()) {
            vehicleYear.setError("Enter the year of the vehicle");
            errors++;
        }
        if (year < 1941 || year > 2023) {
            vehicleYear.setError("Invalid year");
            errors++;
        }
        if (vehicleMake.getText().toString().trim().isEmpty()) {
            vehicleMake.setError("Enter the make of the vehicle");
            errors++;
        }
        if (vehicleModel.getText().toString().trim().isEmpty()) {
            vehicleModel.setError("Enter the model of the vehicle");
            errors++;
        }
        if (vehicleSubmodel.getText().toString().trim().isEmpty()) {
            vehicleSubmodel.setError("Enter the trim of the vehicle");
            errors++;
        }
        return errors;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getMakeOptions() {
        makeOptions.clear();

        // RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);

        // String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, getMakes, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
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
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_light, makeOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleMakePicker =
                            findViewById(R.id.make_options);
                    vehicleMakePicker.setAdapter(stringArrayAdapter);
                } else if (darkMode == 1){
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_dark, makeOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleMakePicker =
                            findViewById(R.id.make_options);
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

    private void getModelOptions() {
        modelOptions.clear();

        // RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);

        // String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, getModels, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
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
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_light, modelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleModelPicker =
                            findViewById(R.id.model_options);
                    vehicleModelPicker.setAdapter(stringArrayAdapter);
                } else if (darkMode == 1){
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_dark, modelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleModelPicker =
                            findViewById(R.id.model_options);
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

    private void getSubmodelOptions() {
        submodelOptions.clear();

        // RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);

        // String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, getSubmodels, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
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
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_light, submodelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleSubmodelPicker =
                            findViewById(R.id.submodel_options);
                    vehicleSubmodelPicker.setAdapter(stringArrayAdapter);
                } else if (darkMode == 1){
                    ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.spinner_item_dark, submodelOptions);
                    stringArrayAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    vehicleSubmodelPicker =
                            findViewById(R.id.submodel_options);
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
}