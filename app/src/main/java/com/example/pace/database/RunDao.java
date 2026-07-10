package com.example.pace.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.pace.model.RunRecord;

import java.util.List;

@Dao
public interface RunDao {
    @Insert
    void insert(RunRecord record);

    @Query("SELECT * FROM run_records ORDER BY timestamp DESC")
    List<RunRecord> getAllRuns();

    @Query("SELECT * FROM run_records WHERE id = :id")
    RunRecord getRunById(int id);
}
