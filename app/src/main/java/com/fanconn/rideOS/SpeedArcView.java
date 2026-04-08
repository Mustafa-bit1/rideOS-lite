package com.fanconn.rideOS;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class SpeedArcView extends View {
    private int currentColor = Color.parseColor("#2097F6");
    private Paint bgPaint;
    private Paint progressPaint;

    private Path fullPath;
    private Path segmentPath;
    private PathMeasure pathMeasure;

    private float speed = 0;
    private float maxSpeed = 100;

    public SpeedArcView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // ✅ 1. Initialize BOTH paints FIRST
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // ✅ 2. Now configure bgPaint
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(55);
        bgPaint.setColor(Color.parseColor("#323232")); // background color
        bgPaint.setStrokeCap(Paint.Cap.SQUARE);

        // ✅ 3. Now configure progressPaint
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(57);
        progressPaint.setColor(Color.parseColor("#2097F6")); // default blue
        progressPaint.setStrokeCap(Paint.Cap.SQUARE);

        // ✅ 4. Init path
        segmentPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        fullPath = new Path();

        float startX = 40;
        float startY = h;   // 🔥 start from TOP now

        float endX = w - 40;
        float endY = h;     // 🔥 end at TOP

        // 🔥 Inverted curve (goes downward)
        float midY = (h / 9f);
//        float midY = h - (h / 8f);


        float leftControlX = w * 0.15f;
        float rightControlX = w * 0.85f;

        float plateauStartX = w * 0.35f;
        float plateauEndX = w * 0.65f;

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

        // Draw full background arc
        canvas.drawPath(fullPath, bgPaint);

        // Calculate progress
        float progress = speed / maxSpeed;

        float length = pathMeasure.getLength();
        float stop = length * progress;

        segmentPath.reset();
        pathMeasure.getSegment(0, stop, segmentPath, true);

        // Draw progress arc
        canvas.drawPath(segmentPath, progressPaint);
    }
    private boolean isIntroMode = false;
    public void setIntroMode(boolean intro) {
        isIntroMode = intro;
    }
    public void setSpeed(float speed, float currentColour) {
        this.speed = speed;   // ✅ animation still based on speed

        int newColor;

        if (isIntroMode) {
            //  ALWAYS BLUE during intro
            newColor = Color.parseColor("#2097F6");
        } else {

            // color based on current consumption
            if(currentColour<0){
                newColor = Color.parseColor("#01FF58");
            } else if (currentColour <= 30) {
                newColor = Color.parseColor("#2097F6");
            } else if (currentColour <= 70) {
                newColor = Color.parseColor("#FF5C00");
            } else {
                newColor = Color.parseColor("#C81F1F");
            }
        }

        if (newColor != currentColor) {
            progressPaint.setColor(newColor);
            currentColor = newColor;
        }

        invalidate();
    }
    public float getCurrentSpeed() {
        return speed;
    }
}