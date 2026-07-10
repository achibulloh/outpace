package com.example.pace.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.pace.model.Notification;
import java.util.List;

@Dao
public interface NotificationDao {
    @Insert
    void insert(Notification notification);

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<Notification> getAllNotifications();

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    int getUnreadCount();

    @Update
    void update(Notification notification);

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    void markAsRead(int id);
}
