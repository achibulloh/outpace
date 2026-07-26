package com.example.pace.model;

public class HelpTicket {
    private String id;
    private String userId;
    private String userEmail;
    private String category;
    private String description;
    private String status; // Open, Pending, Resolved
    private String priority; // Low, Medium, High
    private long timestamp;
    private String adminReply;
    private long replyTimestamp;
    private java.util.List<TicketReply> replies;

    public HelpTicket() {
        // Required for Firestore
    }

    public HelpTicket(String id, String userId, String userEmail, String category, String description, String status, String priority, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.category = category;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

    public long getReplyTimestamp() { return replyTimestamp; }
    public void setReplyTimestamp(long replyTimestamp) { this.replyTimestamp = replyTimestamp; }

    public java.util.List<TicketReply> getReplies() { return replies; }
    public void setReplies(java.util.List<TicketReply> replies) { this.replies = replies; }
}
