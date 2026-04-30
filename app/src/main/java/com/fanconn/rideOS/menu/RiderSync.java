package com.fanconn.rideOS.menu;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "rider_sync_queue")
public class RiderSync {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String operation; // ADD / UPDATE / DELETE

    public String payload;   // JSON

    public long timestamp;
}
