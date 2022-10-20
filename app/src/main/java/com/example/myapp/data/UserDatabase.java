package com.example.myapp.data;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {User.class},
        version = UserDatabase.LATEST_VERSION,
        exportSchema = true,
        autoMigrations = {@AutoMigration(from = UserDatabase.OLD_VERSION, to = UserDatabase.LATEST_VERSION)})
public abstract class UserDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "user_db";
    public static final int LATEST_VERSION = 4;
    public static final int OLD_VERSION = LATEST_VERSION - 1;

    public static UserDatabase instance;

    static UserDatabase getInstance(final Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    UserDatabase.class,
                    DATABASE_NAME
            ).build();
        }
        return instance;
    }

    public abstract UserDao userDao();

}
