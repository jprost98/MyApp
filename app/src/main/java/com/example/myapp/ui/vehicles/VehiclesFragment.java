package com.example.myapp.ui.vehicles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.databinding.FragmentVehiclesBinding;

public class VehiclesFragment extends Fragment {

    private FragmentVehiclesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        VehiclesViewModel vehiclesViewModel =
                new ViewModelProvider(this).get(VehiclesViewModel.class);

        binding = FragmentVehiclesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textVehicles;
        vehiclesViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}