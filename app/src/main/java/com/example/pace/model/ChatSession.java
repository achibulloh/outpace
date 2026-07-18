package com.example.pace.model;

import com.google.firebase.firestore.Exclude;

public class ChatSession {
    private String id;
    private String title;
    private long lastTimestamp;

    public ChatSession() {}

    public ChatSession(String id, String title, long lastTimestamp) {
        this.id = id;
        this.title = title;
        this.lastTimestamp = lastTimestamp;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
}
