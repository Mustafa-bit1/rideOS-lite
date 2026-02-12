package com.fanconn.rideOS;

import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;

public class GestureHelper {

    private static final String TAG = "EDGE_GESTURE";

    private static final int EDGE_SWIPE_THRESHOLD = 80;    // bottom 80px only
    private static final int MIN_SWIPE_DISTANCE = 160;     // strong swipe

    private static boolean gestureEnabledForThisTouch = false;
    private static float startRawY = -1;

    public static boolean handleGesture(Activity activity, MotionEvent event, float[] dummy) {

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenHeight = dm.heightPixels;

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {

                float rawY = event.getRawY();
                startRawY = rawY;

                // ✅ DISTANCE FROM BOTTOM (correct logic)
                float distanceFromBottom = screenHeight - rawY;

                boolean inBottomEdge = distanceFromBottom <= EDGE_SWIPE_THRESHOLD;

                Log.e(TAG, "DOWN rawY=" + rawY +
                        " screenH=" + screenHeight +
                        " distFromBottom=" + distanceFromBottom +
                        " inEdge=" + inBottomEdge);

                // 🔒 HARD GATE
                gestureEnabledForThisTouch = inBottomEdge;

                return false;
            }

            case MotionEvent.ACTION_MOVE:
                // ❌ if not enabled at DOWN → ignore forever
                return false;

            case MotionEvent.ACTION_UP: {

                if (!gestureEnabledForThisTouch) {
                    reset();
                    return false;
                }

                float endRawY = event.getRawY();
                float diffY = endRawY - startRawY;

                Log.e(TAG, "UP rawY=" + endRawY +
                        " diffY=" + diffY +
                        " enabled=" + gestureEnabledForThisTouch);

                // ✅ Only bottom → top swipe
                if (diffY < -MIN_SWIPE_DISTANCE) {

                    Log.e(TAG, "🔥 EDGE HOME GESTURE");

                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                    activity.finish();
                    activity.overridePendingTransition(
                            R.anim.zoom_in_enter,
                            R.anim.zoom_out_exit
                    );

                    reset();
                    return true;
                }

                reset();
                break;
            }

            case MotionEvent.ACTION_CANCEL:
                reset();
                break;
        }

        return false;
    }

    private static void reset() {
        gestureEnabledForThisTouch = false;
        startRawY = -1;
    }
}





//package com.fanconn.rideOS;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.view.MotionEvent;
//
//public class GestureHelper {
//
//
//    private static final int SWIPE_THRESHOLD = 80;
//    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
////    private static final int BOTTOM_EDGE_THRESHOLD = 0; // ↓ make smaller (was 150)
//    private static final int TOP_EDGE_THRESHOLD = 150;
//    private static final int BOTTOM_EDGE_THRESHOLD = 120; // bottom 120px only
//
//    public static boolean handleGesture(Activity activity, MotionEvent event, float[] startYRef) {
//        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                startYRef[0] = event.getY();
//                return false;
//
//            case MotionEvent.ACTION_UP:
//                float endY = event.getY();
//                float diffY = endY - startYRef[0];
//                float startY = startYRef[0];
//
//                if (Math.abs(diffY) > 40) {
//
//                    // ✅ Swipe Top → Bottom → Quick Settings
////                    if (diffY > 20 && startY <= TOP_EDGE_THRESHOLD) {
////                        Intent intent = new Intent(activity, QuickSetting.class);
////                        activity.startActivity(intent);
////                        activity.overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_up);
////                        return true;
////                    }
//
//                    // ✅ Swipe Bottom → Top → Main Activity (only from bottom edge)
//                    // Swipe Bottom → Top → Main Activity (only from bottom edge)
//                    if (diffY < -80
//                            && startY >= screenHeight - BOTTOM_EDGE_THRESHOLD
//                            && Math.abs(diffY) > 100) {
//
//                        Intent intent = new Intent(activity, MainActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                        activity.startActivity(intent);
//                        activity.finish();
//                        activity.overridePendingTransition(R.anim.zoom_in_enter, R.anim.zoom_out_exit);
//                        return true;
//                    }
//
//                }
//
//                break;
//        }
//
//        return false;
//    }
//}
