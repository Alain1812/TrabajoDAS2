package com.example.trabajodas2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class ReminderAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
        PermisosHelper.programarAlarma(context);
    }

    // Gestionar notificacion de reocrdatorio
    private void showNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "recordatorios_channel",
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager.createNotificationChannel(channel);
        }

        // Construir notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "recordatorios_channel")
                .setContentTitle("¡Recordatorio!")
                .setContentText("No olvides revisar las incidencias")
                .setSmallIcon(R.drawable.ic_ubicacion)
                .setAutoCancel(true)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setVibrate(new long[]{0, 500, 250, 500});

        manager.notify(100, builder.build());
    }

}