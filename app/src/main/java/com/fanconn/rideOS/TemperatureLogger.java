package com.fanconn.rideOS;

import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;

public class TemperatureLogger {

    private static final String TAG = "TemperatureLogger";
    private static final String LOG_FILE_PATH = "/sdcard/rideos_temp_log.txt";

    // Map of sensor name → description
    private static final HashMap<String, String> importantSensors = new HashMap<>();
    static {
        importantSensors.put("s3-die-temp-usr", "SoC die temperature (hottest chip zone)");
        importantSensors.put("cpuss-1-usr", "CPU Core Cluster 1");
        importantSensors.put("cpuss-1-step", "CPU Core Cluster 1 (step)");
        importantSensors.put("cpuss-0-usr", "CPU Core Cluster 0");
        importantSensors.put("cpuss-0-step", "CPU Core Cluster 0 (step)");
        importantSensors.put("soc", "System-on-Chip (motherboard-level)");
        importantSensors.put("msm-therm-usr", "Main chipset (e.g., Snapdragon)");
        importantSensors.put("gpu-usr", "GPU Temperature");
        importantSensors.put("battery", "Battery Temperature");
        importantSensors.put("bms", "Battery Management System (BMS)");
    }

    public static void logImportantThermals() {
        File thermalDir = new File("/sys/class/thermal");
        File[] files = thermalDir.listFiles();

        if (files == null) {
            logBoth("No thermal zone files found");
            return;
        }

        for (File file : files) {
            if (file.getName().startsWith("thermal_zone")) {
                String typePath = file.getAbsolutePath() + "/type";
                String tempPath = file.getAbsolutePath() + "/temp";

                String type = readFile(typePath);
                String tempStr = readFile(tempPath);

                if (type != null && tempStr != null && importantSensors.containsKey(type.trim())) {
                    try {
                        float temp = Float.parseFloat(tempStr.trim());
                        if (temp > 1000) temp /= 1000; // Convert from millidegree if needed
                        String description = importantSensors.get(type.trim());
                        String log = String.format(Locale.US,
                                "%s : %.2f°C (%s)", type.trim(), temp, description);
                        logBoth(log);
                    } catch (NumberFormatException e) {
                        logBoth("Invalid temp in " + tempPath);
                    }
                }
            }
        }
    }

    private static String readFile(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            reader.close();
            return line;
        } catch (IOException e) {
            logBoth("Error reading: " + path);
            return null;
        }
    }

    private static void logBoth(String message) {
        String timeStampedMsg = "[" + getCurrentTimestamp() + "] " + message;
        Log.d(TAG, timeStampedMsg);

        try {
            FileWriter writer = new FileWriter(LOG_FILE_PATH, true);
            writer.append(timeStampedMsg).append("\n");
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }

    private static String getCurrentTimestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
