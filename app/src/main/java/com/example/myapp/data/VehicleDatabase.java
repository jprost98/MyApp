package com.example.myapp.data;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Vehicle.class},
        version = VehicleDatabase.LATEST_VERSION,
        exportSchema = true,
        autoMigrations = {@AutoMigration(from = VehicleDatabase.OLD_VERSION, to = VehicleDatabase.LATEST_VERSION)})
public abstract class VehicleDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "vehicles_db";
    public static final int LATEST_VERSION = 5;
    public static final int OLD_VERSION = LATEST_VERSION - 1;

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