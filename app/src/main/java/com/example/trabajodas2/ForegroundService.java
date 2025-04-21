package com.example.trabajodas2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;


public class ForegroundService extends Service {

    private static final String CHANNEL_ID = "foreground_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        // Actualizar siempre que llegue un nuevo Intent
        if (intent != null && intent.hasExtra("status")) {
            String status = intent.getStringExtra("status");
            updateNotification(status);
        } else {
            updateNotification("Esperando cambios...");
        }

        return START_STICKY;
    }

    // Metodo para crear el canal de notificaciones
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Estado de Ubicación",
                    NotificationManager.IMPORTANCE_LOW // Prioridad baja para no molestar
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // Metodo para actualizar la notificación
    public void updateNotification(String status) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Estado de la ubicación")
                .setContentText("Ubicación: " + status)
                .setSmallIcon(R.drawable.ic_ubicacion)
                .setOnlyAlertOnce(true) // Evita notificaciones repetidas
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}