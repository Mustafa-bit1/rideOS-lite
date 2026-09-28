package com.fanconn.rideOS.menu;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RiderQueueDao {

    @Insert
    long insert(RiderQueueEntity entity);

    @Query("SELECT * FROM rider_sync_queue ORDER BY id ASC")
    List<RiderQueueEntity> getAll();

    @Query("DELETE FROM rider_sync_queue WHERE id = :id")
    void delete(long id);

    @Query("SELECT COUNT(*) FROM rider_sync_queue")
    int count();
}