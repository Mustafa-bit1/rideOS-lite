package com.fanconn.rideOS;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {RealTimeData.class}, version = 1)
public abstract class TelemetryDatabase extends RoomDatabase {

    private static TelemetryDatabase instance;

    public abstract TelemetryDao telemetryDao();

    public static synchronized TelemetryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    TelemetryDatabase.class,
                    "telemetry_db"
            ).build();
        }
        return instance;
    }
}