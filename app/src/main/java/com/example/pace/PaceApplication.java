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
            
            // 2. Set User-Agent lebih "Legit" agar server tidak throttling (Mencegah 403 & Slowness)
            String userAgent = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"; 
            org.osmdroid.config.Configuration.getInstance().setUserAgentValue(userAgent);
            
            // 3. Performance Optimizations (High Speed with Animation)
            org.osmdroid.config.Configuration.getInstance().setTileDownloadThreads((short) 10); 
            org.osmdroid.config.Configuration.getInstance().setCacheMapTileCount((short) 24);
            org.osmdroid.config.Configuration.getInstance().setTileFileSystemCacheMaxBytes(500L * 1024 * 1024); // 500MB
            org.osmdroid.config.Configuration.getInstance().setTileFileSystemCacheTrimBytes(450L * 1024 * 1024);
            org.osmdroid.config.Configuration.getInstance().setExpirationExtendedDuration(14 * 24 * 60 * 60 * 1000L); // 14 Hari
            
            // 4. Cache Location
            java.io.File cacheDir = new java.io.File(getCacheDir(), "osmdroid");
            org.osmdroid.config.Configuration.getInstance().setOsmdroidBasePath(cacheDir);
            org.osmdroid.config.Configuration.getInstance().setOsmdroidTileCache(new java.io.File(cacheDir, "tiles"));
            
            // 5. Set Referer
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
