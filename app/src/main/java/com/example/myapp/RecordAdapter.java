package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.data.Record;
import com.example.myapp.data.Vehicle;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    private ArrayList<Record> recordArrayList;
    private final ArrayList<Vehicle> vehicleArrayList;
    private final static int FADE_DURATION = 350;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private FirebaseDatabase mDatabase;
    private DatabaseReference userRef;
    private Record record = new Record();

    public RecordAdapter(ArrayList<Record> recordArrayList, ArrayList<Vehicle> vehicleArrayList) {
        this.recordArrayList = recordArrayList;
        this.vehicleArrayList = vehicleArrayList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(ArrayList<Record> filterList) {
        recordArrayList = filterList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.record_view, parent, false);
        MaterialCardView recordView = view.findViewById(R.id.record_material_card);
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!recordView.isChecked()) {
                    new MaterialAlertDialogBuilder(parent.getContext())
                            .setTitle("Test")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    recordView.setChecked(true);
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
                    Snackbar.make(parent.getContext(), parent.getRootView(), "Task already completed.", Snackbar.LENGTH_SHORT)
                            .setAnchorView(parent.getRootView().findViewById(R.id.bottom_nav_view))
                            .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                            .show();
                }
                return true;
            }
        });
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordAdapter.RecordViewHolder holder, int position) {
        Date displayDate = null;
        String date = null;
        String vehicleTitle = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            displayDate = format.parse(recordArrayList.get(position).getDate());
            assert displayDate != null;
            date = SimpleDateFormat.getDateInstance().format(displayDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        for (Vehicle vehicle:vehicleArrayList) {
            if (String.valueOf(vehicle.getVehicleId()).equals(recordArrayList.get(position).getVehicle())) {
                vehicleTitle = vehicle.vehicleTitle();
            }
        }

        holder.recordTitle.setText(recordArrayList.get(position).getTitle());
        holder.recordDate.setText(date);
        holder.recordVehicle.setText(vehicleTitle);
        holder.recordOdometer.setText(recordArrayList.get(position).getOdometer());
        holder.recordDescription.setText(recordArrayList.get(position).getDescription());

        if (recordArrayList.get(position).getDescription().equals("") || recordArrayList.get(position).getDescription().isEmpty()) {
            holder.record_notes_layout.setVisibility(View.GONE);
        } else {
            holder.record_notes_layout.setVisibility(View.VISIBLE);
        }

        setFadeAnimation(holder.itemView);

        holder.recordView.setOnCheckedChangeListener(new MaterialCardView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MaterialCardView card, boolean isChecked) {
                if (isChecked) {
                    record = recordArrayList.get(holder.getAdapterPosition());
                    Log.d("Record", record.toString());
                    holder.recordView.setChecked(false);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return recordArrayList.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView recordView;
        TextView recordTitle, recordDate, recordVehicle, recordOdometer, recordDescription;
        LinearLayout record_notes_layout;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            recordView = itemView.findViewById(R.id.record_material_card);
            recordTitle = itemView.findViewById(R.id.record_title);
            recordDate = itemView.findViewById(R.id.record_date);
            recordVehicle = itemView.findViewById(R.id.record_vehicle);
            recordOdometer = itemView.findViewById(R.id.record_odometer);
            recordDescription = itemView.findViewById(R.id.record_description);
            record_notes_layout = itemView.findViewById(R.id.record_notes_layout);
        }
    }

    private void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(FADE_DURATION);
        view.startAnimation(anim);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference("users/" + mUser.getUid());
    }
}
