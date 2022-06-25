package com.example.myapp;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.data.Record;

import java.util.ArrayList;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {
    private ArrayList<Record> recordArrayList;

    public RecordAdapter(ArrayList<Record> recordArrayList) {
        this.recordArrayList = recordArrayList;
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
        holder.recordTitle.setText(recordArrayList.get(position).getTitle());
        holder.recordDate.setText(recordArrayList.get(position).getDate());
        holder.recordVehicle.setText(recordArrayList.get(position).getVehicle());
        holder.recordOdometer.setText(recordArrayList.get(position).getOdometer());
        holder.recordDescription.setText(recordArrayList.get(position).getDescription());
    }

    @Override
    public int getItemCount() {
        return recordArrayList.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {

        TextView recordTitle, recordDate, recordVehicle, recordOdometer, recordDescription;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            recordTitle = itemView.findViewById(R.id.record_title);
            recordDate = itemView.findViewById(R.id.record_date);
            recordVehicle = itemView.findViewById(R.id.record_vehicle);
            recordOdometer = itemView.findViewById(R.id.record_odometer);
            recordDescription = itemView.findViewById(R.id.record_description);
        }
    }
}
