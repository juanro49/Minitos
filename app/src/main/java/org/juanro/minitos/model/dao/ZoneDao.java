package org.juanro.minitos.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.juanro.minitos.model.entity.Zone;

@Dao
public interface ZoneDao {
    @Query("SELECT * FROM zones")
    LiveData<List<Zone>> getAllZones();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertZones(List<Zone> zones);

    @Query("DELETE FROM zones")
    void deleteAllZones();

    @Transaction
    default void updateZones(List<Zone> zones) {
        deleteAllZones();
        insertZones(zones);
    }
}
