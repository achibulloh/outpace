package com.example.pace.activities;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.pace.R;
import com.example.pace.database.AppDatabase;
import com.example.pace.model.RunRecord;
import com.example.pace.views.PathDrawingView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShareActivity extends AppCompatActivity {

    private CardView cardCapture;
    private ImageView ivBg, ivLogo;
    private View viewBgDim;
    private PathDrawingView ivPath;
    private LinearLayout draggableStats;
    private TextView tvJarak, tvWaktu, tvPace, tvTitle, tvLabelApp;
    private TextView tvLabelJarak, tvLabelWaktu, tvLabelPace;
    
    private View scrollTemplates, layoutBgControls, layoutTextSettings, layoutScaleControls, scrollPathColors, layoutMoveControls;
    private LinearLayout llTemplateContainer, llColorContainer, llFontContainer, llPathColorContainer;
    private SeekBar sbBgAlpha, sbPathScale, sbTextScale;
    private Button btnModeDragStats, btnModeDragPath;

    private RunRecord currentRun;
    private List<GeoPoint> pathPoints = new ArrayList<>();
    private static final int PICK_IMAGE = 101;

    // State
    private float dX, dY;
    private boolean isDragStatsMode = true;
    private int paceLime = Color.parseColor("#C8F43A");
    private int stravaOrange = Color.parseColor("#FC4C02");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        initViews();
        setupListeners();
        setupUniversalDraggable();

        int runId = getIntent().getIntExtra("RUN_ID", -1);
        if (runId != -1) loadRunData(runId);

        setupTemplates();
        setupColors();
        setupFonts();
        setupPathColors();
        
        showTab(scrollTemplates);
        applyTemplate(0);
    }

    private void initViews() {
        cardCapture = findViewById(R.id.cardCapture);
        ivBg = findViewById(R.id.ivShareBg);
        ivLogo = findViewById(R.id.ivShareLogo);
        viewBgDim = findViewById(R.id.viewBgDim);
        ivPath = findViewById(R.id.ivSharePath);
        draggableStats = findViewById(R.id.draggableStats);
        
        tvJarak = findViewById(R.id.tvShareJarak);
        tvWaktu = findViewById(R.id.tvShareWaktu);
        tvPace = findViewById(R.id.tvSharePace);
        tvTitle = findViewById(R.id.tvShareTitle);
        tvLabelApp = findViewById(R.id.tvLabelApp);
        tvLabelJarak = findViewById(R.id.tvLabelJarak);
        tvLabelWaktu = findViewById(R.id.tvLabelWaktu);
        tvLabelPace = findViewById(R.id.tvLabelPace);

        scrollTemplates = findViewById(R.id.scrollTemplates);
        layoutBgControls = findViewById(R.id.layoutBgControls);
        layoutScaleControls = findViewById(R.id.layoutScaleControls);
        layoutTextSettings = findViewById(R.id.layoutTextSettings);
        scrollPathColors = findViewById(R.id.scrollPathColors);
        layoutMoveControls = findViewById(R.id.layoutMoveControls);
        
        llTemplateContainer = findViewById(R.id.llTemplateContainer);
        llColorContainer = findViewById(R.id.llColorContainer);
        llFontContainer = findViewById(R.id.llFontContainer);
        llPathColorContainer = findViewById(R.id.llPathColorContainer);
        
        sbBgAlpha = findViewById(R.id.sbBgAlpha);
        sbPathScale = findViewById(R.id.sbPathScale);
        sbTextScale = findViewById(R.id.sbTextScale);
        
        btnModeDragStats = findViewById(R.id.btnModeDragStats);
        btnModeDragPath = findViewById(R.id.btnModeDragPath);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupUniversalDraggable() {
        View.OnTouchListener touchListener = (view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float newX = event.getRawX() + dX;
                float newY = event.getRawY() + dY;
                view.setX(newX);
                view.setY(newY);
            }
            return true;
        };

        draggableStats.setOnTouchListener(touchListener);
        ivPath.setOnTouchListener(touchListener);
    }

    private void setupListeners() {
        findViewById(R.id.btnBackShare).setOnClickListener(v -> finish());
        findViewById(R.id.btnDoneShare).setOnClickListener(v -> shareImage());
        findViewById(R.id.btnSaveGallery).setOnClickListener(v -> saveImageToGallery());
        
        findViewById(R.id.btnTabTemplate).setOnClickListener(v -> showTab(scrollTemplates));
        findViewById(R.id.btnTabBg).setOnClickListener(v -> showTab(layoutBgControls));
        findViewById(R.id.btnTabScale).setOnClickListener(v -> showTab(layoutScaleControls));
        findViewById(R.id.btnTabPath).setOnClickListener(v -> showTab(scrollPathColors));
        findViewById(R.id.btnTabText).setOnClickListener(v -> showTab(layoutTextSettings));
        findViewById(R.id.btnTabMove).setOnClickListener(v -> showTab(layoutMoveControls));

        findViewById(R.id.btnSelectPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        sbBgAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) { viewBgDim.setAlpha(p/100f); }
            @Override public void onStartTrackingTouch(SeekBar s) {} @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        sbPathScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) { ivPath.setScaleFactor(p/100f); }
            @Override public void onStartTrackingTouch(SeekBar s) {} @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        sbTextScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean f) {
                draggableStats.setScaleX(p/100f); draggableStats.setScaleY(p/100f);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {} @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        btnModeDragStats.setOnClickListener(v -> {
            isDragStatsMode = true;
            btnModeDragStats.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lime));
            btnModeDragPath.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.muted));
            draggableStats.bringToFront();
        });

        btnModeDragPath.setOnClickListener(v -> {
            isDragStatsMode = false;
            btnModeDragPath.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.lime));
            btnModeDragStats.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.muted));
            ivPath.bringToFront();
        });
    }

    private void showTab(View tab) {
        scrollTemplates.setVisibility(View.GONE);
        layoutBgControls.setVisibility(View.GONE);
        layoutScaleControls.setVisibility(View.GONE);
        layoutTextSettings.setVisibility(View.GONE);
        scrollPathColors.setVisibility(View.GONE);
        layoutMoveControls.setVisibility(View.GONE);
        tab.setVisibility(View.VISIBLE);
    }

    private void loadRunData(int id) {
        new Thread(() -> {
            currentRun = AppDatabase.getInstance(this).runDao().getRunById(id);
            if (currentRun != null) {
                pathPoints = new Gson().fromJson(currentRun.getPathJson(), new TypeToken<List<GeoPoint>>(){}.getType());
                runOnUiThread(() -> {
                    tvJarak.setText(String.format(Locale.getDefault(), "%.2f km", currentRun.getDistance()));
                    long t = currentRun.getDuration();
                    tvWaktu.setText(String.format(Locale.getDefault(), "%dm %ds", t/60, t%60));
                    double p = currentRun.getPace();
                    tvPace.setText(String.format(Locale.getDefault(), "%d:%02d /km", (int)p, (int)((p-(int)p)*60)));
                    ivPath.setPathPoints(pathPoints);
                });
            }
        }).start();
    }

    private void setupTemplates() {
        String[] templates = {"Classic Strava", "OUTPACE Lime", "Minimalist", "Banner Top", "Centered Route", "Split Vertical", "Split Horizontal", "Grid Orange", "Clear Floating", "Freestyle"};
        for (int i = 0; i < templates.length; i++) {
            final int idx = i;
            TextView btn = new TextView(this);
            btn.setText(templates[i]);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundResource(R.drawable.rounded_card);
            btn.setPadding(32, 16, 32, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 16, 0);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> applyTemplate(idx));
            llTemplateContainer.addView(btn);
        }
    }

    private void applyTemplate(int idx) {
        ivPath.setX(0); ivPath.setY(0);
        draggableStats.setX(40); draggableStats.setY(40);
        draggableStats.setOrientation(LinearLayout.VERTICAL);
        viewBgDim.setBackgroundColor(Color.BLACK);
        viewBgDim.setAlpha(0.4f);
        cardCapture.setCardBackgroundColor(Color.parseColor("#090909"));
        ivBg.setVisibility(View.VISIBLE);

        switch (idx) {
            case 0: // Classic Strava
                ivPath.setLineColor(stravaOrange);
                setAllTextColor(Color.WHITE);
                draggableStats.post(() -> draggableStats.setY(cardCapture.getHeight() - draggableStats.getHeight() - 40));
                break;
            case 1: // OUTPACE Lime
                ivPath.setLineColor(paceLime);
                setAllTextColor(Color.WHITE);
                break;
            case 2: // Minimalist
                setAllTextColor(Color.BLACK);
                cardCapture.setCardBackgroundColor(Color.WHITE);
                ivBg.setVisibility(View.GONE);
                viewBgDim.setVisibility(View.GONE);
                break;
            case 3: // Banner Top
                draggableStats.setX(0); draggableStats.setY(0);
                draggableStats.setBackgroundColor(Color.parseColor("#AA000000"));
                break;
            case 4: // Centered Route
                ivPath.setScaleFactor(1.5f);
                draggableStats.setGravity(Gravity.CENTER);
                break;
            case 7: // Grid Orange
                ivBg.setVisibility(View.GONE);
                viewBgDim.setBackgroundResource(R.drawable.bg_checkerboard);
                viewBgDim.setAlpha(1.0f);
                ivPath.setLineColor(stravaOrange);
                break;
            case 9: // Freestyle
                Toast.makeText(this, "Geser rute dan teks sesuka hati!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void setupColors() {
        int[] colors = {Color.WHITE, Color.BLACK, paceLime, stravaOrange, Color.RED, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
        for (int c : colors) {
            View v = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(80, 80);
            lp.setMargins(0, 0, 16, 0);
            v.setLayoutParams(lp);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(c);
            gd.setStroke(2, Color.GRAY);
            v.setBackground(gd);
            v.setOnClickListener(v1 -> setAllTextColor(c));
            llColorContainer.addView(v);
        }
    }

    private void setupPathColors() {
        int[] colors = {paceLime, stravaOrange, Color.WHITE, Color.BLACK, Color.RED, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA};
        for (int c : colors) {
            View v = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(80, 80);
            lp.setMargins(0, 0, 16, 0);
            v.setLayoutParams(lp);
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(c);
            v.setBackground(gd);
            v.setOnClickListener(v1 -> ivPath.setLineColor(c));
            llPathColorContainer.addView(v);
        }
    }

    private void setupFonts() {
        Typeface[] fonts = {Typeface.DEFAULT, Typeface.DEFAULT_BOLD, Typeface.MONOSPACE, Typeface.SERIF};
        String[] names = {"Normal", "Bold", "Mono", "Serif"};
        for (int i = 0; i < fonts.length; i++) {
            final Typeface tf = fonts[i];
            TextView btn = new TextView(this);
            btn.setText(names[i]);
            btn.setTypeface(tf);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundResource(R.drawable.rounded_card);
            btn.setPadding(24, 12, 24, 12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 16, 0);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> setAllTextFont(tf));
            llFontContainer.addView(btn);
        }
    }

    private void setAllTextColor(int c) {
        tvJarak.setTextColor(c); tvWaktu.setTextColor(c); tvPace.setTextColor(c);
        tvTitle.setTextColor(c); tvLabelApp.setTextColor(c);
        tvLabelJarak.setTextColor(c); tvLabelWaktu.setTextColor(c); tvLabelPace.setTextColor(c);
        ivLogo.setColorFilter(c);
    }

    private void setAllTextFont(Typeface tf) {
        tvJarak.setTypeface(tf); tvWaktu.setTypeface(tf); tvPace.setTypeface(tf);
        tvTitle.setTypeface(tf); tvLabelApp.setTypeface(tf);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            ivBg.setImageURI(data.getData());
        }
    }

    private Bitmap captureBitmap() {
        Bitmap b = Bitmap.createBitmap(cardCapture.getWidth(), cardCapture.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        cardCapture.draw(c);
        return b;
    }

    private void shareImage() {
        Bitmap b = captureBitmap();
        try {
            File cp = new File(getCacheDir(), "images"); cp.mkdirs();
            File f = new File(cp, "OUTPACE_share.png");
            FileOutputStream s = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.PNG, 100, s); s.close();
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent sh = new Intent(Intent.ACTION_SEND);
            sh.setType("image/png"); sh.putExtra(Intent.EXTRA_STREAM, u);
            sh.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(sh, "Bagikan"));
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membagikan", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToGallery() {
        Bitmap bitmap = captureBitmap();
        String filename = "OUTPACE_" + System.currentTimeMillis() + ".png";
        OutputStream fos;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "OUTPACE");
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                File imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File file = new File(imagesDir, filename);
                fos = new FileOutputStream(file);
            }

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "Berhasil disimpan ke Galeri", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan ke Galeri", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
