package com.fanconn.rideOS.telemetry;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                StagedTelemetryEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class TelemetryDatabase extends RoomDatabase {

    private static volatile TelemetryDatabase INSTANCE;

    public abstract StagedTelemetryDao stagedTelemetryDao();

    public static TelemetryDatabase getInstance(Context context) {

        if (INSTANCE == null) {

            synchronized (TelemetryDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    TelemetryDatabase.class,
                                    "telemetry_database"
                            )
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}