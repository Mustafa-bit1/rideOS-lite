package com.fanconn.rideOS;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.fanconn.rideOS.authen.LoginActivity;
import com.fanconn.rideOS.menu.menu;

import java.lang.reflect.Method;

public class InactivityHandler implements InactivityManager.InactivityCallback {
    private final Context context;
    private static final String TAG = "TOUCHTIMER";
    public InactivityHandler(Context context) {
        this.context = context.getApplicationContext(); // To avoid leaks
        InactivityManager.getInstance().setCallback(this); // Set this class as the callback
    }

    @Override
    public void onInactivityTimeout(int code) {
//        Log.d(TAG, "SSSSSSSSSSSSSSSSSSSS Timeout triggered with code: " + code);

        switch (code) {
            case 1:
               // Log.d("sadfgg","********2");
              //  Log.d("MainActivity", "Case......1");
//
////                serialPortManager.VCUSendToMCU("FFAA550101000A00");
//                Log.d("MainActivity", "Lock command sent to MCU.");
//
//                // Delay navigation to ensure serial command is sent before activity finishes
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
////                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
////                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
////                    startActivity(intent);
//                    finish();
//                }, 200); // small delay (200ms)
//                break;
//                if (! LoginActivity.isSequenceActive) {
//                    LoginActivity.isSequenceActive = true; // Start sequence
//                    serialPortManager.getHb3().setLEDresponse(1);
//                    serialPortManager.getHb3().sethandleLockresponse(1);
                int rpm = SerialPortManager.getInstance().getHb3().getBSG2_BYTE78();
              //  Log.d(TAG,"code 1 TOUCHTIMER GEAR  "+UIUpdater.directionalGears);
              //  Log.d(TAG,"code 1 TOUCHTIMER STAND  "+UIUpdater.getInstance().isStand);

                if(rpm==0 && (UIUpdater.directionalGears==0 || UIUpdater.getInstance().isStand)){
               //     Log.d("TOUCHTIMER","IF OF code 1 TOUCHTIMER IFTIMER");
                    EVLockService.getInstance().startLockSequence();    // Start lock
                    Context context2 = MainActivity.instance;
                    if (context2 != null) {
                        Intent intent = new Intent(context2, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context2.startActivity(intent);
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                            Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);

                      //      Log.d("PowerAction", "Calling goToSleep()");
                            goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
                      //      Log.d("PowerAction", "goToSleep() invoked.");
                        } catch (Exception e) {
                      //      Log.e("PowerAction", "Failed to initiate goToSleep:", e);
                        }
                    }, 1000); // Wait 1 seconds after lock before sleeping
                }
                else {
                 //   Log.d("TOUCHTIMER","ELSEOF TOUCHTIMER TIMER");
                }

                //below is b4 7/7
//                if (MainActivity.instance != null) {
//                    MainActivity.instance.finish();
//                    Log.d("InactivityHandler", "MainActivity finished.");
//                } else {
//                    Log.d("InactivityHandler", "MainActivity was already null.");
//                }
                //----------------
//                } else {
//                    Log.d("Already", "Already running a sequence");
//                }
                break;
            case 2:Log.d("MainActivity", "Case......2");
//                PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
//                if (powerManager != null) {
//                    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
//                            PowerManager.FULL_WAKE_LOCK |
//                                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
//                                    PowerManager.ON_AFTER_RELEASE,
//                            "MyApp::ScreenOnWakeLock"
//                    );
//                    wakeLock.acquire(3000); // Keep screen on for 3 seconds
//                }
//                if (!isSequenceActive) {
//                    isSequenceActive = true; // Start sequence
//                    serialPortManager.getHb3().setLEDresponse(0);
//                    serialPortManager.getHb3().sethandleLockresponse(0);
//                    EVlockSequence(); // This will run task1 → task2
//                } else {
//                    Log.d("Already", "Already running a sequence");
//                }
              //  Log.d("SHIFT", "DRIVE - NEUTRAL");
                break;

            case 3: //  Log.d("TOUCHTIMER", "Timeout in login screen. Turning screen off...");
               // Log.d("TOUCHTIMER", "Case......3");
                try {
                    LoginActivity.markScreenJustOff();
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
                    goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//                InactivityManager.getInstance().startTimer(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

//            case 4://from charging to locking
//               // Log.d("InactivityTimeout", "SSSSSSSSSSSSSSSSSSSS: Starting lock and screen-off sequence.");
//               // Log.d("MainActivity", "Case......4");
//                // Start EV lock
//                EVLockService.getInstance().startLockSequence();
//
////                Context context3 = MainActivity.instance;
////                if (context3 != null) {
////                    Intent intent = new Intent(context, LoginActivity.class);
////                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//////  DO NOT use CLEAR_TOP here, or ChargingUI might be cleared
////                    context.startActivity(intent);
////                }
//
//                //below is b47/7
////                if (MainActivity.instance != null) {
////                    MainActivity.instance.finish();
////                }
//                //-----------------
//                // Delay screen off slightly to allow lock to complete
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    try {
//                        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//                        Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
//
//                      //  Log.d("PowerAction", "Calling goToSleep()");
//                        goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//                       // Log.d("PowerAction", "goToSleep() invoked.");
//                    } catch (Exception e) {
//                       // Log.e("PowerAction", "Failed to initiate goToSleep:", e);
//                    }
//                }, 1000); // Wait 1 seconds after lock before sleeping
//                break;

            case 4:
                EVLockService.getInstance().startLockSequence();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    boolean isLoginActive = (LoginActivity.getInstance() != null);

                    if (!isLoginActive) {
                        //  Case A → Login NOT in stack

                        // Close Charging UI
                        if (chargingUI.instance != null) {
                            chargingUI.instance.finish();
                        }

                        // Launch Login
                        Context ctx = MainActivity.instance;
                        if (ctx != null) {
                            Intent intent = new Intent(ctx, LoginActivity.class);
                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                            );
                            ctx.startActivity(intent);
                        }

                        // Turn screen OFF (after delay)
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            turnScreenOff(context);
                        }, 300);

                    } else {
                        //  Case B → Login already exists

                        //  DO NOT close charging UI
                        //  DO NOT launch login again

                        // Just turn OFF screen
                        turnScreenOff(context);
                    }

                }, 2000);

                break;

            case 5:
              //  Log.d("InactivityHandler", "Case......5: Locking and opening LoginActivity from settings");
                // Lock the EV
                EVLockService.getInstance().startLockSequence();
                // Launch LoginActivity
                Intent loginIntent = new Intent(context, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // Required since context is app-level
                context.startActivity(loginIntent);

                // Finish menu activity if active
                if (menu.setinstance != null) {
                    menu.setinstance.finish();
                    menu.setinstance = null; // Optional, prevent leaks
                }
                break;
            case 7:
                //Log.d("MainActivity", "After 15 seconds of locking EV Now Screen is Getting OFFED");
              //  Log.d("MainActivity", "Case......7");
                try {
                    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
                    goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
//                InactivityManager.getInstance().startTimer(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:
               // Log.d("InactivityHandler", "Unhandled timeout code: " + code);
                break;
        }
    }
    private void turnScreenOff(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            Method goToSleep = PowerManager.class.getMethod("goToSleep", long.class);
            goToSleep.invoke(powerManager, SystemClock.uptimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
