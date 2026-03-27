package com.fanconn.rideOS;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class ChargingArcView extends View {

    private Paint bgPaint;
    private Paint progressPaint;

    private Path fullPath;
    private Path segmentPath;
    private PathMeasure pathMeasure;

    private float batteryLevel = 0;
    private float maxBattery = 100;

    private int currentColor = Color.parseColor("#4AD560");

    //  PULSE VARIABLES
    private boolean isPulseMode = false;
    private float pulseBoost = 0f;

    private boolean blockDraw = false;

    public ChargingArcView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayerType(LAYER_TYPE_HARDWARE, null); //  smooth rendering

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(55);
        bgPaint.setColor(Color.parseColor("#373737"));
        bgPaint.setStrokeCap(Paint.Cap.SQUARE);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(57);
        progressPaint.setColor(currentColor);
        progressPaint.setStrokeCap(Paint.Cap.SQUARE);

        segmentPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        fullPath = new Path();

        float startX = 40;
        float startY = 5;

        float endX = w - 40;
        float endY = 5;

        float midY = h - (h / 1.73f);

        float leftControlX = w * 0.15f;
        float rightControlX = w * 0.85f;

        float plateauStartX = w * 0.32f;
        float plateauEndX = w * 0.68f;

        fullPath.moveTo(startX, startY);

        fullPath.quadTo(leftControlX, midY, plateauStartX, midY);
        fullPath.lineTo(plateauEndX, midY);
        fullPath.quadTo(rightControlX, midY, endX, endY);

        pathMeasure = new PathMeasure(fullPath, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (blockDraw) return;

        canvas.drawPath(fullPath, bgPaint);

        if (pathMeasure == null) return;

        // APPLY PULSE
        float displayLevel = batteryLevel;
        if (isPulseMode) {
            displayLevel += pulseBoost;
        }

        if (displayLevel > 100) displayLevel = 100;

        float progress = displayLevel / maxBattery;

        float length = pathMeasure.getLength();
        float stop = length * progress;

        segmentPath.reset();
        pathMeasure.getSegment(0, stop, segmentPath, true);

        canvas.drawPath(segmentPath, progressPaint);
    }

    //  BASE SOC
    public void setBatteryLevel(float battery) {
        this.batteryLevel = battery;

        // 🔥 COLOR LOGIC (same as dashboard)
        float ratio;
        int newColor;

        if (battery <= 25) {
            ratio = battery / 25f;
            newColor = interpolateColor(
                    Color.parseColor("#8D1A09"),
                    Color.parseColor("#E0290E"),
                    ratio
            );
        } else if (battery <= 60) {
            ratio = (battery - 25f) / (60f - 25f);
            newColor = interpolateColor(
                    Color.parseColor("#E0290E"),
                    Color.parseColor("#8BD54A"),
                    ratio
            );
        } else {
            ratio = (battery - 60f) / (100f - 60f);
            newColor = interpolateColor(
                    Color.parseColor("#8BD54A"),
                    Color.parseColor("#4AD560"),
                    ratio
            );
        }

        if (newColor != currentColor) {
            progressPaint.setColor(newColor);
            currentColor = newColor;
        }

        invalidate();
    }

    //PULSE CONTROL
    public void setPulseMode(boolean enable) {
        isPulseMode = enable;
    }

    public void setPulseValue(float value) {
        pulseBoost = value;

        // Optional glow
        progressPaint.setAlpha((int) (180 + value * 5));

        invalidate();
    }

    public void blockInitialDraw(boolean block) {
        this.blockDraw = block;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        batteryLevel = 0;
        pulseBoost = 0;
    }

    private int interpolateColor(int startColor, int endColor, float fraction) {

        int startA = Color.alpha(startColor);
        int startR = Color.red(startColor);
        int startG = Color.green(startColor);
        int startB = Color.blue(startColor);

        int endA = Color.alpha(endColor);
        int endR = Color.red(endColor);
        int endG = Color.green(endColor);
        int endB = Color.blue(endColor);

        return Color.argb(
                (int) (startA + (endA - startA) * fraction),
                (int) (startR + (endR - startR) * fraction),
                (int) (startG + (endG - startG) * fraction),
                (int) (startB + (endB - startB) * fraction)
        );
    }
}