package com.example.myapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VehicleDao {

    @Insert
    void insertVehicle(Vehicle... vehicles);

    @Query("SELECT * FROM vehicles WHERE vehicleId LIKE :vehicleID")
    List<Vehicle> retrieveVehicle(int vehicleID);

    @Delete
    int deleteVehicle(Vehicle... vehicles);

    @Query("DELETE FROM vehicles")
    void deleteAllVehicles();

    @Update
    int updateVehicle(Vehicle... vehicles);

    @Query("SELECT * FROM vehicles ORDER BY year ASC")
    List<Vehicle> getAllVehicles();
}
