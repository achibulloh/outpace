package com.example.pace.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pace.model.RunRecord;

import java.util.List;

@Dao
public interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RunRecord record);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RunRecord> records);

    @Query("SELECT * FROM run_records ORDER BY timestamp DESC")
    List<RunRecord> getAllRuns();

    @Query("SELECT * FROM run_records WHERE id = :id")
    RunRecord getRunById(int id);

    @Query("SELECT MAX(timestamp) FROM run_records")
    long getLatestTimestamp();
}
