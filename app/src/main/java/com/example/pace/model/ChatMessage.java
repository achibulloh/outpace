package com.example.pace.model;

import android.graphics.Bitmap;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private Bitmap image;
    private long timestamp;

    public ChatMessage() {} // For Firestore

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String text, boolean isUser, Bitmap image) {
        this.text = text;
        this.isUser = isUser;
        this.image = image;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public Bitmap getImage() { return image; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
