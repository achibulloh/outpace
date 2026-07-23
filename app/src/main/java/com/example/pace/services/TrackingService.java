package com.example.pace.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.pace.R;
import com.example.pace.activities.MainActivity;
import com.example.pace.database.AppDatabase;
import com.example.pace.utils.LocaleHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Locale;

public class TrackingService extends Service implements SensorEventListener {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String TRACKING_UPDATE = "TRACKING_UPDATE";

    private static final String CHANNEL_ID = "TrackingChannel";
    private static final int NOTIFICATION_ID = 123;

    private PowerManager.WakeLock wakeLock;
    private FusedLocationProviderClient fusedLocationClient;
    private HandlerThread locationHandlerThread;
    private LocationCallback locationCallback;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor stepDetectorSensor;

    private boolean isTracking = false;
    private boolean isManualPaused = false;
    private boolean isAutoPaused = false;
    private long timeInSeconds = 0L;
    private double totalDistance = 0.0;
    private double totalElevationGain = 0.0;
    
    // Split Tracking
    private int nextSplitKm = 1;
    private long lastSplitTime = 0L;
    private double lastSplitElevation = 0.0;
    private int lastSplitSteps = 0;
    
    private ArrayList<Double> splits = new ArrayList<>();
    private ArrayList<Double> elevationSplits = new ArrayList<>();
    private ArrayList<Integer> cadenceSplits = new ArrayList<>();
    private ArrayList<GeoPoint> fullPathPoints = new ArrayList<>();

    private int startSteps = -1;
    private int currentSteps = 0;
    private Location lastLocation;
    private volatile boolean isLocationUpdatesRunning = false;
    
