package com.example.myapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapp.data.Vehicle;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Objects;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {
    private ArrayList<Vehicle> vehicleArrayList;
    private final static int FADE_DURATION = 350;
    private Vehicle vehicle = new Vehicle();

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
        MaterialCardView vehicleView = view.findViewById(R.id.vehicle_material_card);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!vehicleView.isChecked()) {
                    vehicleView.setChecked(true);
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        return new VehicleAdapter.VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        vehicle = vehicleArrayList.get(holder.getAdapterPosition());
        holder.vehicleYear.setText(vehicle.getYear());
        holder.vehicleMake.setText(vehicle.getMake());
        holder.vehicleModel.setText(vehicle.getModel());
        holder.vehicleSubmodel.setText(vehicle.getSubmodel());

        setFadeAnimation(holder.itemView);

        holder.vehicleView.setOnCheckedChangeListener(new MaterialCardView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(MaterialCardView card, boolean isChecked) {
                if (isChecked) {
                    vehicle = vehicleArrayList.get(holder.getAdapterPosition());
                    MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(card.getContext());
                    AlertDialog dialog;
                    LayoutInflater li = (LayoutInflater) card.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    @SuppressLint("InflateParams") final View detailedVehicleView = li.inflate(R.layout.vehicle_detailed_view, null);

                    TextView vehicleYear = detailedVehicleView.findViewById(R.id.vehicle_detailed_year);
                    TextView vehicleMake = detailedVehicleView.findViewById(R.id.vehicle_detailed_make);
                    TextView vehicleModel = detailedVehicleView.findViewById(R.id.vehicle_detailed_model);
                    TextView vehicleSubmodel = detailedVehicleView.findViewById(R.id.vehicle_detailed_submodel);
                    TextView vehicleEngine = detailedVehicleView.findViewById(R.id.vehicle_detailed_engine);
                    TextView vehicleNotes = detailedVehicleView.findViewById(R.id.vehicle_detailed_notes);

                    vehicleYear.setText(vehicle.getYear());
                    vehicleMake.setText(vehicle.getMake());
                    vehicleModel.setText(vehicle.getModel());
                    vehicleSubmodel.setText(vehicle.getSubmodel());
                    vehicleEngine.setText(vehicle.getEngine());
                    if (!vehicle.getNotes().isEmpty()) vehicleNotes.setText(vehicle.getNotes());
                    else vehicleNotes.setText("-----");

                    dialogBuilder.setView(detailedVehicleView);
                    dialog = dialogBuilder.create();
                    Objects.requireNonNull(dialog.getWindow()).getAttributes().windowAnimations = R.style.DialogAnim;
                    dialog.show();
                    dialog.setCancelable(true);

                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            holder.vehicleView.setChecked(false);
                            dialogInterface.cancel();
                            dialog.dismiss();
                        }
                    });
                }
            }
        });
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

        MaterialCardView vehicleView;
        TextView vehicleYear, vehicleMake, vehicleModel, vehicleSubmodel, vehicleEngine, vehicleNotes;
        LinearLayout vehicle_notes_layout;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            vehicleView = itemView.findViewById(R.id.vehicle_material_card);
            vehicleYear = itemView.findViewById(R.id.vehicle_year);
            vehicleMake = itemView.findViewById(R.id.vehicle_make);
            vehicleModel = itemView.findViewById(R.id.vehicle_model);
            vehicleSubmodel = itemView.findViewById(R.id.vehicle_submodel);
            vehicleEngine = itemView.findViewById(R.id.vehicle_engine);
            vehicleNotes = itemView.findViewById(R.id.vehicle_notes);
            vehicle_notes_layout = itemView.findViewById(R.id.vehicle_notes_layout);
        }
    }

    private void setFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(FADE_DURATION);
        view.startAnimation(anim);
    }
}
