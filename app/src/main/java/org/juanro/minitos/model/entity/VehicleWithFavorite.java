package org.juanro.minitos.model.entity;

import androidx.room.Embedded;
import androidx.room.ColumnInfo;

import java.io.Serializable;

public class VehicleWithFavorite implements Serializable {
    @Embedded
    private Vehicle vehicle;

    @ColumnInfo(name = "is_favorite")
    private boolean favorite;

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
