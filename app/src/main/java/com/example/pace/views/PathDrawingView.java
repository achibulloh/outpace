package com.example.pace.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class PathDrawingView extends View {
    private Paint pathPaint;
    private Path path;
    private List<GeoPoint> points;
    private int lineColor = Color.parseColor("#C8F43A");
    private float scaleFactor = 0.8f; // Default 80% to give some natural padding

    public PathDrawingView(Context context) {
        super(context);
        init();
    }

    public PathDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PathDrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(lineColor);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(10f);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        path = new Path();
    }

    public void setLineColor(int color) {
        this.lineColor = color;
        pathPaint.setColor(color);
        invalidate();
    }

    public void setPathPoints(List<GeoPoint> points) {
        this.points = points;
        invalidate();
    }

    public void setScaleFactor(float factor) {
        this.scaleFactor = factor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points == null || points.size() < 2) return;

        float width = getWidth();
        float height = getHeight();

        // 1. Find Min/Max coordinates
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double minLng = Double.POSITIVE_INFINITY, maxLng = Double.NEGATIVE_INFINITY;

        for (GeoPoint p : points) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLng = Math.min(minLng, p.getLongitude());
            maxLng = Math.max(maxLng, p.getLongitude());
        }

        double deltaLat = maxLat - minLat;
        double deltaLng = maxLng - minLng;

        if (deltaLat == 0) deltaLat = 0.0001;
        if (deltaLng == 0) deltaLng = 0.0001;

        // 2. Calculate scale to fit while maintaining aspect ratio
        float usableWidth = width * scaleFactor;
        float usableHeight = height * scaleFactor;
        
        float scale = (float) Math.min(usableWidth / deltaLng, usableHeight / deltaLat);

        // Center calculation
        double centerLng = (minLng + maxLng) / 2;
        double centerLat = (minLat + maxLat) / 2;
        float viewCenterX = width / 2;
        float viewCenterY = height / 2;

        path.reset();
        for (int i = 0; i < points.size(); i++) {
            GeoPoint p = points.get(i);
            // Conversion logic: (Lng - centerLng) * scale
            // Note: Latitude is inverted on screen Y axis
            float x = viewCenterX + (float)((p.getLongitude() - centerLng) * scale);
            float y = viewCenterY - (float)((p.getLatitude() - centerLat) * scale);

            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }

        canvas.drawPath(path, pathPaint);
    }
}
