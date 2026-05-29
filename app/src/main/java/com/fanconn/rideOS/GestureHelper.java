package com.fanconn.rideOS;

import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

public class GestureHelper {

    // Swipe must START from bottom area
    private static final int EDGE_ZONE_DP = 22;

    // Required upward swipe distance
    private static final int MIN_SWIPE_DISTANCE_DP = 180;

    // Reject diagonal / horizontal swipes
    private static final int MAX_HORIZONTAL_MOVEMENT_DP = 70;

    // Swipe must be fast
    private static final long MAX_GESTURE_TIME = 300;

    private static boolean gestureValid = false;

    private static float startX;
    private static float startY;

    private static long downTime;

    public static boolean handleGesture(Activity activity, MotionEvent event) {

        // REAL screen metrics
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);

        float density = dm.density;

        int screenHeight = dm.heightPixels;

        float EDGE_ZONE = EDGE_ZONE_DP * density;
        float MIN_SWIPE_DISTANCE = MIN_SWIPE_DISTANCE_DP * density;
        float MAX_HORIZONTAL_MOVEMENT = MAX_HORIZONTAL_MOVEMENT_DP * density;

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {

                startX = event.getRawX();
                startY = event.getRawY();

                downTime = System.currentTimeMillis();

                // Distance from bottom edge
                float distanceFromBottom = screenHeight - startY;

                // Gesture valid ONLY if touch starts from bottom zone
                gestureValid =
                        distanceFromBottom >= 0 &&
                                distanceFromBottom <= EDGE_ZONE;

                return false;
            }

            case MotionEvent.ACTION_MOVE: {

                if (!gestureValid) {
                    return false;
                }

                float currentX = event.getRawX();

                float diffX = Math.abs(currentX - startX);

                // Reject if finger moves too much sideways
                if (diffX > MAX_HORIZONTAL_MOVEMENT) {
                    gestureValid = false;
                }

                return false;
            }

            case MotionEvent.ACTION_UP: {

                if (!gestureValid) {
                    reset();
                    return false;
                }

                float endY = event.getRawY();

                // Upward swipe distance
                float diffY = startY - endY;

                // Gesture duration
                long gestureDuration =
                        System.currentTimeMillis() - downTime;

                // FINAL CHECK
                if (diffY > MIN_SWIPE_DISTANCE &&
                        gestureDuration < MAX_GESTURE_TIME) {

                    Intent intent =
                            new Intent(activity, MainActivity.class);

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );

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
                return false;
            }

            case MotionEvent.ACTION_CANCEL: {
                reset();
                break;
            }
        }

        return false;
    }

    private static void reset() {
        gestureValid = false;
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
