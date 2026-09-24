package com.fanconn.rideOS.telemetry;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StagedTelemetryDao {

    @Insert
    long insert(StagedTelemetryEntity entity);

    @Query("""
            SELECT *
            FROM staged_telemetry
            ORDER BY id ASC
            LIMIT :limit
            """)
    List<StagedTelemetryEntity> getOldest(int limit);

    @Delete
    void delete(StagedTelemetryEntity entity);

    @Query("DELETE FROM staged_telemetry WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM staged_telemetry")
    int getCount();

    @Query("""
            DELETE FROM staged_telemetry
            WHERE id IN (
                SELECT id
                FROM staged_telemetry
                ORDER BY id ASC
                LIMIT :count
            )
            """)
    void deleteOldest(int count);

    @Query("""
            SELECT id
            FROM staged_telemetry
            ORDER BY id ASC
            LIMIT 1
            """)
    Long getOldestId();
}