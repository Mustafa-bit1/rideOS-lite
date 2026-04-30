package com.fanconn.rideOS;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.fanconn.rideOS.menu.RiderSync;
import com.fanconn.rideOS.menu.RiderSyncDao;

@Database(
        entities = {
                RealTimeData.class,
                RiderSync.class
        },
        version = 2,
        exportSchema = false
)
public abstract class TelemetryDatabase extends RoomDatabase {

    private static volatile TelemetryDatabase instance;

    public abstract TelemetryDao telemetryDao();
    public abstract RiderSyncDao riderSyncDao();

    public static synchronized TelemetryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            TelemetryDatabase.class,
                            "telemetry_db"
                    )
                    // ✅ IMPORTANT (avoids crash due to version change)
                    .fallbackToDestructiveMigration()

                    // (Optional but recommended)
                    .allowMainThreadQueries() // remove later in production

                    .build();
        }
        return instance;
    }
}