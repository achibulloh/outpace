package com.example.pace.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class BarChartView extends View {
    private Paint barPaint;
    private Paint labelPaint;
    private String[] labels = null;
    private float[] values = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
    private boolean showLabels = true;

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#C8F43A")); // Use @color/lime
        barPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#76767F")); // Use @color/muted_fg
        labelPaint.setTextSize(26f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
        this.showLabels = true;
        invalidate();
    }

    public void setData(float[] newValues) {
        if (newValues == null || newValues.length == 0) return;
        
        this.values = new float[newValues.length];
        float max = 0.1f;
        for (float v : newValues) if (v > max) max = v;
        
        for (int i = 0; i < newValues.length; i++) {
            this.values[i] = (newValues[i] / max) * 0.8f + 0.1f;
        }
        invalidate();
    }

    public void setShowLabels(boolean show) {
        this.showLabels = show;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values == null || values.length == 0) return;

        float width = getWidth();
        float height = getHeight();
        float labelHeight = showLabels ? 60f : 0f;
        float chartHeight = height - labelHeight;
        
        float spacing = width / (float) values.length;
        float barWidth = spacing * 0.6f;

        for (int i = 0; i < values.length; i++) {
            float x = spacing / 2 + i * spacing;
            float barHeight = chartHeight * values[i];
            
            RectF rect = new RectF(x - barWidth/2, chartHeight - barHeight, x + barWidth/2, chartHeight);
            canvas.drawRoundRect(rect, 10f, 10f, barPaint);
            
            if (showLabels && labels != null && i < labels.length) {
                canvas.drawText(labels[i], x, height - 10f, labelPaint);
            }
        }
    }
}
