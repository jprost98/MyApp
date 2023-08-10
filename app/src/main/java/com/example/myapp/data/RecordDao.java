package com.example.myapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RecordDao {

    @Insert
    void addRecord(Record... record);

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle ORDER BY date DESC")
    List<Record> getRecordsByVehicle(String recordVehicle);

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle ORDER BY date ASC")
    List<Record> getRecordsByVehicleDateASC(String recordVehicle);

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle ORDER BY odometer DESC")
    List<Record> getRecordsByVehicleMilesDesc(String recordVehicle);

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle ORDER BY odometer ASC")
    List<Record> getRecordsByVehicleMilesAsc(String recordVehicle);

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle ORDER BY title ASC")
    List<Record> getRecordsByVehicleTitleAsc(String recordVehicle);

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle ORDER BY title DESC")
    List<Record> getRecordsByVehicleTitleDesc(String recordVehicle);

    @Query("SELECT * FROM records ORDER BY date DESC")
    List<Record> getAllRecords();

    @Query("SELECT * FROM records ORDER BY date ASC")
    List<Record> getRecordsDateAsc();

    @Query("SELECT * FROM records ORDER BY odometer DESC")
    List<Record> getRecordsMilesDesc();

    @Query("SELECT * FROM records ORDER BY odometer ASC")
    List<Record> getRecordsMilesASC();

    @Query("SELECT * FROM records ORDER BY title ASC")
    List<Record> getRecordsTitleAsc();

    @Query("SELECT * FROM records ORDER BY title DESC")
    List<Record> getRecordsTitleDesc();

    @Delete
    int deleteRecord(Record... record);

    @Update
    int updateRecord(Record... record);

    @Query("DELETE FROM records")
    void deleteAllRecords();

    @Query("DELETE FROM records WHERE vehicle LIKE :recordVehicle")
    void deleteRecordsOfVehicle(String recordVehicle);

    @Update(entity = Record.class)
    void updateAllRecords(Record... records);
}
