package com.example.pace.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.model.Notification;

@Database(entities = {RunRecord.class, Notification.class}, version = 6)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract RunDao runDao();
    public abstract NotificationDao notificationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "pace_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