    // Auto Pause Logic
    private long lowSpeedStartTime = 0;
    private static final float SPEED_THRESHOLD_KMH = 1.0f;
    private static final long AUTO_PAUSE_DELAY_MS = 3000;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTracking && !isAutoPaused && !isManualPaused) {
                timeInSeconds++;
                updateNotification();
                broadcastUpdate();
            }
            if (isTracking) {
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        createNotificationChannel();
        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startTracking();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseTracking();
            } else if (ACTION_STOP.equals(action)) {
                stopTracking();
            }
        }
        return START_STICKY;
    }

    private void startTracking() {
        // Satisfaction for Android 8.0+ requirements: call startForeground immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, getNotification(getString(R.string.notif_tracking_active)), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, getNotification(getString(R.string.notif_tracking_active)));
        }

        if (isTracking && !isAutoPaused && !isManualPaused) return;
        
        if (isManualPaused) {
            isManualPaused = false;
        } else if (isAutoPaused) {
            isAutoPaused = false;
        } else {
            isTracking = true;
            timerHandler.post(timerRunnable);
            registerSensors();

            // Acquire WakeLock to keep CPU running when screen is off
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OUTPACE:TrackingWakeLock");
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            
            // App-internal Notification
            insertAppNotification(getString(R.string.app_notif_start_title), getString(R.string.app_notif_start_body));
        }
        
        startLocationUpdates();
        broadcastUpdate();
        updateNotification();
    }

    private void registerSensors() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void pauseTracking() {
        isManualPaused = true;
        updateNotification();
        broadcastUpdate();
    }

    private void stopTracking() {
        isTracking = false;
        isAutoPaused = false;
        timerHandler.removeCallbacks(timerRunnable);
        stopLocationUpdates();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Release WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        // Show summary notification
        showSummaryNotification();
        
        insertAppNotification(getString(R.string.app_notif_end_title), String.format(getString(R.string.app_notif_end_body), totalDistance));

        stopForeground(true);
        stopSelf();
    }

    private void showSummaryNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification summary = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notif_finished_title))
                .setContentText(String.format(getString(R.string.notif_finished_body), 
                        totalDistance, (timeInSeconds / 60), (timeInSeconds % 60)))
                .setSmallIcon(R.drawable.ic_logo)
                .setAutoCancel(true)
                .build();
        manager.notify(456, summary);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (!isTracking) return;
                
                for (Location location : locationResult.getLocations()) {
                    float speedKmh = location.getSpeed() * 3.6f;
                    handleAutoPauseLogic(speedKmh);

                    if (!isAutoPaused && !isManualPaused) {
                        if (lastLocation != null) {
                            totalDistance += lastLocation.distanceTo(location) / 1000.0;
                            
                            // Elevation Gain Calculation
                            if (location.hasAltitude() && lastLocation.hasAltitude()) {
                                double altDiff = location.getAltitude() - lastLocation.getAltitude();
                                // Threshold 0.2m to filter small GPS jitters
                                if (altDiff > 0.2) {
                                    totalElevationGain += altDiff;
                                }
                            }

                            // Split Tracking
                            if (totalDistance >= nextSplitKm) {
                                long currentSplitTime = timeInSeconds - lastSplitTime;
                                double currentSplitElev = totalElevationGain - lastSplitElevation;
                                int currentSplitSteps = currentSteps - lastSplitSteps;
                                
                                splits.add((double) currentSplitTime);
                                elevationSplits.add(currentSplitElev);
                                
                                // Cadence = steps / minutes
                                double mins = currentSplitTime / 60.0;
                                int cadence = (mins > 0) ? (int)(currentSplitSteps / mins) : 0;
                                
                                // Filter impossible cadence (max 240 spm for humans)
                                if (cadence > 240) cadence = 180;
                                cadenceSplits.add(cadence);

                                lastSplitTime = timeInSeconds;
                                lastSplitElevation = totalElevationGain;
                                lastSplitSteps = currentSteps;
                                nextSplitKm++;
                            }
                        }
                        lastLocation = location;
                        fullPathPoints.add(new GeoPoint(location.getLatitude(), location.getLongitude()));
                        
                        // Fallback steps update every location change
                        if (currentSteps == 0 || (totalDistance * 1350 > currentSteps + 10)) {
                            // Only update if sensor hasn't provided anything meaningful
                            if (startSteps < 0) { 
                                currentSteps = (int) (totalDistance * 1350);
                            }
                        }
                    }
                }
                broadcastUpdate();
            }
        };
    }

    private void handleAutoPauseLogic(float speedKmh) {
        if (speedKmh < SPEED_THRESHOLD_KMH) {
            if (lowSpeedStartTime == 0) {
                lowSpeedStartTime = System.currentTimeMillis();
            } else if (!isAutoPaused && (System.currentTimeMillis() - lowSpeedStartTime > AUTO_PAUSE_DELAY_MS)) {
                isAutoPaused = true;
                updateNotification();
            }
        } else {
            lowSpeedStartTime = 0;
            if (isAutoPaused) {
                isAutoPaused = false;
                updateNotification();
            }
        }
    }

    private void startLocationUpdates() {
        if (isLocationUpdatesRunning) return;

        if (locationHandlerThread == null) {
            locationHandlerThread = new HandlerThread("LocationUpdatesThread");
            locationHandlerThread.start();
        }

        final Looper looper = locationHandlerThread.getLooper();
        new Handler(looper).post(() -> {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(500)
                    .setMaxUpdateDelayMillis(2000)
                    .setWaitForAccurateLocation(false)
                    .build();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, looper)
                        .addOnSuccessListener(aVoid -> isLocationUpdatesRunning = true)
                        .addOnFailureListener(e -> {
                            isLocationUpdatesRunning = false;
                            Log.e("TrackingService", "Failed to request location updates", e);
                        });
            }
        });
    }

    private void stopLocationUpdates() {
        if (!isLocationUpdatesRunning) return;
        
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (locationHandlerThread != null) {
            locationHandlerThread.quitSafely();
            locationHandlerThread = null;
        }
        isLocationUpdatesRunning = false;
    }

    private void broadcastUpdate() {
        Intent intent = new Intent(TRACKING_UPDATE);
        intent.putExtra("isTracking", isTracking);
        intent.putExtra("isAutoPaused", isAutoPaused || isManualPaused);
        intent.putExtra("time", timeInSeconds);
        intent.putExtra("distance", totalDistance);
        intent.putExtra("elevation", totalElevationGain);
        intent.putExtra("steps", currentSteps);

        // Include partial split data in the broadcast for smoother charts
        double partialDistance = totalDistance - (nextSplitKm - 1);
        long partialTime = timeInSeconds - lastSplitTime;
        double partialElev = totalElevationGain - lastSplitElevation;
        int partialSteps = currentSteps - lastSplitSteps;

        int size = splits.size();
        boolean hasPartial = partialDistance > 0.005; // at least 5 meters
        int totalSize = hasPartial ? size + 1 : size;

        double[] splitArray = new double[totalSize];
        double[] elevSplitArray = new double[totalSize];
        int[] cadSplitArray = new int[totalSize];

        for(int i=0; i<size; i++) {
            splitArray[i] = splits.get(i);
            elevSplitArray[i] = elevationSplits.get(i);
            cadSplitArray[i] = cadenceSplits.get(i);
        }

        if (hasPartial) {
            // Predict full km pace/cadence for partial split
            splitArray[size] = (partialTime / Math.max(0.001, partialDistance));
            elevSplitArray[size] = partialElev; // Elevation is usually kept as is or scaled
            
            double mins = partialTime / 60.0;
            int predCadence = (mins > 0.1) ? (int)(partialSteps / mins) : 0;
            if (predCadence > 240) predCadence = 180;
            cadSplitArray[size] = predCadence;
        }

        intent.putExtra("splits", splitArray);
        intent.putExtra("elevSplits", elevSplitArray);
        intent.putExtra("cadSplits", cadSplitArray);
        intent.putParcelableArrayListExtra("fullPath", fullPathPoints);

        if (lastLocation != null) {
            intent.putExtra("lat", lastLocation.getLatitude());
            intent.putExtra("lng", lastLocation.getLongitude());
            intent.putExtra("accuracy", lastLocation.getAccuracy());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null); // Keep it high priority but silent
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("NAVIGATE_TO", "RUN_FRAGMENT");
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Pause/Resume via Service
        Intent pauseIntent = new Intent(this, TrackingService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pPauseIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        // Stop/Save - Redirect to Apps
        Intent stopAppIntent = new Intent(this, MainActivity.class);
        stopAppIntent.putExtra("NAVIGATE_TO", "RUN_FRAGMENT");
        stopAppIntent.putExtra("SHOW_POST_RUN", true);
        stopAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pStopIntent = PendingIntent.getActivity(this, 2, stopAppIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String status = (isAutoPaused || isManualPaused) ? "[" + getString(R.string.notif_tracking_paused).toUpperCase() + "] " : "";
        String stats = String.format(Locale.getDefault(), getString(R.string.notif_stats_format),
                totalDistance, (timeInSeconds / 60), (timeInSeconds % 60), (int)(totalDistance * 65));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(status + getString(R.string.notif_tracking_active))
                .setContentText(stats)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (isTracking && !isAutoPaused && !isManualPaused) {
            builder.addAction(R.drawable.ic_play, getString(R.string.notif_action_pause), pPauseIntent);
        } else {
            Intent startIntent = new Intent(this, TrackingService.class);
            startIntent.setAction(ACTION_START);
            PendingIntent pStartIntent = PendingIntent.getService(this, 3, startIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_play, getString(R.string.notif_action_resume), pStartIntent);
        }
        builder.addAction(R.drawable.ic_logo, getString(R.string.notif_action_stop), pStopIntent);

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, getNotification(null));
    }

    private void insertAppNotification(String title, String body) {
        new Thread(() -> {
            com.example.pace.model.Notification n = new com.example.pace.model.Notification(title, body, System.currentTimeMillis(), false);
            AppDatabase.getInstance(this).notificationDao().insert(n);
        }).start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isTracking || isAutoPaused || isManualPaused) return;

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];
            if (startSteps < 0) startSteps = totalSteps;
            currentSteps = totalSteps - startSteps;
            broadcastUpdate();
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0f) {
                // If counter not initialized or working, detector is a backup
                if (startSteps < 0) {
                    currentSteps++;
                    broadcastUpdate();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
