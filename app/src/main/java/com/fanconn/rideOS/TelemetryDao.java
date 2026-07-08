package com.fanconn.rideOS;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TelemetryDao {

    @Insert
    void insert(RealTimeData data);

    @Query("SELECT * FROM telemetry_queue ORDER BY id ASC LIMIT :limit")
    List<RealTimeData> getBatch(int limit);

    @Query("DELETE FROM telemetry_queue WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);

    @Query("SELECT COUNT(*) FROM telemetry_queue")
    int count();

    @Query("DELETE FROM telemetry_queue WHERE id NOT IN (SELECT id FROM telemetry_queue ORDER BY id DESC LIMIT 86400)")
    void keepLast24Hours();

    @Query("DELETE FROM telemetry_queue")
    void deleteAll();
}
