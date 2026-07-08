package com.fanconn.rideOS;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class batteryPackSwitcher {

    private final SerialPortManager serialPortManager;
    private final Handler handler;
    private final int MAX_RETRIES = 13;
    private final int MAX_RETRIES2 = 10;
    private int retryCount1 = 0;
    private int retryCount2 = 0;
    private int retryCount3 = 0;
    private boolean isSequenceActive = false;
    private TextView statusTextView; // remove final

    public batteryPackSwitcher() {
        serialPortManager = serialApplication.getSerialPortManager();
        this.handler = new Handler(Looper.getMainLooper());
        this.statusTextView = null; // explicitly set to null
    }

    //  Overloaded constructor that also initializes the superclass fields
    public batteryPackSwitcher(TextView statusTextView) {
        this(); //  Calls the no-arg constructor to initialize serialPortManager and handler
        this.statusTextView = statusTextView;
    }
    //  Safe method to update text
    private void updateStatusText(String message) {
        if (statusTextView != null) {
            statusTextView.setText(message);
        }
    }
    public void startSwitchingToAPU() {
        isSequenceActive = true;
        retryCount1 = 0;
        retryCount2 = 0;
        if (statusTextView != null) {
            statusTextView.setVisibility(View.VISIBLE);
        }
        updateStatusText("Switching to rescue pack, Please wait...");
        mainChargeOFF();
    }

    private void apuDisON() {
        if (!isSequenceActive) return;

        serialPortManager.VCUSendToMCU("FFAA5501030015162C02800101020304050607");
        //Log.d("CommandSequence", "APU Dis On cmd sent");

        handler.postDelayed(() -> {
            Heartbeat3 hb3 = serialPortManager.getHb3();
            if (hb3 != null) {
                //Log.d("CommandSequence", "APU DIs MosState " + hb3.getApuDisChargeMosState());
            }

            boolean commandSuccess = (hb3 != null && hb3.getApuDisChargeMosState() == 1);

            if (!commandSuccess && retryCount2 < MAX_RETRIES) {
                retryCount2++;
//                Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount2);
                apuDisON();
            } else if (commandSuccess) {
                // Check again if main charge is really off
                boolean mainChargeStillOn = (hb3 != null && hb3.getMainChargeMosState() != 0);
                //Log.d("CommandSequence", "This-B------------"+hb3.getMainChargeMosState());
                updateStatusText("Successfully Switched To rescue pack");
                UIUpdater.apuflag=true;
                if(serialPortManager!=null){
                    serialPortManager.VCUSendToMCU("FFAA550103001510FBA80728007800B004A000");//line current 10 A
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            serialPortManager.VCUSendToMCU("FFAA550103001510FBA80728007800B004A000"), 1000);
//                    new Handler(Looper.getMainLooper()).postDelayed(() ->
//                            serialPortManager.VCUSendToMCU("FFAA550103001510F8A807040001020E007387"), 2000);
                }

                //Log.i("CommandSequence", "Switched TO APU succeeded");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (statusTextView != null) {
                        statusTextView.setVisibility(View.INVISIBLE);
                    }
                }, 3000);
                isSequenceActive = false;

//                if (mainChargeStillOn) {
//                    retryCount1 = 0;
//                    doMainchargeOffAgain();
//                } else {
//                    updateStatusText("Successfully Switched To APU");
//                    Log.i("CommandSequence", "Switched TO APU succeeded");
//                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                        if (statusTextView != null) {
//                            statusTextView.setVisibility(View.INVISIBLE);
//                        }
//                    }, 3000);
//                    isSequenceActive = false;
//                }
            } else {
                //Log.e("CommandSequence", "APU Dis ON failed after max retries");
                isSequenceActive = false;
                if(statusTextView!=null){
                statusTextView.setVisibility(View.INVISIBLE);}
            }
        }, 800);
    }

    private void apuChargeON() {
        if (!isSequenceActive) return;

        serialPortManager.VCUSendToMCU("FFAA5501030015162A02800101020304050607");
        //Log.d("CommandSequence", "APU Dis On cmd sent");

        handler.postDelayed(() -> {
            Heartbeat3 hb3 = serialPortManager.getHb3(); // ✅ Fetch fresh state now
            if (hb3 != null) {
                //Log.d("CommandSequence", "APU DIs MosState " + hb3.getApuDisChargeMosState());
            }

            boolean commandSuccess = (hb3 != null && hb3.getApuChargeMosState() == 1);

            if (!commandSuccess && retryCount3 < MAX_RETRIES) {
                retryCount3++;
//                Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount2);
                apuChargeON();
            } else if (commandSuccess) {
                // Check again if main charge is really off
                boolean mainChargeStillOn = (hb3 != null && hb3.getMainChargeMosState() != 0);
                //Log.d("CommandSequence", "This-B------------"+hb3.getMainChargeMosState());
                apuDisON();

//                if (mainChargeStillOn) {
//                    retryCount1 = 0;
//                    doMainchargeOffAgain();
//                } else {
//                    isSequenceActive = false;
//                }
            } else {
                //Log.e("CommandSequence", "APU Dis ON failed after max retries");
                isSequenceActive = false;
                if(statusTextView!=null){
                    statusTextView.setVisibility(View.INVISIBLE);}
            }
        }, 800);
    }

    private void mainChargeOFF() {
        if (!isSequenceActive) return;

        serialPortManager.VCUSendToMCU("FFAA5501030015162A01800001020304050607");
        //Log.d("CommandSequence", "MainCharge oFF sent");

        handler.postDelayed(() -> {
            Heartbeat3 hb3 = serialPortManager.getHb3(); // Fetch latest heartbeat now
            if (hb3 != null) {
                //Log.d("CommandSequence", "MainChargeState " + hb3.getMainChargeMosState());
            }

            boolean commandSuccess = (hb3 != null && hb3.getMainChargeMosState() == 0);

            if (!commandSuccess && retryCount1 < MAX_RETRIES2) {
                retryCount1++;
                //Log.d("CommandSequence", "Retrying command 2, attempt " + retryCount1);
                mainChargeOFF(); // retry
            } else if (commandSuccess) {
                //Log.i("APU", "CMD1 Success");
                //Log.d("CommandSequence", "This-D------------"+hb3.getMainChargeMosState());
                apuChargeON(); // proceed to APU chr ON
            } else {
                //Log.e("CommandSequence", "Command 2 failed after max retries");
                //apuDisON(); // proceed anyway with backup check
                //Log.d("CommandSequence", "This-A------------"+hb3.getMainChargeMosState());
                updateStatusText("Switching failed, Main charge still ON");
                //Log.i("CommandSequence", "Final Main Charge OFF confirmed");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (statusTextView != null) {
                        statusTextView.setVisibility(View.INVISIBLE);
                    }
                }, 5000);
            }
        }, 800);
    }
    public void doMainchargeOffAgain() {
        serialPortManager.VCUSendToMCU("FFAA5501030015162A01800001020304050607");

        handler.postDelayed(() -> {
            Heartbeat3 hb3 = serialPortManager.getHb3(); // ✅ Fresh check
            Log.d("dsafsg", "Retry check for MainCharge state: " + (hb3 != null ? hb3.getMainChargeMosState() : -1));
            //Log.d("CommandSequence", "This-C------------"+hb3.getMainChargeMosState());

            boolean commandSuccess = (hb3 != null && hb3.getMainChargeMosState() == 0);

            if (!commandSuccess && retryCount1 < MAX_RETRIES2) {
                retryCount1++;
                //Log.d("CommandSequence", "Retrying Main Charge OFF again, attempt " + retryCount1);
                doMainchargeOffAgain();
            } else if (commandSuccess) {
                updateStatusText("Successfully Switched To APU");
                //Log.i("CommandSequence", "Final Main Charge OFF confirmed");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (statusTextView != null) {
                        statusTextView.setVisibility(View.INVISIBLE);
                    }
                }, 3000);
            } else {
                if (statusTextView != null) {
                    statusTextView.setVisibility(View.INVISIBLE);
                }            }
        }, 800);
    }


    //--------------------------------------------------------------
    public void startSwitchingToNormalState() {
        isSequenceActive = true;
        retryCount1 = 0;
        retryCount2 = 0;
        apuDisOFF();
    }

    private void apuDisOFF() {
        if (!isSequenceActive) return;

        serialPortManager.VCUSendToMCU("FFAA5501030015162C02800001020304050607");
        //Log.d("CommandSequence", "ApU DIS OFF  command Sent ");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Heartbeat3 hb3 = serialPortManager.getHb3();
            //Log.d("CommandSequence", "APU Dis State "+hb3.getApuDisChargeMosState());

            boolean commandSuccess = (hb3 != null && hb3.getApuDisChargeMosState() == 0);

            if (!commandSuccess && retryCount1 < MAX_RETRIES) {
                retryCount1++;
                //Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount1);
                apuDisOFF(); // retry
            } else if (commandSuccess) {
                //Log.i("CommandSequence", "Command 1 succeeded");
                mainChargeON();
            } else {
                //Log.e("CommandSequence", "Command 1 failed after max retries");
                mainChargeON();
            }
        }, 800); // adjust delay as needed
    }

    private void mainChargeON() {
        if (!isSequenceActive) return;

        serialPortManager.VCUSendToMCU("FFAA5501030015162A01800101020304050607");
        //Log.d("CommandSequence", "main CHarge ON cmd sent");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Heartbeat3 hb3 = serialPortManager.getHb3();
            //Log.d("CommandSequence", "MainChargeState"+hb3.getMainChargeMosState());

            boolean commandSuccess = (hb3 != null && hb3.getMainChargeMosState() == 1);

            if (!commandSuccess && retryCount2 < MAX_RETRIES) {
                retryCount2++;
                //Log.d("CommandSequence", "Retrying command 1, attempt " + retryCount2);
                mainChargeON(); // retry
            } else if (commandSuccess) {

                //Log.i("APU", "Switched to Main successfully");

                isSequenceActive = false;
            } else {
                //Log.e("CommandSequence", "Command 2 failed after max retries");
                isSequenceActive = false;
                //isSequenceActive = false;
            }
        }, 800); // adjust delay as needed
    }

}
