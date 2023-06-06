package com.example.myapp;

import android.annotation.SuppressLint;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    private ArrayList<Record> recordArrayList;
    private final ArrayList<Vehicle> vehicleArrayList;
    private final static int FADE_DURATION = 350;

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
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
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
    }

    @Override
    public int getItemCount() {
        return recordArrayList.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {

        TextView recordTitle, recordDate, recordVehicle, recordOdometer, recordDescription;
        LinearLayout record_notes_layout;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
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

}
