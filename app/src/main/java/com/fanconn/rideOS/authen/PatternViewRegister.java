package com.fanconn.rideOS.authen;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fanconn.rideOS.R;

import java.util.ArrayList;
import java.util.List;

public class PatternViewRegister extends View {

    private Paint circlePaint, linePaint;
    private float[][] dotCenters;
    private List<Integer> pattern = new ArrayList<>();
    private boolean isDrawing = false;
    private Path linePath = new Path();
    private PatternCompleteListener listener;
    private float radius;
    private int padding;

    public PatternViewRegister(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setOnPatternCompleteListener(PatternCompleteListener listener) {
        this.listener = listener;
    }

    private void init(Context context) {
        setBackgroundColor(Color.parseColor("#000000")); // fallback dark background

        // Convert 50dp to px
        radius = dpToPx(context, 19); // ~48dp diameter
        padding = (int) dpToPx(context, 32);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.WHITE); // Visible on dark background
        circlePaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#FF953E"));
        linePaint.setStrokeWidth(dpToPx(context, 10));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (dotCenters == null) calculateDotCenters();

        // Draw all the dots
        for (float[] center : dotCenters) {
            canvas.drawCircle(center[0], center[1], radius, circlePaint);
        }

        // Draw the connecting path
        canvas.drawPath(linePath, linePaint);
    }

    private void calculateDotCenters() {
        dotCenters = new float[9][2];
        int width = getWidth();
        int height = getHeight();

        // Adjust grid placement dynamically based on screen size
        float spacingFactor = 1f; // increase this to space out the dots more
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
                linePath.reset();  // This will reset the Path
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
                if (pattern.size() < 4) {
                    // Show a message if the user draws fewer than 4 dots
                    showCustomToast("Connect atleast 4 dots");
                    //Toast.makeText(getContext(), "Connect atleast 4 dots", Toast.LENGTH_SHORT).show();
                    // Clear the pattern
                    pattern.clear();
                    linePath.reset();
                } else {
                    // If the pattern is complete, notify the listener
                    if (listener != null && !pattern.isEmpty()) {
                        listener.onPatternComplete(new ArrayList<>(pattern));
                    }
                }
                postInvalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.toast_custom, null);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_text);
        icon.setImageResource(R.drawable.toastlogo);
        text.setText(message);

        Toast toast = new Toast(getContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void checkAndAddDot(float x, float y) {
        for (int i = 0; i < dotCenters.length; i++) {
            float dx = dotCenters[i][0];
            float dy = dotCenters[i][1];
            float distance = (float) Math.hypot(dx - x, dy - y);
            if (distance < radius && !pattern.contains(i)) {
                pattern.add(i);
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

    public void clearPattern() {
        if (pattern != null) {
            pattern.clear();
        }
        linePath.reset(); // Reset the line path when clearing the pattern
        invalidate(); // Force redraw of the view
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDotCenters(); // Recalculate dot positions if the view size changes
    }
}
