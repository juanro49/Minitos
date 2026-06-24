package org.juanro.minitos.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.juanro.minitos.R;
import org.juanro.minitos.gui.activity.MapActivity;

public class TripNotificationService extends Service {
    public static final String CHANNEL_ID = "TripChannel";
    public static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    
    public static final String EXTRA_VEHICLE_NAME = "EXTRA_VEHICLE_NAME";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                String vehicleName = intent.getStringExtra(EXTRA_VEHICLE_NAME);
                startForeground(NOTIFICATION_ID, createNotification(vehicleName));
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private Notification createNotification(String vehicleName) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // For simplicity, using placeholders for actions. 
        // In a real implementation, these would trigger MapActivity or a BroadcastReceiver
        // to call the Minits API.
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Minitos en uso")
                .setContentText("Vehículo: " + (vehicleName != null ? vehicleName : "Desconocido"))
                .setSmallIcon(R.drawable.ic_launcher_minitos_foreground)
                .setContentIntent(pendingIntent)
                .setUsesChronometer(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Viaje Activo",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
