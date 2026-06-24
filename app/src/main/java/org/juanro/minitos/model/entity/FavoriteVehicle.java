package org.juanro.minitos.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fav_vehicles")
public class FavoriteVehicle {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    public FavoriteVehicle(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }
}
