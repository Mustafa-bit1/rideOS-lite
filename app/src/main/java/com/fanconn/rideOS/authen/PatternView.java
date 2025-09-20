package com.fanconn.rideOS.authen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.fanconn.rideOS.R;

import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {

    private Paint circlePaint, linePaint;
    private float[][] dotCenters;
    private List<Integer> pattern = new ArrayList<>();
    private boolean isDrawing = false;
    private Path linePath = new Path();
    private PatternCompleteListener listener;
    private float radius;
    private int padding;

    private SoundPool soundPool;
    private int soundBeepId;
    private boolean soundLoaded = false;
    private boolean isMuted;

    public PatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setOnPatternCompleteListener(PatternCompleteListener listener) {
        this.listener = listener;
    }

    private void init(Context context) {
        setBackgroundColor(Color.parseColor("#000000"));

        radius = dpToPx(context, 19); // visual radius
        padding = (int) dpToPx(context, 32);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.parseColor("#FFFFFFFF"));
        circlePaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#DA6200"));
        linePaint.setStrokeWidth(dpToPx(context, 4));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        soundBeepId = soundPool.load(context, R.raw.dottap33, 1);
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) soundLoaded = true;
        });

        isMuted = context.getSharedPreferences("volume_prefs", Context.MODE_PRIVATE)
                .getBoolean("is_muted", false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (dotCenters == null) calculateDotCenters();

        for (float[] center : dotCenters) {
            canvas.drawCircle(center[0], center[1], radius, circlePaint);
        }

        canvas.drawPath(linePath, linePaint);
    }

    public void setLoginMode(boolean isLoginMode) {
        linePaint.setAlpha(isLoginMode ? 0 : 255);
        invalidate();
    }

    private void calculateDotCenters() {
        dotCenters = new float[9][2];
        int width = getWidth();
        int height = getHeight();

        float spacingFactor = 1f;
        int gridWidth = (int) (getWidth() / spacingFactor);
        int gridHeight = (int) (getHeight() / spacingFactor);
        int startX = (width - gridWidth) / 2;
        int startY = (height - gridHeight) / 2;
        int cellWidth = gridWidth / 3;
        int cellHeight = gridHeight / 3;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                dotCenters[index][0] = startX + col * cellWidth + cellWidth / 2f;
                dotCenters[index][1] = startY + row * cellHeight + cellHeight / 2f;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pattern.clear();
                linePath.reset();
                isDrawing = true;
                checkAndAddDot(touchX, touchY);
                postInvalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDrawing) {
                    checkAndAddDot(touchX, touchY);
                    postInvalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                isDrawing = false;
                if (listener != null && !pattern.isEmpty()) {
                    listener.onPatternComplete(new ArrayList<>(pattern));
                }
                postInvalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void checkAndAddDot(float x, float y) {
        for (int i = 0; i < dotCenters.length; i++) {
            float dx = dotCenters[i][0];
            float dy = dotCenters[i][1];
            float distance = (float) Math.hypot(dx - x, dy - y);
            if (distance < radius * 2.2f && !pattern.contains(i)) {  // 🔸 Touch area increased
                pattern.add(i);

                if (soundLoaded && soundPool != null) {
                    AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    float volume = maxVolume == 0 ? 0f : (float) currentVolume / maxVolume;

                    if (volume > 0f) {
                        soundPool.play(soundBeepId, volume, volume, 1, 0, 1f);
                    }
                }

                if (pattern.size() == 1) {
                    linePath.moveTo(dx, dy);
                } else {
                    linePath.lineTo(dx, dy);
                }
                break;
            }
        }
    }

    private float dpToPx(Context context, float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public interface PatternCompleteListener {
        void onPatternComplete(List<Integer> pattern);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
