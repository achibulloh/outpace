package com.example.pace;

import android.app.Application;
import android.content.Context;
import com.example.pace.utils.LocaleHelper;

public class PaceApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Load configuration in background to avoid blocking main thread during startup
        new Thread(() -> {
            // 1. Load config
            org.osmdroid.config.Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
            
            // 2. Set Professional User-Agent
            String userAgent = "Mozilla/5.0 (Android; OUTPACE-Tracker/1.0)"; 
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(userAgent);
            
            // 3. Set Referer
            org.osmdroid.config.Configuration.getInstance().getAdditionalHttpRequestProperties().put("Referer", "http://www.outpace.app");
        }).start();

        // Force locale
        LocaleHelper.onAttach(this);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.onAttach(this);
    }
}
