package org.juanro.minitos.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.juanro.minitos.model.MinitosDatabase;
import org.juanro.minitos.model.entity.FavoriteVehicle;
import org.juanro.minitos.model.entity.VehicleWithFavorite;
import org.juanro.minitos.model.entity.Zone;
import org.juanro.minitos.data.worker.SyncWorker;

public class VehiclesViewModel extends AndroidViewModel {
    private final MinitosDatabase db;
    private final LiveData<List<VehicleWithFavorite>> vehicles;
    private final LiveData<List<VehicleWithFavorite>> favoriteVehicles;
    private final LiveData<List<Zone>> zones;
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public VehiclesViewModel(@NonNull Application application) {
        super(application);
        db = MinitosDatabase.getInstance(application);
        vehicles = db.getVehicleDao().getAllVehiclesWithFavorite();
        favoriteVehicles = db.getVehicleDao().getFavoriteVehicles();
        zones = db.getZoneDao().getAllZones();
    }

    public LiveData<List<VehicleWithFavorite>> getVehicles() {
        return vehicles;
    }

    public LiveData<List<VehicleWithFavorite>> getFavoriteVehicles() {
        return favoriteVehicles;
    }

    public LiveData<List<Zone>> getZones() {
        return zones;
    }

    public LiveData<Boolean> isFavorite(String id) {
        return db.getVehicleDao().isFavorite(id);
    }

    public void toggleFavorite(String id, boolean favorite) {
        DB_EXECUTOR.execute(() -> {
            if (favorite) {
                db.getVehicleDao().addFavorite(new FavoriteVehicle(id));
            } else {
                db.getVehicleDao().removeFavorite(id);
            }
        });
    }

    public void syncVehicles(double lat, double lon) {
        Data inputData = new Data.Builder()
                .putDouble("lat", lat)
                .putDouble("lon", lon)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplication()).enqueue(syncRequest);
    }
}
