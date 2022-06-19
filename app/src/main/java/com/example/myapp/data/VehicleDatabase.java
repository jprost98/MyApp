package com.example.myapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Vehicle.class}, version = 1)
public abstract class VehicleDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "vehicles_db";

    public static VehicleDatabase instance;

    static VehicleDatabase getInstance(final Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    VehicleDatabase.class,
                    DATABASE_NAME
            ).build();
        }
        return instance;
    }

    public abstract VehicleDao vehicleDao();

}
