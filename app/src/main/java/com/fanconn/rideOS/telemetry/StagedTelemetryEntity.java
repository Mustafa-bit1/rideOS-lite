package com.fanconn.rideOS.telemetry;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "staged_telemetry")
public class StagedTelemetryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String vin;

    public String payload;

    public long timestamp;

    public StagedTelemetryEntity(
            String vin,
            String payload,
            long timestamp) {

        this.vin = vin;
        this.payload = payload;
        this.timestamp = timestamp;
    }
}