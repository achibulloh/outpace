package com.example.pace.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "run_records")
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

    public RunRecord(long timestamp, double distance, long duration, double pace, int calories, double elevationGain, String pathJson) {
        this.timestamp = timestamp;
        this.distance = distance;
        this.duration = duration;
        this.pace = pace;
        this.calories = calories;
        this.elevationGain = elevationGain;
        this.pathJson = pathJson;
    }

    // Setters for new fields
    public void setSplitsJson(String splitsJson) { this.splitsJson = splitsJson; }
    public void setElevationSplitsJson(String elevationSplitsJson) { this.elevationSplitsJson = elevationSplitsJson; }
    public void setCadenceSplitsJson(String cadenceSplitsJson) { this.cadenceSplitsJson = cadenceSplitsJson; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    // Getters for new fields
    public String getSplitsJson() { return splitsJson; }
    public String getElevationSplitsJson() { return elevationSplitsJson; }
    public String getCadenceSplitsJson() { return cadenceSplitsJson; }
    public String getLocationName() { return locationName; }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public double getDistance() { return distance; }
    public long getDuration() { return duration; }
    public double getPace() { return pace; }
    public int getCalories() { return calories; }
    public double getElevationGain() { return elevationGain; }
    public String getPathJson() { return pathJson; }
}
