package com.example.pace.model;

public class LeaderboardUser {
    private String rank;
    private String name;
    private String pace;
    private String distance;
    private int iconResId;
    private boolean isYou;

    public LeaderboardUser(String rank, String name, String pace, String distance, int iconResId, boolean isYou) {
        this.rank = rank;
        this.name = name;
        this.pace = pace;
        this.distance = distance;
        this.iconResId = iconResId;
        this.isYou = isYou;
    }

    public String getRank() { return rank; }
    public String getName() { return name; }
    public String getPace() { return pace; }
    public String getDistance() { return distance; }
    public int getIconResId() { return iconResId; }
    public boolean isYou() { return isYou; }
}
