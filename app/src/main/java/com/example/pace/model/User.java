package com.example.pace.model;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private String dob;
    private String weight;
    private String height;
    private String monthly_target;
    private String goal = "Health & Wellness"; // Weight Loss, Increase Speed, Health & Wellness, Marathon Training
    private String targetWeight = "0";
    private int fitnessLevel = 1; // 1: Beginner, 2: Intermediate, 3: Pro
    private String status = "idle"; // idle, active, running, training
    private double latitude = 0;
    private double longitude = 0;
    private long lastLocationUpdate = 0;

    // Leaderboard stats
    private double totalDistanceToday = 0;
    private double totalDistanceWeek = 0;
    private double totalDistanceMonth = 0;
    private double bestPace = 999; 
    private double bestPaceToday = 999;
    private double bestPaceWeek = 999;
    private double bestPaceMonth = 999;
    private double longestRun = 0;
    private int currentStreak = 0;
    private int streakWeek = 0;
    private int streakMonth = 0;
    private String lastRunDate = ""; // yyyy-MM-dd
    private int lastRunWeek = -1;
    private int lastRunMonth = -1;

    public User() {
        // Required for Firestore
    }

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
    }

    // Getters and Setters for stats
    public double getTotalDistanceToday() { return totalDistanceToday; }
    public void setTotalDistanceToday(double totalDistanceToday) { this.totalDistanceToday = totalDistanceToday; }

    public double getTotalDistanceWeek() { return totalDistanceWeek; }
    public void setTotalDistanceWeek(double totalDistanceWeek) { this.totalDistanceWeek = totalDistanceWeek; }

    public double getTotalDistanceMonth() { return totalDistanceMonth; }
    public void setTotalDistanceMonth(double totalDistanceMonth) { this.totalDistanceMonth = totalDistanceMonth; }

    public double getBestPace() { return bestPace; }
    public void setBestPace(double bestPace) { this.bestPace = bestPace; }

    public double getBestPaceToday() { return bestPaceToday; }
    public void setBestPaceToday(double bestPaceToday) { this.bestPaceToday = bestPaceToday; }

    public double getBestPaceWeek() { return bestPaceWeek; }
    public void setBestPaceWeek(double bestPaceWeek) { this.bestPaceWeek = bestPaceWeek; }

    public double getBestPaceMonth() { return bestPaceMonth; }
    public void setBestPaceMonth(double bestPaceMonth) { this.bestPaceMonth = bestPaceMonth; }

    public double getLongestRun() { return longestRun; }
    public void setLongestRun(double longestRun) { this.longestRun = longestRun; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getStreakWeek() { return streakWeek; }
    public void setStreakWeek(int streakWeek) { this.streakWeek = streakWeek; }

    public int getStreakMonth() { return streakMonth; }
    public void setStreakMonth(int streakMonth) { this.streakMonth = streakMonth; }

    public String getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(String lastRunDate) { this.lastRunDate = lastRunDate; }

    public int getLastRunWeek() { return lastRunWeek; }
    public void setLastRunWeek(int lastRunWeek) { this.lastRunWeek = lastRunWeek; }

    public int getLastRunMonth() { return lastRunMonth; }
    public void setLastRunMonth(int lastRunMonth) { this.lastRunMonth = lastRunMonth; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getMonthlyTarget() { return monthly_target; }
    public void setMonthlyTarget(Object monthlyTarget) { 
        if (monthlyTarget != null) this.monthly_target = String.valueOf(monthlyTarget); 
    }
    
    // Alias for Firestore backward compatibility
    public String getMonthly_target() { return monthly_target; }
    public void setMonthly_target(Object monthly_target) { 
        if (monthly_target != null) this.monthly_target = String.valueOf(monthly_target); 
    }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getTargetWeight() { return targetWeight; }
    public void setTargetWeight(String targetWeight) { this.targetWeight = targetWeight; }

    public int getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(int fitnessLevel) { this.fitnessLevel = fitnessLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(long lastLocationUpdate) { this.lastLocationUpdate = lastLocationUpdate; }
}
