package com.example.myapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VehicleDao {

    @Insert
    void addVehicle(Vehicle... vehicles);

    @Query("SELECT * FROM vehicles WHERE vehicleId LIKE :vehicleID")
    List<Vehicle> getVehicle(int vehicleID);

    @Delete
    int deleteVehicle(Vehicle... vehicles);

    @Query("DELETE FROM vehicles")
    void deleteAllVehicles();

    @Update
    int updateVehicle(Vehicle... vehicles);

    @Query("SELECT * FROM vehicles ORDER BY year DESC")
    List<Vehicle> getAllVehicles();

    @Query("SELECT * FROM vehicles ORDER BY year ASC")
    List<Vehicle> getAllVehiclesYearAsc();

    @Query("SELECT * FROM vehicles ORDER BY make ASC")
    List<Vehicle> getAllVehiclesMakeAsc();

    @Query("SELECT * FROM vehicles ORDER BY make DESC")
    List<Vehicle> getAllVehiclesMakeDesc();
}
