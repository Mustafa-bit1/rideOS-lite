package com.fanconn.rideOS.menu;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {RiderQueueEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class RiderQueueDatabase extends RoomDatabase {

    private static volatile RiderQueueDatabase INSTANCE;

    public abstract RiderQueueDao riderQueueDao();

    public static RiderQueueDatabase getInstance(Context context) {

        if (INSTANCE == null) {

            synchronized (RiderQueueDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            RiderQueueDatabase.class,
                            "rider_queue_database"
                    ).build();
                }
            }
        }

        return INSTANCE;
    }
}