package com.example.pace.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class CalendarDotsView extends View {
    private Paint activePaint;
    private Paint inactivePaint;
    private Paint textPaint;
    private int rows = 5;
    private int cols = 7;
    private Set<Integer> activeDays = new HashSet<>();

    public CalendarDotsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activePaint.setColor(Color.parseColor("#334D1A"));
        activePaint.setStyle(Paint.Style.FILL);

        inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        inactivePaint.setColor(Color.parseColor("#1AFFFFFF"));
        inactivePaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#CDFF00"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setActiveDays(Set<Integer> days) {
        this.activeDays = days;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int itemSize = width / cols;
        setMeasuredDimension(width, itemSize * rows);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float itemSize = width / cols;
        float radius = itemSize * 0.4f;

        int day = 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (day > 30) break;

                float x = c * itemSize + itemSize / 2;
                float y = r * itemSize + itemSize / 2;

                boolean isActive = activeDays.contains(day);
                
                canvas.drawCircle(x, y, radius, isActive ? activePaint : inactivePaint);
                
                textPaint.setColor(isActive ? Color.parseColor("#CDFF00") : Color.parseColor("#9CA5B3"));
                canvas.drawText(String.valueOf(day), x, y + 12f, textPaint);
                
                day++;
            }
        }
    }
}
