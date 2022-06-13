package com.example.myapp.ui.vehicles;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class VehiclesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public VehiclesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Vehicles List");
    }

    public LiveData<String> getText() {
        return mText;
    }
}