package com.example.pace.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pace.R;
import com.example.pace.adapter.NotificationAdapter;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.Notification;
import com.example.pace.utils.LocaleHelper;
import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private LinearLayout layoutEmptyNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmptyNotifications = findViewById(R.id.layoutEmptyNotifications);
        
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        new Thread(() -> {
            List<Notification> items = AppDatabase.getInstance(this).notificationDao().getAllNotifications();
            
            final List<Notification> finalItems = items;
            runOnUiThread(() -> {
                notificationList.clear();
                if (finalItems != null && !finalItems.isEmpty()) {
                    notificationList.addAll(finalItems);
                    rvNotifications.setVisibility(View.VISIBLE);
                    layoutEmptyNotifications.setVisibility(View.GONE);
                } else {
                    rvNotifications.setVisibility(View.GONE);
                    layoutEmptyNotifications.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    public void onNotificationClick(Notification notification) {
        new Thread(() -> {
            AppDatabase.getInstance(this).notificationDao().markAsRead(notification.getId());
            notification.setRead(true);
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }).start();
    }
}
