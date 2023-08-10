package com.example.myapp;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class AchievementsActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private ValueEventListener achListener;
    private MaterialCardView foc_card, hm_card, ehm_card, vhm_card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Add Record");
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_achievements);

        setSupportActionBar(findViewById(R.id.achievements_tb));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Achievements");
        }

        initFirebase();
    }

    private void initVars() {
        foc_card = findViewById(R.id.foc_card);
        hm_card = findViewById(R.id.hm_card);
        vhm_card = findViewById(R.id.vhm_card);
        ehm_card = findViewById(R.id.ehm_card);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("users/" + mUser.getUid() + "/achievements");

        initVars();

        addAchievementsEventListener(databaseReference);
    }

    private void addAchievementsEventListener(DatabaseReference achRef) {
        achListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (Objects.equals(dataSnapshot.child("first_oil_change").getValue(), "true")) {
                    foc_card.setChecked(true);
                }
                if (Objects.equals(dataSnapshot.child("high_mileage").getValue(), "true")) {
                    hm_card.setChecked(true);
                }
                if (Objects.equals(dataSnapshot.child("very_high_mileage").getValue(), "true")) {
                    vhm_card.setChecked(true);
                }
                if (Objects.equals(dataSnapshot.child("extremely_high_mileage").getValue(), "true")) {
                    ehm_card.setChecked(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
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