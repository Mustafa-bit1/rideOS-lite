package com.fanconn.rideOS.menu;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "rider_sync_queue")
public class RiderQueueEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String operation;

    public String payload;

    public long createdAt;
}