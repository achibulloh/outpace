package com.example.pace.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShareActivity extends AppCompatActivity {

    private static final int THEME_COUNT = 10;

    private FrameLayout shareFrame;
    private View tabPaceTheme, tabDefault, paceStyleSelector, defaultControls;
    private LinearLayout dotsContainer;
    private ViewGroup bottomControls;
    private String jarak = "0.00", waktu = "00:00", elev = "0 m", pace = "0:00";
    private RunRecord runData;

    private int currentThemeIndex = 0;
    private boolean isDefaultMode = true;
    private float lastX, lastY;
    private Uri selectedBgUri;
    private int selectedBgColor = Color.TRANSPARENT;

    private View selectedView;
    private View elementEditor, layoutBgEditor;
    private TextView selectedElementName;
    private SeekBar sbSize, sbRotation;

    private List<GeoPoint> pathPoints = new ArrayList<>();

    private final int[] bgColors = {
            Color.TRANSPARENT,
            Color.parseColor("#0F0F12"),
            Color.parseColor("#C8F43A"),
            Color.parseColor("#2A2A2E"),
            Color.parseColor("#001F3F"),
            Color.parseColor("#4B0082"),
            Color.parseColor("#8B0000"),
            Color.parseColor("#004D40"),
            Color.parseColor("#FFFFFF")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_share);
            initViews();
            setupTabs();
            setupThemeNavigator();
            setupSwipePreview();
            setupColorPicker();

            // Default empty setup
            setupDefaultMode();

            int runId = getIntent().getIntExtra("RUN_ID", -1);
            if (runId != -1) {
                new Thread(() -> {
                    try {
                        runData = AppDatabase.getInstance(this).runDao().getRunById(runId);
                        if (runData != null) {
                            jarak = String.format(Locale.getDefault(), "%.2f", runData.getDistance());
                            long t = runData.getDuration();
                            waktu = String.format(Locale.getDefault(), "%02d:%02d", t / 60, t % 60);
                            elev = String.format(Locale.getDefault(), "%.0f m", runData.getElevationGain());
                            double p = runData.getPace();
                            pace = String.format(Locale.getDefault(), "%d:%02d", (int)p, (int)((p - (int)p) * 60));
                            
                            String json = runData.getPathJson();
                            if (json != null && !json.isEmpty()) {
                                pathPoints = new Gson().fromJson(json, new TypeToken<List<GeoPoint>>(){}.getType());
                            }
                            
                            runOnUiThread(() -> {
                                populateTabPreview();
                                if (isDefaultMode) setupDefaultMode();
                                else applyTheme(currentThemeIndex);
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuka editor: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        shareFrame = findViewById(R.id.shareFrame);
        tabPaceTheme = findViewById(R.id.tabPaceTheme);
        tabDefault = findViewById(R.id.tabDefault);
        paceStyleSelector = findViewById(R.id.paceStyleSelector);
        defaultControls = findViewById(R.id.defaultControls);
        dotsContainer = findViewById(R.id.dotsContainer);
        bottomControls = findViewById(R.id.panelContent);

        elementEditor = findViewById(R.id.elementEditor);
        layoutBgEditor = findViewById(R.id.layoutBgEditor);
        selectedElementName = findViewById(R.id.selectedElementName);
        sbSize = findViewById(R.id.sbSize);
        sbRotation = findViewById(R.id.sbRotation);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveShare).setOnClickListener(v -> saveAndShare());

        findViewById(R.id.btnOpenBgEditor).setOnClickListener(v -> {
            if (layoutBgEditor.getVisibility() == View.VISIBLE) {
                smoothLayoutChange();
                layoutBgEditor.setVisibility(View.GONE);
            } else {
                smoothLayoutChange();
                layoutBgEditor.setVisibility(View.VISIBLE);
                elementEditor.setVisibility(View.GONE);
                deselectElement();
            }
        });

        findViewById(R.id.btnAddImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 100);
        });

        setupSliders();
    }

    private void setupSwipePreview() {
        shareFrame.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isDefaultMode) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        if (Math.abs(startX - endX) > 150) {
                            if (startX > endX) currentThemeIndex = (currentThemeIndex + 1) % THEME_COUNT;
                            else currentThemeIndex = (currentThemeIndex - 1 + THEME_COUNT) % THEME_COUNT;
                            applyTheme(currentThemeIndex);
                        }
                        v.performClick();
                        return true;
                }
                return false;
            }
        });
    }

    private void populateTabPreview() {
        TextView tvValue = findViewById(R.id.tvTabPolosValue);
        TextView tvSub = findViewById(R.id.tvTabPolosSub);
        if (tvValue != null) tvValue.setText(jarak);
        if (tvSub != null) tvSub.setText(pace + " /km · " + waktu);
    }

    private void setupTabs() {
        tabPaceTheme.setOnClickListener(v -> {
            isDefaultMode = false;
            smoothLayoutChange();
            deselectElement();
            tabPaceTheme.setBackgroundResource(R.drawable.card_tab_selected);
            ((TextView) findViewById(R.id.tvTabThemeLabel)).setTextColor(ContextCompat.getColor(this, R.color.lime));
            tabDefault.setBackgroundResource(R.drawable.card_glass);
            ((TextView) findViewById(R.id.tvTabPolosMode)).setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
            paceStyleSelector.setVisibility(View.VISIBLE);
            defaultControls.setVisibility(View.GONE);
            layoutBgEditor.setVisibility(View.GONE);
            applyTheme(currentThemeIndex);
        });

        tabDefault.setOnClickListener(v -> {
            isDefaultMode = true;
            smoothLayoutChange();
            deselectElement();
            tabDefault.setBackgroundResource(R.drawable.card_tab_selected);
            ((TextView) findViewById(R.id.tvTabPolosMode)).setTextColor(ContextCompat.getColor(this, R.color.lime));
            tabPaceTheme.setBackgroundResource(R.drawable.card_glass);
            ((TextView) findViewById(R.id.tvTabThemeLabel)).setTextColor(ContextCompat.getColor(this, R.color.muted_fg));
            paceStyleSelector.setVisibility(View.GONE);
            defaultControls.setVisibility(View.VISIBLE);
            layoutBgEditor.setVisibility(View.GONE);
            setupDefaultMode();
        });
    }

    private void setupThemeNavigator() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < THEME_COUNT; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) dpToPx(8), (int) dpToPx(8));
            lp.setMarginEnd((int) dpToPx(6));
            dot.setLayoutParams(lp);
            dotsContainer.addView(dot);
        }
        updateDots();
        findViewById(R.id.btnThemePrev).setOnClickListener(v -> {
            currentThemeIndex = (currentThemeIndex - 1 + THEME_COUNT) % THEME_COUNT;
            applyTheme(currentThemeIndex);
        });
        findViewById(R.id.btnThemeNext).setOnClickListener(v -> {
            currentThemeIndex = (currentThemeIndex + 1) % THEME_COUNT;
            applyTheme(currentThemeIndex);
        });
    }

    private void updateDots() {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            boolean active = i == currentThemeIndex;
            dot.setBackgroundResource(active ? R.drawable.dot_active : R.drawable.dot_inactive);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = (int) dpToPx(active ? 20 : 8);
            dot.setLayoutParams(lp);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void applyTheme(int index) {
        currentThemeIndex = index;
        updateDots();
        shareFrame.removeAllViews();
        int layoutRes = getResources().getIdentifier("layout_share_pace_" + (index + 1), "layout", getPackageName());
        if (layoutRes == 0) return;
        View themeView = LayoutInflater.from(this).inflate(layoutRes, shareFrame, false);

        updateTextView(themeView, R.id.tvShareJarak, jarak);
        updateTextView(themeView, R.id.tvShareWaktu, waktu);
        updateTextView(themeView, R.id.tvShareElev, elev);
        updateTextView(themeView, R.id.tvSharePace, pace);
        updateTextView(themeView, R.id.tvShareRitme, "165 lpm");
        updateTextView(themeView, R.id.tvShareSteps, "6,933");
        updateTextView(themeView, R.id.tvLocation, "📍 Bandung");
        
        PathDrawingView pv = themeView.findViewById(R.id.ivSharePath);
        if (pv != null) pv.setPathPoints(pathPoints);

        shareFrame.addView(themeView);
        scaleToFitPreview(themeView);
        applyCurrentBackground();
    }

    private void setupColorPicker() {
        LinearLayout container = findViewById(R.id.llBgColorContainer);
        if (container == null) return;
        container.removeAllViews();
        for (int color : bgColors) {
            FrameLayout frame = new FrameLayout(this);
            int size = (int) dpToPx(38);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(0, 0, (int)dpToPx(10), 0);
            frame.setLayoutParams(lp);

            View circle = new View(this);
            circle.setLayoutParams(new FrameLayout.LayoutParams(size, size));
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            if (color == Color.TRANSPARENT) {
                shape.setColor(Color.parseColor("#33FFFFFF"));
                shape.setStroke(2, Color.WHITE);
                ImageView ic = new ImageView(this);
                ic.setImageResource(R.drawable.ic_minimize);
                ic.setPadding((int)dpToPx(10), (int)dpToPx(10), (int)dpToPx(10), (int)dpToPx(10));
                ic.setColorFilter(Color.WHITE);
                frame.addView(ic);
            } else {
                shape.setColor(color);
                if (color == Color.WHITE) shape.setStroke(2, Color.LTGRAY);
            }
            circle.setBackground(shape);
            frame.addView(circle);
            frame.setOnClickListener(v -> {
                selectedBgUri = null;
                selectedBgColor = color;
                applyCurrentBackground();
            });
            container.addView(frame);
        }
    }

    private void applyCurrentBackground() {
        if (shareFrame.getChildCount() == 0) return;
        View currentView = shareFrame.getChildAt(0);
        ImageView bg = currentView.findViewById(R.id.ivShareBg);

        if (bg == null && currentView instanceof ViewGroup) {
            bg = new ImageView(this);
            bg.setId(R.id.ivShareBg);
            // Use generic LayoutParams
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            ((ViewGroup) currentView).addView(bg, 0, lp);
            bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        if (bg != null) {
            if (selectedBgUri != null) {
                bg.setVisibility(View.VISIBLE);
                bg.setImageURI(selectedBgUri);
                currentView.setBackgroundColor(Color.TRANSPARENT);
            } else {
                bg.setVisibility(View.GONE);
                currentView.setBackgroundColor(selectedBgColor);
            }
        }
    }

    private void scaleToFitPreview(View content) {
        content.post(() -> {
            int contentW = content.getWidth();
            int contentH = content.getHeight();
            int boxW = shareFrame.getWidth();
            int boxH = shareFrame.getHeight();
            if (contentW == 0 || boxW == 0 || boxH == 0) return;
            float scale = Math.min((float) boxW / contentW, (float) boxH / contentH) * 0.92f;
            content.setPivotX(contentW / 2f);
            content.setPivotY(contentH / 2f);
            content.setScaleX(scale);
            content.setScaleY(scale);
            content.setTranslationX((boxW - contentW) / 2f);
            content.setTranslationY((boxH - contentH) / 2f);
        });
    }

    private void updateTextView(View root, int id, String text) {
        TextView tv = root.findViewById(id);
        if (tv != null) tv.setText(text);
    }

    private void setupSliders() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && selectedView != null) {
                    if (seekBar.getId() == R.id.sbSize) {
                        if (selectedView instanceof TextView) ((TextView) selectedView).setTextSize(10 + progress);
                        else if (selectedView instanceof PathDrawingView) ((PathDrawingView) selectedView).setScaleFactor(progress / 100f);
                        else {
                            float s = 0.5f + (progress / 50.0f);
                            selectedView.setScaleX(s); selectedView.setScaleY(s);
                        }
                    } else {
                        selectedView.setRotation(progress);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
        sbSize.setOnSeekBarChangeListener(listener);
        sbRotation.setOnSeekBarChangeListener(listener);
    }

    private void setupDefaultMode() {
        shareFrame.removeAllViews();
        View defaultView = LayoutInflater.from(this).inflate(R.layout.layout_share_activity, shareFrame, false);
        setupElement(defaultView.findViewById(R.id.tvShareJarak), "Jarak");
        setupElement(defaultView.findViewById(R.id.tvShareElev), "Elevasi");
        setupElement(defaultView.findViewById(R.id.tvShareWaktu), "Waktu");
        setupElement(defaultView.findViewById(R.id.ivSharePath), "Rute");
        setupElement(defaultView.findViewById(R.id.tvPaceWatermark), "Watermark");
        updateTextView(defaultView, R.id.tvShareJarak, jarak + " km");
        updateTextView(defaultView, R.id.tvShareWaktu, waktu);
        updateTextView(defaultView, R.id.tvShareElev, elev);
        PathDrawingView pv = defaultView.findViewById(R.id.ivSharePath);
        if (pv != null) pv.setPathPoints(pathPoints);
        shareFrame.addView(defaultView);
        scaleToFitPreview(defaultView);
        applyCurrentBackground();
        defaultView.setOnClickListener(v -> deselectElement());
    }

    private void setupElement(View view, String name) {
        if (view == null) return;
        view.setTag(name);
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getRawX(); lastY = event.getRawY();
                    selectElement(v);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - lastX; float dy = event.getRawY() - lastY;
                    float ps = (v.getParent() instanceof View) ? ((View) v.getParent()).getScaleX() : 1f;
                    v.setX(v.getX() + dx / (ps > 0 ? ps : 1f)); v.setY(v.getY() + dy / (ps > 0 ? ps : 1f));
                    lastX = event.getRawX(); lastY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    return true;
            }
            return false;
        });
    }

    private void selectElement(View view) {
        if (!isDefaultMode) return;
        if (selectedView != null) selectedView.setForeground(null);
        selectedView = view;
        Drawable glow = ContextCompat.getDrawable(this, R.drawable.element_select_glow);
        if (glow != null) view.setForeground(glow.mutate());
        if (elementEditor.getVisibility() != View.VISIBLE) {
            smoothLayoutChange();
            elementEditor.setVisibility(View.VISIBLE);
            layoutBgEditor.setVisibility(View.GONE);
        }
        selectedElementName.setText(view.getTag().toString());
    }

    private void deselectElement() {
        if (selectedView != null) { selectedView.setForeground(null); selectedView = null; }
        if (elementEditor.getVisibility() == View.VISIBLE) { smoothLayoutChange(); elementEditor.setVisibility(View.GONE); }
    }

    private void smoothLayoutChange() {
        TransitionManager.beginDelayedTransition(bottomControls, new AutoTransition().setDuration(220));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedBgUri = data.getData(); applyCurrentBackground();
        }
    }

    private void saveAndShare() {
        if (shareFrame.getWidth() <= 0) { Toast.makeText(this, "Tunggu sejenak...", Toast.LENGTH_SHORT).show(); return; }
        shareFrame.post(() -> {
            try {
                Bitmap b = Bitmap.createBitmap(shareFrame.getWidth(), shareFrame.getHeight(), Bitmap.Config.ARGB_8888);
                shareFrame.draw(new Canvas(b));
                File f = new File(getCacheDir(), "images"); f.mkdirs();
                File file = new File(f, "share.png");
                FileOutputStream out = new FileOutputStream(file); b.compress(Bitmap.CompressFormat.PNG, 100, out); out.close();
                saveImageToGallery(b);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                Intent si = new Intent(Intent.ACTION_SEND); si.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                si.putExtra(Intent.EXTRA_STREAM, uri); si.setType("image/png");
                startActivity(Intent.createChooser(si, "Bagikan"));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void saveImageToGallery(Bitmap b) {
        String n = "OUTPACE_" + System.currentTimeMillis() + ".png";
        try {
            OutputStream os;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues cv = new ContentValues(); cv.put(MediaStore.MediaColumns.DISPLAY_NAME, n);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "OUTPACE");
                Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                os = u != null ? getContentResolver().openOutputStream(u) : null;
            } else {
                File d = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OUTPACE");
                if (!d.exists()) d.mkdirs();
                os = new FileOutputStream(new File(d, n));
            }
            if (os != null) { b.compress(Bitmap.CompressFormat.PNG, 100, os); os.close(); }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
