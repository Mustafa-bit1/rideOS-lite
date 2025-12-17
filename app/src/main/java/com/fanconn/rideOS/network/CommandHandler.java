//package com.fanconn.rideOS.network;
//
//import android.util.Log;
//
//public class CommandHandler {
//
//    private final DashboardCommandCallback callback;
//
//    public interface DashboardCommandCallback {
//        void onCommandReceived(String commandType, int value, String clientId);
//    }
//
//    public CommandHandler(DashboardCommandCallback callback) {
//        this.callback = callback;
//    }
//
//    public void handleCommand(String commandType, int value, String clientId) {
//        Log.d("CMD_HANDLER", "Handling command: " + commandType + " = " + value);
//
//        // Pass to MainActivity (UI or UART)
//        callback.onCommandReceived(commandType, value, clientId);
//    }
//}
