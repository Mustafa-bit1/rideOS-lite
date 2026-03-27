package com.fanconn.rideOS;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class BatteryArcView extends View {

    private Paint bgPaint;
    private Paint progressPaint;

    private Path fullPath;
    private Path segmentPath;
    private PathMeasure pathMeasure;
    private float batteryLevel = 0;
    private float maxBattery = 100;
    private int currentColor = Color.parseColor("#4AD560"); // default green

    public BatteryArcView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //  Paint init
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 🔹 Background arc
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(55);
        bgPaint.setColor(Color.parseColor("#373737"));
        bgPaint.setStrokeCap(Paint.Cap.SQUARE);

        //  Battery progress arc
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(58);
        progressPaint.setColor(currentColor);
        progressPaint.setStrokeCap(Paint.Cap.SQUARE);

        segmentPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        fullPath = new Path();

        float startX = 40;
        float startY = 0;   //  start from TOP now

        float endX = w - 40;
        float endY = 0;     //  end at TOP

        //  Inverted curve (goes downward)
//        float midY = (h / 9f);
        float midY = h - (h / 1.73f);


        float leftControlX = w * 0.15f;
        float rightControlX = w * 0.85f;

        float plateauStartX = w * 0.32f;
        float plateauEndX = w * 0.68f;

        fullPath.moveTo(startX, startY);

        // 🔹 Left curve (top → down)
        fullPath.quadTo(
                leftControlX, midY,
                plateauStartX, midY
        );

        // 🔹 Plateau (bottom flat)
        fullPath.lineTo(plateauEndX, midY);

        //  Right curve (down → top)
        fullPath.quadTo(
                rightControlX, midY,
                endX, endY
        );

        pathMeasure = new PathMeasure(fullPath, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (blockDraw) return;
        // 🔹 Draw background
        canvas.drawPath(fullPath, bgPaint);

        if (pathMeasure == null) return;

        // 🔹 Progress calculation
        float progress = batteryLevel / maxBattery;

        float length = pathMeasure.getLength();
        float stop = length * progress;

        segmentPath.reset();
        pathMeasure.getSegment(0, stop, segmentPath, true);

        // 🔹 Draw battery progress
        canvas.drawPath(segmentPath, progressPaint);
    }
    public void blockInitialDraw(boolean block) {
        this.blockDraw = block;
    }
    private boolean isIntroMode = false;
    private boolean blockDraw = false;
    //Set battery value
    public void setBatteryLevel(float battery) {
        this.batteryLevel = battery;

        int newColor;

        if (isIntroMode) {
            //  ALWAYS GREEN during intro
            newColor = Color.parseColor("#4AD560");
        } else {

            float ratio;

            if (battery <= 25) {
                //  Deep red → bright red
                ratio = battery / 25f;
                newColor = interpolateColor(
                        Color.parseColor("#8D1A09"),
                        Color.parseColor("#E0290E"),
                        ratio
                );

            } else if (battery <= 60) {
                //  red → mid green
                ratio = (battery - 25f) / (60f - 25f);
                newColor = interpolateColor(
                        Color.parseColor("#E0290E"),
                        Color.parseColor("#8BD54A"),
                        ratio
                );

            } else {
                //  mid green → full green
                ratio = (battery - 60f) / (100f - 60f);
                newColor = interpolateColor(
                        Color.parseColor("#8BD54A"),
                        Color.parseColor("#4AD560"),
                        ratio
                );
            }
        }

        if (newColor != currentColor) {
            progressPaint.setColor(newColor);
            currentColor = newColor;
        }

        invalidate();
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        batteryLevel = 0;   //  HARD RESET BEFORE FIRST DRAW
    }
    public void setIntroMode(boolean intro) {
        isIntroMode = intro;
    }
    public float getBatteryLevel() {
        return batteryLevel;
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