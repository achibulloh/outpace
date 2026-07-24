package com.example.pace.model;

import com.google.firebase.Timestamp;

public class GeminiKey {
    private String id; // Document ID
    private String key;
    private String status; // active, limit, blocked
    private String owner;
    private long usage_count;
    private Timestamp last_used;
    private Timestamp quota_reset_at;
    private String error_message;
    private Timestamp created_at;

    public GeminiKey() {
        // Required for Firestore
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public long getUsage_count() { return usage_count; }
    public void setUsage_count(long usage_count) { this.usage_count = usage_count; }

    public Timestamp getLast_used() { return last_used; }
    public void setLast_used(Timestamp last_used) { this.last_used = last_used; }

    public Timestamp getQuota_reset_at() { return quota_reset_at; }
    public void setQuota_reset_at(Timestamp quota_reset_at) { this.quota_reset_at = quota_reset_at; }

    public String getError_message() { return error_message; }
    public void setError_message(String error_message) { this.error_message = error_message; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }
}
