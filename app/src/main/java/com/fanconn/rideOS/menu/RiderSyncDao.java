package com.fanconn.rideOS.menu;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RiderSyncDao {

    @Insert
    void insert(RiderSync data);

    @Query("SELECT * FROM rider_sync_queue ORDER BY id ASC LIMIT :limit")
    List<RiderSync> getBatch(int limit);

    @Query("DELETE FROM rider_sync_queue WHERE id IN (:ids)")
    void deleteByIds(List<Long> ids);

    @Query("SELECT COUNT(*) FROM rider_sync_queue")
    int count();
}