package com.example.myapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AchievementsActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private ValueEventListener achListener;

    private CheckBox foc_box;
    private CheckBox hm_box;
    private CheckBox vhm_box;
    private CheckBox ehm_box;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Add Record");
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_achievements);

        foc_box = findViewById(R.id.foc_box);
        hm_box = findViewById(R.id.hm_box);
        ehm_box = findViewById(R.id.ehm_box);
        vhm_box = findViewById(R.id.vhm_box);

        setSupportActionBar(findViewById(R.id.achievements_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Achievements");
        }

        initFirebase();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("users/" + mUser.getUid() + "/achievements");

        addAchievementsEventListener(databaseReference);
    }

    private void addAchievementsEventListener(DatabaseReference achRef) {
        achListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("first_oil_change").exists()) {
                    foc_box.setChecked(dataSnapshot.child("first_oil_change").getValue().toString().equals("true"));
                }
                if (dataSnapshot.child("high_mileage").exists()) {
                    hm_box.setChecked(dataSnapshot.child("high_mileage").getValue().toString().equals("true"));
                }
                if (dataSnapshot.child("very_high_mileage").exists()) {
                    vhm_box.setChecked(dataSnapshot.child("very_high_mileage").getValue().toString().equals("true"));
                }
                if (dataSnapshot.child("extremely_high_mileage").exists()) {
                    ehm_box.setChecked(dataSnapshot.child("extremely_high_mileage").getValue().toString().equals("true"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ERROR", "loadExp:onCancelled", databaseError.toException());
            }
        };
        achRef.addValueEventListener(achListener);
    }

    @Override
    protected void onDestroy() {
        databaseReference.removeEventListener(achListener);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}