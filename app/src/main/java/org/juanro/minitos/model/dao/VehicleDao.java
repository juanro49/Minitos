package org.juanro.minitos.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import org.juanro.minitos.model.entity.Vehicle;
import org.juanro.minitos.model.entity.FavoriteVehicle;
import org.juanro.minitos.model.entity.VehicleWithFavorite;

@Dao
public interface VehicleDao {
    @Query("SELECT v.*, (f.id IS NOT NULL) as is_favorite FROM vehicles v LEFT JOIN fav_vehicles f ON v.id = f.id ORDER BY v.name ASC")
    LiveData<List<VehicleWithFavorite>> getAllVehiclesWithFavorite();

    @Query("SELECT v.*, (f.id IS NOT NULL) as is_favorite FROM vehicles v LEFT JOIN fav_vehicles f ON v.id = f.id ORDER BY v.name ASC")
    List<VehicleWithFavorite> getAllVehiclesWithFavoriteSync();

    @Query("SELECT v.*, (f.id IS NOT NULL) as is_favorite FROM vehicles v LEFT JOIN fav_vehicles f ON v.id = f.id WHERE v.id = :id")
    LiveData<VehicleWithFavorite> getVehicleWithFavoriteById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVehicles(List<Vehicle> vehicles);

    @Query("DELETE FROM vehicles")
    void deleteAllVehicles();

    @Transaction
    default void updateVehicles(List<Vehicle> vehicles) {
        deleteAllVehicles();
        insertVehicles(vehicles);
    }

    // Favorites
    @Query("SELECT v.*, 1 as is_favorite FROM vehicles v INNER JOIN fav_vehicles f ON v.id = f.id ORDER BY v.name ASC")
    LiveData<List<VehicleWithFavorite>> getFavoriteVehicles();

    @Query("SELECT EXISTS(SELECT 1 FROM fav_vehicles WHERE id = :id)")
    LiveData<Boolean> isFavorite(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFavorite(FavoriteVehicle favorite);

    @Query("DELETE FROM fav_vehicles WHERE id = :id")
    void removeFavorite(String id);
}
