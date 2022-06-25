package com.example.myapp;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.data.Record;
import com.example.myapp.data.Vehicle;

import java.util.ArrayList;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {
    private ArrayList<Vehicle> vehicleArrayList;

    public VehicleAdapter(ArrayList<Vehicle> vehicleArrayList) {
        this.vehicleArrayList = vehicleArrayList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterList(ArrayList<Vehicle> filterList) {
        vehicleArrayList = filterList;
        notifyDataSetChanged();
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
        holder.vehicleYear.setText(vehicleArrayList.get(position).getYear());
        holder.vehicleMake.setText(vehicleArrayList.get(position).getMake());
        holder.vehicleModel.setText(vehicleArrayList.get(position).getModel());
        holder.vehicleSubmodel.setText(vehicleArrayList.get(position).getSubmodel());
        holder.vehicleEngine.setText(vehicleArrayList.get(position).getEngine());
        holder.vehicleNotes.setText(vehicleArrayList.get(position).getNotes());
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

        TextView vehicleYear, vehicleMake, vehicleModel, vehicleSubmodel, vehicleEngine, vehicleNotes;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            vehicleYear = itemView.findViewById(R.id.vehicle_year);
            vehicleMake = itemView.findViewById(R.id.vehicle_make);
            vehicleModel = itemView.findViewById(R.id.vehicle_model);
            vehicleSubmodel = itemView.findViewById(R.id.vehicle_submodel);
            vehicleEngine = itemView.findViewById(R.id.vehicle_engine);
            vehicleNotes = itemView.findViewById(R.id.vehicle_notes);
        }
    }
}
