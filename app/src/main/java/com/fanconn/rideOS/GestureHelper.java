package com.fanconn.rideOS;

import android.app.Activity;
import android.content.Intent;
import android.view.MotionEvent;

public class GestureHelper {

    private static final int SWIPE_THRESHOLD = 80;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final int BOTTOM_EDGE_THRESHOLD = 0; // ↓ make smaller (was 150)
    private static final int TOP_EDGE_THRESHOLD = 150;

    public static boolean handleGesture(Activity activity, MotionEvent event, float[] startYRef) {
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startYRef[0] = event.getY();
                return false;

            case MotionEvent.ACTION_UP:
                float endY = event.getY();
                float diffY = endY - startYRef[0];
                float startY = startYRef[0];

                if (Math.abs(diffY) > 40) {

                    // ✅ Swipe Top → Bottom → Quick Settings
                    if (diffY > 20 && startY <= TOP_EDGE_THRESHOLD) {
                        Intent intent = new Intent(activity, QuickSetting.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
                        return true;
                    }

                    // ✅ Swipe Bottom → Top → Main Activity (only from bottom edge)
                    else if (diffY < -40
                            && startY >= screenHeight - BOTTOM_EDGE_THRESHOLD
                            && endY <= screenHeight - (BOTTOM_EDGE_THRESHOLD + 40)) {
                        // Must start within bottom 80px and move at least 40px upward

                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activity.startActivity(intent);
                        activity.finish();
                        activity.overridePendingTransition(R.anim.zoom_in_enter, R.anim.zoom_out_exit);
                        return true;
                    }
                }

                break;
        }

        return false;
    }
}
