package com.example.pace.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class LineChartView extends View {
    private Paint linePaint, fillPaint, pointPaint, labelPaint, selectedPaint, tooltipPaint, tooltipTextPaint;
    private Path linePath, fillPath;
    
    private float[] rawValues = {};
    private String[] infoLabels = {};
    private float[] normalizedValues = {};
    private int selectedIndex = -1;

    private int chartColor = Color.parseColor("#C8F43A");

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            android.content.res.TypedArray a = context.obtainStyledAttributes(attrs, com.example.pace.R.styleable.LineChartView);
            chartColor = a.getColor(com.example.pace.R.styleable.LineChartView_chartColor, chartColor);
            a.recycle();
        }
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(chartColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(chartColor);
        pointPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#76767F"));
        labelPaint.setTextSize(26f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(2f);

        tooltipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipPaint.setColor(Color.parseColor("#E6000000"));
        tooltipPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(28f);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        linePath = new Path();
        fillPath = new Path();
    }

    public void setChartColor(int color) {
        this.chartColor = color;
        if (linePaint != null) linePaint.setColor(color);
        if (pointPaint != null) pointPaint.setColor(color);
        invalidate();
    }

    public void setData(float[] newValues) {
        setDetailedData(newValues, null);
    }

    public void setDetailedData(float[] values, String[] labels) {
        if (values == null || values.length == 0) return;
        this.rawValues = values;
        this.infoLabels = labels;
        this.normalizedValues = new float[values.length];
        
        float max = 0.1f;
        float min = 999999f;
        for (float v : values) {
            if (v > max) max = v;
            if (v < min) min = v;
        }
        
        if (max == min) {
            for (int i = 0; i < values.length; i++) normalizedValues[i] = 0.5f;
        } else {
            for (int i = 0; i < values.length; i++) {
                normalizedValues[i] = (values[i] - min) / (max - min) * 0.6f + 0.2f;
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (normalizedValues.length < 2) return false;
        
        float width = getWidth();
        float spacing = width / (normalizedValues.length - 1);
        
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            int index = Math.round(x / spacing);
            if (index >= 0 && index < normalizedValues.length) {
                selectedIndex = index;
                invalidate();
            }
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            selectedIndex = -1;
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (normalizedValues.length == 0) return;

        float width = getWidth();
        float height = getHeight();
        float labelPadding = (infoLabels != null && infoLabels.length <= 7) ? 50f : 20f;
        float chartHeight = height - labelPadding;
        
        // Handle single point or multiple points
        if (normalizedValues.length == 1) {
            float x = width / 2f;
            float y = chartHeight * (1f - normalizedValues[0]);
            canvas.drawCircle(x, y, 8f, pointPaint);
            if (infoLabels != null && infoLabels.length > 0) {
                canvas.drawText(infoLabels[0], x, height - 10f, labelPaint);
            }
            return;
        }

        float spacing = width / (normalizedValues.length - 1);

    linePath.reset();
    fillPath.reset();
    
    int alphaColor = Color.argb(77, Color.red(chartColor), Color.green(chartColor), Color.blue(chartColor));
    fillPaint.setShader(new LinearGradient(0, 0, 0, chartHeight, 
            alphaColor, Color.TRANSPARENT, Shader.TileMode.CLAMP));

    for (int i = 0; i < normalizedValues.length; i++) {
        float x = i * spacing;
        float y = chartHeight * (1f - normalizedValues[i]);

        if (i == 0) {
            linePath.moveTo(x, y);
            fillPath.moveTo(x, chartHeight);
            fillPath.lineTo(x, y);
        } else {
            linePath.lineTo(x, y);
        }
        
        if (i == normalizedValues.length - 1) {
            fillPath.lineTo(x, y);
            fillPath.lineTo(x, chartHeight);
            fillPath.close();
        }
    }

    canvas.drawPath(fillPath, fillPaint);
    canvas.drawPath(linePath, linePaint);

    // Draw X-Axis Labels if provided
    if (infoLabels != null && infoLabels.length == normalizedValues.length && normalizedValues.length <= 7) {
        for (int i = 0; i < infoLabels.length; i++) {
            float x = i * spacing;
            canvas.drawText(infoLabels[i], x, height - 10f, labelPaint);
        }
    }

        // Draw selection line and tooltip
        if (selectedIndex != -1) {
            float x = selectedIndex * spacing;
            float y = chartHeight * (1f - normalizedValues[selectedIndex]);
            
            canvas.drawLine(x, 0, x, chartHeight, selectedPaint);
            canvas.drawCircle(x, y, 12f, pointPaint);
            canvas.drawCircle(x, y, 14f, selectedPaint);

            String text = String.valueOf(rawValues[selectedIndex]);
            if (infoLabels != null && selectedIndex < infoLabels.length) {
                text = infoLabels[selectedIndex];
            }
            
            float tw = tooltipTextPaint.measureText(text) + 40;
            float tx = Math.max(tw/2, Math.min(width - tw/2, x));
            canvas.drawRoundRect(tx - tw/2, 10, tx + tw/2, 60, 10, 10, tooltipPaint);
            canvas.drawText(text, tx, 45, tooltipTextPaint);
        }
    }
}
