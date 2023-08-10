package com.example.myapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {

    @Insert
    void addUser(User... users);

    @Query("DELETE FROM users")
    void deleteUser();

    @Update
    int updateUser(User... users);

    @Query("SELECT * FROM users")
    List<User> getUser();
}
