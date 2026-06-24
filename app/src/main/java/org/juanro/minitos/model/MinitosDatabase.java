package org.juanro.minitos.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serial;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.juanro.minitos.model.dao.VehicleDao;
import org.juanro.minitos.model.dao.ZoneDao;
import org.juanro.minitos.model.entity.Vehicle;
import org.juanro.minitos.model.entity.Zone;
import org.juanro.minitos.model.entity.FavoriteVehicle;

@Database(
    entities = {Vehicle.class, Zone.class, FavoriteVehicle.class},
    version = 1
)
public abstract class MinitosDatabase extends RoomDatabase {
    public static final String DATABASE_NAME = "minitos_room.db";

    public abstract VehicleDao getVehicleDao();
    public abstract ZoneDao getZoneDao();

    public static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final AtomicReference<MinitosDatabase> sInstance = new AtomicReference<>();
    private static final String LOG_TAG = "MinitosDatabase";

    public static MinitosDatabase getInstance(Context context) {
        MinitosDatabase db = sInstance.get();
        if (db == null) {
            synchronized (sInstance) {
                db = sInstance.get();
                if (db == null) {
                    var appContext = context.getApplicationContext();
                    var builder = Room.databaseBuilder(
                            appContext, MinitosDatabase.class, DATABASE_NAME);

                    // If we ever need migrations from version 1 to 2, we would add them here
                    // builder.addMigrations(new AssetFileBasedMigration(appContext, 2));

                    db = builder.build();
                    sInstance.set(db);
                }
            }
        }
        return db;
    }

    public static void resetInstance() {
        synchronized (sInstance) {
            MinitosDatabase db = sInstance.get();
            if (db != null) {
                db.close();
                sInstance.set(null);
            }
        }
    }

    private static class AssetFileBasedMigration extends Migration {

        private final int newVersion;
        private final Context context;

        AssetFileBasedMigration(Context context, int newVersion) {
            super(newVersion - 1, newVersion);
            this.newVersion = newVersion;
            this.context = context;
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Log.i(LOG_TAG, String.format(Locale.US, "Migrating database to version %d...", newVersion));
            try (var reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(String.format(Locale.US, "migrations/%d.sql", newVersion))))) {

                var statement = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    int commentIndex = line.indexOf("--");
                    if (commentIndex != -1) {
                        line = line.substring(0, commentIndex);
                    }

                    var trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) {
                        continue;
                    }

                    statement.append(line);
                    if (trimmedLine.endsWith(";")) {
                        database.execSQL(statement.toString());
                        statement.setLength(0);
                    } else {
                        statement.append(" ");
                    }
                }
                Log.i(LOG_TAG, String.format(Locale.US, "Migration to version %d completed successfully.", newVersion));
            } catch (IOException e) {
                Log.e(LOG_TAG, String.format(Locale.US, "Error during migration to version %d.", newVersion), e);
                throw new DatabaseMigrationException(newVersion, e);
            }
        }
    }

    public static class DatabaseMigrationException extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        public DatabaseMigrationException(int version, Throwable cause) {
            super("Critical error during database migration to version " + version, cause);
        }
    }
}
