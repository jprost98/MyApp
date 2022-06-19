package com.example.myapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.data.Vehicle;

import java.util.ArrayList;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {
    private ArrayList<Vehicle> vehicleArrayList;

    public VehicleAdapter(ArrayList<Vehicle> vehicleArrayList) {
        this.vehicleArrayList = vehicleArrayList;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.vehicle_view, parent, false);
        return new VehicleAdapter.VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        holder.vehicleTitle.setText(vehicleArrayList.get(position).getYear());
        holder.vehicleDate.setText(vehicleArrayList.get(position).getMake());
        holder.vehicleVehicle.setText(vehicleArrayList.get(position).getModel());
        holder.vehicleOdometer.setText(vehicleArrayList.get(position).getSubmodel());
        holder.vehicleDescription.setText(vehicleArrayList.get(position).getEngine());
    }

    public void setVehicleArrayList(ArrayList<Vehicle> vehicleArrayList) {
        this.vehicleArrayList = vehicleArrayList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return vehicleArrayList.size();
    }

    public static class VehicleViewHolder extends RecyclerView.ViewHolder {

        TextView vehicleTitle, vehicleDate, vehicleVehicle, vehicleOdometer, vehicleDescription;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            vehicleTitle = itemView.findViewById(R.id.vehicle_year);
            vehicleDate = itemView.findViewById(R.id.vehicle_make);
            vehicleVehicle = itemView.findViewById(R.id.vehicle_model);
            vehicleOdometer = itemView.findViewById(R.id.vehicle_submodel);
            vehicleDescription = itemView.findViewById(R.id.vehicle_engine);
        }
    }
}
