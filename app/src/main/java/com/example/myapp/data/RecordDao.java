package com.example.myapp.data;

import androidx.lifecycle.LiveData;
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

    @Query("SELECT * FROM records WHERE vehicle LIKE :recordVehicle")
    List<Record> getRecordsByVehicle(String recordVehicle);

    @Query("SELECT * FROM records ORDER BY date ASC")
    List<Record> getAllRecords();

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
