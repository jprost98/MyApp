package com.example.myapp.data;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Record.class},
        version = RecordDatabase.LATEST_VERSION,
        exportSchema = true,
        autoMigrations = {@AutoMigration(from = RecordDatabase.OLD_VERSION, to = RecordDatabase.LATEST_VERSION)})
public abstract class RecordDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "records_db";
    public static final int LATEST_VERSION = 5;
    public static final int OLD_VERSION = LATEST_VERSION - 1;

    public static RecordDatabase instance;

    static RecordDatabase getInstance(final Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    RecordDatabase.class,
                    DATABASE_NAME
            ).build();
        }
        return instance;
    }

    public abstract RecordDao recordDao();

}
