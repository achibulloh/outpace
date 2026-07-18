package com.example.pace.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "run_records", indices = {@Index(value = {"firebaseId"}, unique = true)})
public class RunRecord {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private long timestamp;
    private double distance; // in kilometers
    private long duration; // in seconds
    private double pace;
    private int calories;
    private double elevationGain;
    private String pathJson; // Store LatLng list as JSON string
    private String splitsJson; // Store pace splits as JSON
    private String elevationSplitsJson; // Store elevation splits
    private String cadenceSplitsJson; // Store cadence splits
    private String locationName; // Store city/area name
    private String firebaseId; // ID unik untuk sinkronisasi Firebase
    private String date; // Tanggal lari
    private String startTime; // Jam mulai lari
    private String endTime; // Jam selesai lari
    private boolean isSynced; // Status sinkronisasi ke Firebase
    private String mood; // Great, Good, Neutral, Tired
    private int fatigueLevel; // 1-10
    private String aiInsights; // Store Gemini response

    public RunRecord() {
        // Dibutuhkan untuk Firestore
    }

    public RunRecord(long timestamp, double distance, long duration, double pace, int calories, double elevationGain, String pathJson) {
        this.timestamp = timestamp;
        this.distance = distance;
        this.duration = duration;
        this.pace = pace;
        this.calories = calories;
        this.elevationGain = elevationGain;
        this.pathJson = pathJson;
        this.firebaseId = String.valueOf(timestamp);
    }

    public String getSplitsJson() { return splitsJson; }
    public String getElevationSplitsJson() { return elevationSplitsJson; }
    public String getCadenceSplitsJson() { return cadenceSplitsJson; }
    public String getLocationName() { return locationName; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public int getFatigueLevel() { return fatigueLevel; }
    public void setFatigueLevel(int fatigueLevel) { this.fatigueLevel = fatigueLevel; }

    public String getAiInsights() { return aiInsights; }
    public void setAiInsights(String aiInsights) { this.aiInsights = aiInsights; }

    // Setters for new fields
    public void setSplitsJson(String splitsJson) { this.splitsJson = splitsJson; }
    public void setElevationSplitsJson(String elevationSplitsJson) { this.elevationSplitsJson = elevationSplitsJson; }
    public void setCadenceSplitsJson(String cadenceSplitsJson) { this.cadenceSplitsJson = cadenceSplitsJson; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public void setDate(String date) { this.date = date; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public double getPace() { return pace; }
    public void setPace(double pace) { this.pace = pace; }
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    public double getElevationGain() { return elevationGain; }
    public void setElevationGain(double elevationGain) { this.elevationGain = elevationGain; }
    public String getPathJson() { return pathJson; }
    public void setPathJson(String pathJson) { this.pathJson = pathJson; }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }
}
