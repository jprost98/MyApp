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
    List<Record> getRecord(String recordVehicle);

    @Query("SELECT * FROM records ORDER BY date")
    List<Record> getAllRecords();

    @Delete
    int deleteRecord(Record... record);

    @Update
    int updateRecord(Record... record);

    @Query("DELETE FROM records")
    void deleteAllRecords();

    @Update(entity = Record.class)
    void updateAllRecords(Record... records);
}
