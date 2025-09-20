package com.fanconn.rideOS;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;

import com.fanconn.rideOS.MainActivity;
import com.fanconn.rideOS.QuickSetting;

public class GestureHelper {

    private static final int SWIPE_THRESHOLD = 150;
    private static final int SWIPE_VELOCITY_THRESHOLD = 150;
    private static final int BOTTOM_EDGE_THRESHOLD = 180;
    private static final int TOP_EDGE_THRESHOLD = 180;

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

                //Log.d("GestureHelper", "startY=" + startY + " | endY=" + endY + " | diffY=" + diffY);

                if (Math.abs(diffY) > SWIPE_THRESHOLD) {

                    // ✅ Swipe Top → Bottom → from TOP edge only
                    if (diffY > 0 && startY <= TOP_EDGE_THRESHOLD) {
                        //Log.d("GestureHelper", "Swipe ↓ from top edge → QuickSettings");
                        Intent intent = new Intent(activity, QuickSetting.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(0, 0);
                        return true;
                    }

                    // ✅ Swipe Bottom → Top → from BOTTOM edge only
                    else if (diffY < 0 && startY >= screenHeight - BOTTOM_EDGE_THRESHOLD && endY < screenHeight - (BOTTOM_EDGE_THRESHOLD * 2)) {
                        //Log.d("GestureHelper", "Swipe ↑ from bottom edge → MainActivity");
                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activity.startActivity(intent);
                        activity.finish();
                        activity.overridePendingTransition(0, 0);
                        return true;
                    }

                    // Ignored: started from wrong area
                    else {
                        ///Log.d("GestureHelper", "Swipe ignored — not from correct edge region");
                    }
                }

                break;
        }

        return false;
    }
}
