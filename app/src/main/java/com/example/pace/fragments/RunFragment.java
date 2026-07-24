package com.example.pace.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.pace.R;
import com.example.pace.activities.MainActivity;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.services.TrackingService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.pace.model.User;
import com.example.pace.views.BarChartView;

public class RunFragment extends Fragment implements android.hardware.SensorEventListener {

    private MapView map = null;
    private Polyline polyline;
    private List<GeoPoint> pathPoints = new ArrayList<>();
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private android.hardware.SensorManager sensorManager;
    private android.hardware.Sensor rotationSensor;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 2;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 3;

    private Button btnStart, btnPause, btnStop;
    private Button btnPauseOverlay, btnStopOverlay;
    private ImageButton btnMinimize, btnLayerToggle, btnMyLocation, btnCompass;
    private LinearLayout btnBack, layoutMapLoading, panelStatsBottom, layoutStatsOverlay;
    private TextView tvDuration, tvDistance, tvPace, tvCalories, tvSteps;
    private View viewGpsIndicator;
    private TextView tvGpsStatus;

    // Overlay Views
    private TextView tvOverlayGpsStatus, tvOverlayDuration, tvOverlayAvgPace, tvOverlayDistance, tvOverlayCalories, tvOverlaySteps, tvOverlayElevation;
    private BarChartView splitBarChart;

    private boolean isTracking = false;
    private boolean isAutoPaused = false;
    private long lastTime = 0;
    private double lastDistance = 0;
    private double lastElevation = 0;
    private double[] currentSplits = new double[0];
    private double[] currentElevSplits = new double[0];
    private int[] currentCadSplits = new int[0];
    private int userWeight = 70;

    private boolean isMapReady = false;
    private boolean isLocationReady = false;
    private boolean isStatsExpanded = false;

    private LocationCallback statusLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            android.location.Location loc = locationResult.getLastLocation();
            if (loc != null) updateGpsStatus(loc.getAccuracy());
        }
    };
    private GestureDetector gestureDetector;

    private boolean isSaving = false;

    private BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TrackingService.TRACKING_UPDATE.equals(intent.getAction())) {
                updateUIFromService(intent);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context ctx = requireActivity().getApplicationContext();
        
        // 1. Initialize configuration
        Configuration.getInstance().load(ctx, requireActivity().getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        
        // 2. Set specific User-Agent to prevent 403 Access Blocked
        String userAgent = "OutpaceTracker/1.0 (" + ctx.getPackageName() + "; contact@outpace.app)";
        Configuration.getInstance().setUserAgentValue(userAgent);

        View view = inflater.inflate(R.layout.fragment_run, container, false);
        
        sensorManager = (android.hardware.SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ORIENTATION);

        loadUserWeight();
        initUI(view);
        setupMap(view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity().getApplicationContext());
        enableInitialLocation();
        checkActivityRecognitionPermission();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Handle Auto-Open Post Run Dialog from Notification
        if (getArguments() != null && getArguments().getBoolean("SHOW_POST_RUN", false)) {
            // We need to wait a bit until Fragment is fully interactive
            view.postDelayed(this::showPostRunDialog, 600);
        }
    }

    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.activity_recognition_title)
                        .setMessage(R.string.activity_recognition_desc)
                        .setPositiveButton(R.string.grant_permission, (dialog, which) -> {
                            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
                        })
                        .setNegativeButton(R.string.later, null)
                        .show();
            }
        }
    }

    private void loadUserWeight() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userWeight = prefs.getInt("weight", 70);
    }

    private void initUI(View view) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(false);
        }

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnStop = view.findViewById(R.id.btnStop);
        btnBack = view.findViewById(R.id.btnBack);
        btnLayerToggle = view.findViewById(R.id.btnLayerToggle);
        btnMyLocation = view.findViewById(R.id.btnMyLocation);
        btnCompass = view.findViewById(R.id.btnCompass);
        btnMinimize = view.findViewById(R.id.btnMinimize);
        layoutMapLoading = view.findViewById(R.id.layoutMapLoading);
        panelStatsBottom = view.findViewById(R.id.panelStatsBottom);
        layoutStatsOverlay = view.findViewById(R.id.layoutStatsOverlay);
        
        btnPauseOverlay = view.findViewById(R.id.btnPauseOverlay);
        btnStopOverlay = view.findViewById(R.id.btnStopOverlay);

        viewGpsIndicator = view.findViewById(R.id.viewGpsIndicator);
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus);

        tvDuration = view.findViewById(R.id.tvDuration);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvPace = view.findViewById(R.id.tvPace);
        tvCalories = view.findViewById(R.id.tvCalories);
        tvSteps = view.findViewById(R.id.tvSteps);

        tvOverlayGpsStatus = view.findViewById(R.id.tvOverlayGpsStatus);
        tvOverlayDuration = view.findViewById(R.id.tvOverlayDuration);
        tvOverlayAvgPace = view.findViewById(R.id.tvOverlayAvgPace);
        tvOverlayDistance = view.findViewById(R.id.tvOverlayDistance);
        tvOverlayCalories = view.findViewById(R.id.tvOverlayCalories);
        tvOverlaySteps = view.findViewById(R.id.tvOverlaySteps);
        tvOverlayElevation = view.findViewById(R.id.tvOverlayElevation);
        splitBarChart = view.findViewById(R.id.splitBarChart);
        if (splitBarChart != null) splitBarChart.setShowLabels(false);

        btnPauseOverlay.setVisibility(View.GONE);
        btnStopOverlay.setVisibility(View.GONE);

        btnStart.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    showBackgroundLocationDialog();
                    return;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                    return;
                }
            }
            startRunning();
        });
        
        View.OnClickListener pauseAction = v -> {
            if (isTracking && !isAutoPaused) sendAction(TrackingService.ACTION_PAUSE);
            else sendAction(TrackingService.ACTION_START);
        };
        btnPause.setOnClickListener(pauseAction);
        btnPauseOverlay.setOnClickListener(pauseAction);

        View.OnClickListener stopAction = v -> {
            if (!isSaving) {
                if (lastDistance >= 0.005) {
                    showPostRunDialog();
                } else {
                    isSaving = true;
                    saveRunBeforeStop(null, 0, null);
                    sendAction(TrackingService.ACTION_STOP);
                }
            }
        };
        btnStop.setOnClickListener(stopAction);
        btnStopOverlay.setOnClickListener(stopAction);

        btnBack.setOnClickListener(v -> {
            if (isTracking) {
                Toast.makeText(getContext(), R.string.stop_tracking_first, Toast.LENGTH_SHORT).show();
            } else {
                goBack();
            }
        });

        btnLayerToggle.setOnClickListener(v -> showLayerSelectionDialog());
        btnMyLocation.setOnClickListener(v -> {
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                map.getController().animateTo(myLocationOverlay.getMyLocation());
                map.getController().setZoom(18.0);
            } else {
                Toast.makeText(requireContext(), R.string.searching_gps, Toast.LENGTH_SHORT).show();
            }
        });
        btnCompass.setOnClickListener(v -> {
            map.setMapOrientation(0);
            Toast.makeText(requireContext(), R.string.map_facing_north, Toast.LENGTH_SHORT).show();
        });
        if (btnMinimize != null) btnMinimize.setOnClickListener(v -> toggleStatsOverlay());
        panelStatsBottom.setOnClickListener(v -> toggleStatsOverlay());
        
        setupSwipeGestures();
    }

    private void setupSwipeGestures() {
        gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    if (e1.getY() - e2.getY() > 100) { if (!isStatsExpanded) toggleStatsOverlay(); return true; }
                    else if (e2.getY() - e1.getY() > 100) { if (isStatsExpanded) toggleStatsOverlay(); return true; }
                }
                return false;
            }
        });
        View.OnTouchListener touchListener = (v, event) -> { gestureDetector.onTouchEvent(event); return false; };
        panelStatsBottom.setOnTouchListener(touchListener);
        layoutStatsOverlay.setOnTouchListener(touchListener);
    }

    private void toggleStatsOverlay() {
        isStatsExpanded = !isStatsExpanded;
        if (isStatsExpanded) {
            layoutStatsOverlay.setVisibility(View.VISIBLE);
            panelStatsBottom.setVisibility(View.GONE);
            // Hide Map Control Buttons when expanded
            btnLayerToggle.setVisibility(View.GONE);
            btnMyLocation.setVisibility(View.GONE);
            btnCompass.setVisibility(View.GONE);
        } else {
            layoutStatsOverlay.setVisibility(View.GONE);
            panelStatsBottom.setVisibility(View.VISIBLE);
            // Show Map Control Buttons when minimized
            btnLayerToggle.setVisibility(View.VISIBLE);
            btnMyLocation.setVisibility(View.VISIBLE);
            btnCompass.setVisibility(View.VISIBLE);
        }
    }

    private void setupMap(View view) {
        map = view.findViewById(R.id.map);
        
        // Use OSM HOT instead of MAPNIK to prevent 403
        map.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource("OSMHot", 0, 19, 256, ".png", 
                new String[] {
                    "https://a.tile.openstreetmap.fr/hot/",
                    "https://b.tile.openstreetmap.fr/hot/",
                    "https://c.tile.openstreetmap.fr/hot/" 
                }));
        
        map.setMultiTouchControls(true);
        map.setBackgroundColor(Color.parseColor("#121212"));
        
        // Apply Dark Mode Filter to the map tiles using negative color matrix
        float[] negative = {
                -1.0f, 0, 0, 0, 255, // red
                0, -1.0f, 0, 0, 255, // green
                0, 0, -1.0f, 0, 255, // blue
                0, 0, 0, 1.0f, 0     // alpha
        };
        map.getOverlayManager().getTilesOverlay().setColorFilter(new android.graphics.ColorMatrixColorFilter(negative));

        map.getController().setZoom(18.0);
        
        polyline = new Polyline();
        polyline.getOutlinePaint().setColor(Color.parseColor("#C8F43A")); // Warna Lime sesuai aplikasi
        polyline.getOutlinePaint().setStrokeWidth(14f);
        map.getOverlayManager().add(polyline);

        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(map);
        rotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(rotationGestureOverlay);

        // My Location Overlay
        GpsMyLocationProvider provider = new GpsMyLocationProvider(requireContext());
        provider.setLocationUpdateMinTime(3000); // 3 seconds
        provider.setLocationUpdateMinDistance(2); // 2 meters
        myLocationOverlay = new MyLocationNewOverlay(provider, map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);

        // Compass Overlay
        compassOverlay = new CompassOverlay(requireContext(), new InternalCompassOrientationProvider(requireContext()), map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        map.addOnFirstLayoutListener((v, left, top, right, bottom) -> {
            isMapReady = true;
            checkLoadingFinished();
        });
    }

    private void enableInitialLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null && !isTracking) {
                        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                        map.getController().setCenter(point);
                        updateGpsStatus(location.getAccuracy());
                    }
                    isLocationReady = true;
                    checkLoadingFinished();
                })
                .addOnFailureListener(e -> { isLocationReady = true; checkLoadingFinished(); });
    }

    private void startStatusLocationUpdates() {
        if (isTracking) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();
        fusedLocationClient.requestLocationUpdates(request, statusLocationCallback, android.os.Looper.getMainLooper());
    }

    private void stopStatusLocationUpdates() {
        if (fusedLocationClient != null && statusLocationCallback != null) fusedLocationClient.removeLocationUpdates(statusLocationCallback);
    }

    private void checkLoadingFinished() {
        if (isMapReady && isLocationReady && layoutMapLoading != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (layoutMapLoading != null) {
                    layoutMapLoading.animate().alpha(0).setDuration(500).withEndAction(() -> layoutMapLoading.setVisibility(View.GONE)).start();
                }
            }, 500);
        }
    }

    private void updateGpsStatus(float accuracy) {
        String statusText = (accuracy > 0 && accuracy < 20) ? getString(R.string.gps_good) : getString(R.string.gps_signal_weak);
        int resId = (accuracy > 0 && accuracy < 20) ? R.drawable.progress_fill : R.drawable.dot_red;
        int color = (accuracy > 0 && accuracy < 20) ? Color.parseColor("#C8F43A") : Color.RED;

        if (tvGpsStatus != null) { tvGpsStatus.setText(statusText); viewGpsIndicator.setBackgroundResource(resId); }
        if (tvOverlayGpsStatus != null) {
            String text = isAutoPaused ? getString(R.string.auto_paused) : getString(R.string.gps_signal_format, statusText.split(" ")[statusText.split(" ").length - 1].toLowerCase());
            tvOverlayGpsStatus.setText(text);
            tvOverlayGpsStatus.setTextColor(isAutoPaused ? Color.YELLOW : color);
        }
    }

    private void showLayerSelectionDialog() {
        if (getContext() == null) return;
        
        String[] layers = {
                getString(R.string.map_layer_standard),
                getString(R.string.map_layer_cyclosm),
                getString(R.string.map_layer_satellite)
        };
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.map_layers_title);
        builder.setItems(layers, (dialog, which) -> {
            if (map == null) return;
            
            switch (which) {
                case 0:
                    // Use OSM HOT instead of MAPNIK to prevent 403
                    map.setTileSource(new org.osmdroid.tileprovider.tilesource.XYTileSource("OSMHot", 0, 19, 256, ".png", 
                        new String[] {
                            "https://a.tile.openstreetmap.fr/hot/",
                            "https://b.tile.openstreetmap.fr/hot/",
                            "https://c.tile.openstreetmap.fr/hot/" 
                        }));
                    if (map.getOverlayManager().getTilesOverlay() != null) {
                        map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                    }
                    break;
                case 1:
                    XYTileSource cyclOSM = new XYTileSource("CyclOSM", 0, 20, 256, ".png",
                            new String[]{"https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
                                    "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
                                    "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"}, "© CyclOSM contributors");
                    map.setTileSource(cyclOSM);
                    if (map.getOverlayManager().getTilesOverlay() != null) {
                        map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                    }
                    break;
                case 2:
                    XYTileSource googleSatellite = new XYTileSource("GoogleSatellite", 0, 20, 256, ".png",
                            new String[]{"https://mt0.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
                                    "https://mt1.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
                                    "https://mt2.google.com/vt/lyrs=s&x={x}&y={y}&z={z}",
                                    "https://mt3.google.com/vt/lyrs=s&x={x}&y={y}&z={z}"});
                    map.setTileSource(googleSatellite);
                    if (map.getOverlayManager().getTilesOverlay() != null) {
                        map.getOverlayManager().getTilesOverlay().setColorFilter(null);
                    }
                    break;
            }
            map.invalidate(); 
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUIFromService(Intent intent) {
        if (isSaving) return; // Ignore updates while saving/closing
        
        isTracking = intent.getBooleanExtra("isTracking", false);
        isAutoPaused = intent.getBooleanExtra("isAutoPaused", false);

        // Pastikan overlay lokasi tetap aktif
        if (myLocationOverlay != null && !myLocationOverlay.isMyLocationEnabled()) {
            myLocationOverlay.enableMyLocation();
        }

        long time = intent.getLongExtra("time", 0);
        double distance = intent.getDoubleExtra("distance", 0.0);
        double elevation = intent.getDoubleExtra("elevation", 0.0);
        int steps = intent.getIntExtra("steps", 0);
        double lat = intent.getDoubleExtra("lat", 0);
        double lng = intent.getDoubleExtra("lng", 0);
        float accuracy = intent.getFloatExtra("accuracy", 0);
        
        ArrayList<GeoPoint> fullPath = intent.getParcelableArrayListExtra("fullPath");
        if (fullPath != null && !fullPath.isEmpty()) {
            pathPoints = fullPath;
            polyline.setPoints(pathPoints);
            updateWaypoints(pathPoints);
        }

        lastTime = time; lastDistance = distance; lastElevation = elevation;
        updateGpsStatus(accuracy);

        double[] tempSplits = intent.getDoubleArrayExtra("splits");
        if (tempSplits != null) currentSplits = tempSplits;
        double[] tempElev = intent.getDoubleArrayExtra("elevSplits");
        if (tempElev != null) currentElevSplits = tempElev;
        int[] tempCad = intent.getIntArrayExtra("cadSplits");
        if (tempCad != null) currentCadSplits = tempCad;

        tvDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", time / 60, time % 60));
        tvOverlayDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", time / 3600, (time % 3600) / 60, time % 60));
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", distance));
        tvOverlayDistance.setText(String.format(Locale.getDefault(), "%.1f", distance));
        
        String cal = String.valueOf((int)(distance * userWeight * 1.036));
        if (tvCalories != null) tvCalories.setText(cal);
        if (tvOverlayCalories != null) tvOverlayCalories.setText(cal);

        String s = (steps > 0) ? String.valueOf(steps) : "--";
        if (tvSteps != null) tvSteps.setText(s);
        if (tvOverlaySteps != null) tvOverlaySteps.setText(s);
        if (tvOverlayElevation != null) tvOverlayElevation.setText(String.format(Locale.getDefault(), "%.0fm Up", elevation));

        if (distance > 0.001) {
            double p = (time / 60.0) / distance;
            String ps = String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p - (int)p) * 60));
            tvPace.setText(ps); tvOverlayAvgPace.setText(ps);
        }

        updateButtons(isTracking && !isAutoPaused);

        if (lat != 0 && lng != 0) {
            GeoPoint point = new GeoPoint(lat, lng);
            if (!isAutoPaused) map.getController().animateTo(point);
            map.invalidate();
        }

        double[] splits = intent.getDoubleArrayExtra("splits");
        if (splits != null && splits.length > 0 && splitBarChart != null) {
            float[] sp = new float[splits.length];
            for(int i=0; i<splits.length; i++) sp[i] = (float) (splits[i] / 60.0);
            splitBarChart.setData(sp);
        }
    }

    private void updateWaypoints(List<GeoPoint> points) {
        if (points == null || points.isEmpty()) return;
        // No markers added here as per user request to only show them in DetailActivity
        map.invalidate();
    }


    private void updateButtons(boolean tracking) {
        if (isTracking) {
            btnStart.setVisibility(View.GONE); btnPause.setVisibility(View.VISIBLE); btnPauseOverlay.setVisibility(View.VISIBLE);
            if (isAutoPaused) { 
                btnPause.setText(R.string.btn_resume); btnPauseOverlay.setText(R.string.btn_resume); 
                btnStop.setVisibility(View.GONE); btnStopOverlay.setVisibility(View.GONE); 
            }
            else { 
                btnPause.setText(R.string.btn_pause); btnPauseOverlay.setText(R.string.btn_pause); 
                btnStop.setVisibility(View.VISIBLE); btnStopOverlay.setVisibility(View.VISIBLE); 
            }
        } else if (lastTime > 0) {
            btnStart.setVisibility(View.GONE); btnPause.setVisibility(View.VISIBLE); btnPauseOverlay.setVisibility(View.VISIBLE);
            btnPause.setText(R.string.btn_resume); btnPauseOverlay.setText(R.string.btn_resume); 
            btnStop.setVisibility(View.GONE); btnStopOverlay.setVisibility(View.GONE);
        } else {
            btnStart.setVisibility(View.VISIBLE); btnPause.setVisibility(View.GONE); btnPauseOverlay.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE); btnStopOverlay.setVisibility(View.GONE);
        }
    }

    private void startRunning() {
        stopStatusLocationUpdates();
        updateUserStatus("running");
        sendAction(TrackingService.ACTION_START);
        if (!isStatsExpanded) toggleStatsOverlay();
    }

    private void updateUserStatus(String status) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .update("status", status);
        }
    }

    private void showBackgroundLocationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.bg_location_title)
                .setMessage(R.string.bg_location_desc)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 999);
                })
                .setNegativeButton(R.string.later, (dialog, which) -> startRunning()).show();
    }

    private String selectedMood = "Neutral";
    private void showPostRunDialog() {
        android.view.View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_run, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).setView(dialogView).create();
        
        TextView btnGreat = dialogView.findViewById(R.id.btnMoodGreat);
        TextView btnGood = dialogView.findViewById(R.id.btnMoodGood);
        TextView btnNeutral = dialogView.findViewById(R.id.btnMoodNeutral);
        TextView btnTired = dialogView.findViewById(R.id.btnMoodTired);
        android.widget.SeekBar sbFatigue = dialogView.findViewById(R.id.sbFatigue);
        android.widget.Button btnSubmit = dialogView.findViewById(R.id.btnSubmitPostRun);

        selectedMood = ""; // Reset mood agar wajib dipilih
        View.OnClickListener moodClick = v -> {
            btnGreat.setBackgroundResource(R.drawable.tab_unselected);
            btnGood.setBackgroundResource(R.drawable.tab_unselected);
            btnNeutral.setBackgroundResource(R.drawable.tab_unselected);
            btnTired.setBackgroundResource(R.drawable.tab_unselected);
            
            v.setBackgroundResource(R.drawable.btn_outline_lime);
            if (v.getId() == R.id.btnMoodGreat) selectedMood = "Great";
            else if (v.getId() == R.id.btnMoodGood) selectedMood = "Good";
            else if (v.getId() == R.id.btnMoodNeutral) selectedMood = "Neutral";
            else if (v.getId() == R.id.btnMoodTired) selectedMood = "Tired";
        };

        btnGreat.setOnClickListener(moodClick);
        btnGood.setOnClickListener(moodClick);
        btnNeutral.setOnClickListener(moodClick);
        btnTired.setOnClickListener(moodClick);

        btnSubmit.setOnClickListener(v -> {
            if (selectedMood.isEmpty()) {
                Toast.makeText(requireContext(), "Silakan pilih mood lari Anda!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Prevent double click
            btnSubmit.setEnabled(false);

            // Show Loading to prevent ANR feeling
            android.app.ProgressDialog pd = new android.app.ProgressDialog(requireContext());
            pd.setMessage("Saving your activity...");
            pd.setCancelable(false);
            pd.show();

            isSaving = true;
            saveRunBeforeStop(selectedMood, sbFatigue.getProgress() + 1, pd);
            sendAction(TrackingService.ACTION_STOP);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveRunBeforeStop(String mood, int fatigue, android.app.ProgressDialog pd) {
        if (lastDistance < 0.005) { 
            isSaving = false;
            updateUserStatus("active");
            sendAction(TrackingService.ACTION_STOP);
            if (pd != null) pd.dismiss();
            goBack(); 
            return; 
        }
        
        btnStop.setEnabled(false);
        if (btnStopOverlay != null) btnStopOverlay.setEnabled(false);

        // Capture data safely
        final long ts = System.currentTimeMillis(); 
        final double d = lastDistance; 
        final long dur = lastTime;
        final double p = (d > 0) ? (dur / 60.0) / d : 0; 
        final int c = (int) (d * userWeight * 1.036);
        final double e = lastElevation; 
        final ArrayList<GeoPoint> pathCopy = new ArrayList<>(pathPoints);
        final double[] sCopy = currentSplits != null ? currentSplits.clone() : new double[0];
        final double[] eCopy = currentElevSplits != null ? currentElevSplits.clone() : new double[0];
        final int[] cCopy = currentCadSplits != null ? currentCadSplits.clone() : new int[0];

        // Context check
        final Context context = getContext() != null ? getContext().getApplicationContext() : null;
        if (context == null) {
            if (pd != null) pd.dismiss();
            return;
        }

        new Thread(() -> {
            try {
                // Heavy work in background
                String pj = new Gson().toJson(pathCopy);
                String sJson = new Gson().toJson(sCopy);
                String eJson = new Gson().toJson(eCopy);
                String cJson = new Gson().toJson(cCopy);

                // Geocoding
                String ln = "Unknown Location";
                if (!pathCopy.isEmpty()) {
                    try {
                        Geocoder geo = new Geocoder(context, Locale.getDefault());
                        List<Address> ads = geo.getFromLocation(pathCopy.get(0).getLatitude(), pathCopy.get(0).getLongitude(), 1);
                        if (ads != null && !ads.isEmpty()) {
                            Address a = ads.get(0);
                            String city = a.getSubAdminArea() != null ? a.getSubAdminArea() : a.getLocality();
                            String area = a.getSubLocality() != null ? a.getSubLocality() : a.getLocality();
                            if (city != null && area != null && !city.equals(area)) ln = city + " - " + area;
                            else if (city != null) ln = city; else if (area != null) ln = area;
                        }
                    } catch (Exception ignored) {}
                }

                // Date Formatting
                long startTs = ts - (dur * 1000);
                SimpleDateFormat sdfDate = new SimpleDateFormat("EEEE, dd MMM yyyy", new Locale("id", "ID"));
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
                String dateStr = sdfDate.format(new Date(startTs));
                String startTimeStr = sdfTime.format(new Date(startTs));
                String endTimeStr = sdfTime.format(new Date(ts));

                RunRecord r = new RunRecord(ts, d, dur, p, c, e, pj);
                r.setLocationName(ln);
                r.setSplitsJson(sJson);
                r.setElevationSplitsJson(eJson);
                r.setCadenceSplitsJson(cJson);
                r.setDate(dateStr);
                r.setStartTime(startTimeStr);
                r.setEndTime(endTimeStr);
                r.setMood(mood);
                r.setFatigueLevel(fatigue);
                r.setSynced(false);

                // Save to Room
                AppDatabase.getInstance(context).runDao().insert(r);
                
                // Sync to Firebase
                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
                if (fUser != null) {
                    FirebaseFirestore.getInstance().collection("users").document(fUser.getUid())
                            .collection("runs").document(r.getFirebaseId())
                            .set(r)
                            .addOnSuccessListener(aVoid -> {
                                new Thread(() -> {
                                    r.setSynced(true);
                                    AppDatabase.getInstance(context).runDao().insert(r);
                                }).start();
                                updateUserLeaderboardStats(r);
                            });
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pd != null && pd.isShowing()) pd.dismiss();
                        Toast.makeText(context, R.string.activity_saved, Toast.LENGTH_SHORT).show();
                        isSaving = false;
                        goBack();
                    });
                }
            } catch (Exception ex) {
                Log.e("RunFragment", "Error saving run", ex);
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (pd != null && pd.isShowing()) pd.dismiss();
                    Toast.makeText(context, "Error saving activity", Toast.LENGTH_SHORT).show();
                    isSaving = false;
                });
            }
        }).start();
    }

    private void updateUserLeaderboardStats(RunRecord run) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get().addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u == null) return;

                    Calendar cal = Calendar.getInstance();
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                    int thisWeek = cal.get(Calendar.WEEK_OF_YEAR);
                    int thisMonth = cal.get(Calendar.MONTH);

                    // Reset logic
                    if (!today.equals(u.getLastRunDate())) {
                        u.setTotalDistanceToday(0);
                        u.setBestPaceToday(999);
                    }
                    if (thisWeek != u.getLastRunWeek()) {
                        u.setTotalDistanceWeek(0);
                        u.setBestPaceWeek(999);
                        u.setStreakWeek(0);
                    }
                    if (thisMonth != u.getLastRunMonth()) {
                        u.setTotalDistanceMonth(0);
                        u.setBestPaceMonth(999);
                        u.setStreakMonth(0);
                    }

                    // Update Distances
                    u.setTotalDistanceToday(u.getTotalDistanceToday() + run.getDistance());
                    u.setTotalDistanceWeek(u.getTotalDistanceWeek() + run.getDistance());
                    u.setTotalDistanceMonth(u.getTotalDistanceMonth() + run.getDistance());

                    // Update Best Pace (only if distance >= 3km)
                    if (run.getDistance() >= 3.0) {
                        double p = run.getPace();
                        if (p < u.getBestPace() || u.getBestPace() == 999) u.setBestPace(p);
                        if (p < u.getBestPaceToday() || u.getBestPaceToday() == 999) u.setBestPaceToday(p);
                        if (p < u.getBestPaceWeek() || u.getBestPaceWeek() == 999) u.setBestPaceWeek(p);
                        if (p < u.getBestPaceMonth() || u.getBestPaceMonth() == 999) u.setBestPaceMonth(p);
                    }

                    // Update Longest Run
                    if (run.getDistance() > u.getLongestRun()) {
                        u.setLongestRun(run.getDistance());
                    }

                    // Update Streak
                    cal.add(Calendar.DATE, -1);
                    String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                    if (yesterday.equals(u.getLastRunDate())) {
                        u.setCurrentStreak(u.getCurrentStreak() + 1);
                        u.setStreakWeek(u.getStreakWeek() + 1);
                        u.setStreakMonth(u.getStreakMonth() + 1);
                    } else if (!today.equals(u.getLastRunDate())) {
                        u.setCurrentStreak(1);
                        u.setStreakWeek(1);
                        u.setStreakMonth(1);
                    }

                    u.setLastRunDate(today);
                    u.setLastRunWeek(thisWeek);
                    u.setLastRunMonth(thisMonth);
                    u.setStatus("active");

                    FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(u);
                });
    }

    private void sendAction(String a) {
        Intent i = new Intent(getContext(), TrackingService.class); i.setAction(a);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) requireContext().startForegroundService(i);
        else requireContext().startService(i);
    }

    private void goBack() {
        if (!isAdded()) return;
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(true);
        }
        
        try {
            getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragmentContainer, new HomeFragment(), "HomeFragment")
                .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(android.hardware.SensorEvent event) {
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(false);
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(trackingReceiver, new IntentFilter(TrackingService.TRACKING_UPDATE));
    }

    @Override
    public void onStop() { super.onStop(); LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(trackingReceiver); }

    @Override
    public void onResume() { 
        super.onResume(); 
        if (map != null) map.onResume(); 
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        if (compassOverlay != null) compassOverlay.enableCompass();
        if (rotationSensor != null) sensorManager.registerListener(this, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_UI); 
        startStatusLocationUpdates(); 
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(trackingReceiver, new IntentFilter(TrackingService.TRACKING_UPDATE));
    }

    @Override
    public void onPause() { 
        super.onPause(); 
        if (map != null) map.onPause(); 
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        if (compassOverlay != null) compassOverlay.disableCompass();
        sensorManager.unregisterListener(this); 
        stopStatusLocationUpdates(); 
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(trackingReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] p, @NonNull int[] gr) {
        super.onRequestPermissionsResult(rc, p, gr);
        if (rc == LOCATION_PERMISSION_REQUEST_CODE) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) enableInitialLocation();
        } else if (rc == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(getContext(), R.string.physical_activity_permission_granted, Toast.LENGTH_SHORT).show();
        } else if (rc == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (gr.length > 0 && gr[0] == PackageManager.PERMISSION_GRANTED) startRunning();
        } else if (rc == 999) {
            startRunning();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setNavigationVisibility(true);
        }
        stopStatusLocationUpdates();
    }
}
