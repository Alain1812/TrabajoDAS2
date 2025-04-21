package com.example.trabajodas2;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermisosHelper {

    // Códigos de solicitud
    public static final int REQUEST_CODE_NOTIFICACIONES = 101;
    public static final int REQUEST_CODE_ALARMAS = 102;
    public static final int REQUEST_CODE_UBICACION = 103;
    public static final int REQUEST_CODE_BACKGROUND_UBICACION = 104;
    public static final int REQUEST_CODE_CAMARA = 105;

    // Grupos de permisos
    private static final String[] PERMISOS_UBICACION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // =================================================================
    // Métodos para notificaciones
    // =================================================================
    public static void solicitarPermisoNotificaciones(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_NOTIFICACIONES
            );
        } else {
            // Toast.makeText(activity, "Permiso de notificaciones ya concedido", Toast.LENGTH_SHORT).show();
        }
    }

    // =================================================================
    // Métodos para alarmas
    // =================================================================
    public static void solicitarPermisoAlarma(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                mostrarDialogoAjustes(activity);
            } else {
                programarAlarma(activity);
            }
        } else {
            programarAlarma(activity);
        }
    }

    private static void mostrarDialogoAjustes(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Permiso requerido")
                .setMessage("Necesitas permitir alarmas exactas para los recordatorios.")
                .setPositiveButton("Abrir ajustes", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static void programarAlarma(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long intervalo = AlarmManager.INTERVAL_HALF_HOUR;
        long horaInicial = System.currentTimeMillis() + intervalo;

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        horaInicial,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, horaInicial, pendingIntent);
            }
            // Toast.makeText(context, "Alarma programada cada 30 minutos", Toast.LENGTH_SHORT).show();
        }
    }

    // =================================================================
    // Métodos para ubicación
    // =================================================================
    public static void verificarPermisosUbicacion(Activity activity) {
        if (!tienePermisosUbicacion(activity)) {
            solicitarPermisosUbicacion(activity);
        } else {
            verificarPermisoBackground(activity);
        }
    }

    public static boolean tienePermisosUbicacion(Context context) {
        return ContextCompat.checkSelfPermission(context, PERMISOS_UBICACION[0]) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, PERMISOS_UBICACION[1]) == PackageManager.PERMISSION_GRANTED;
    }

    private static void solicitarPermisosUbicacion(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                PERMISOS_UBICACION,
                REQUEST_CODE_UBICACION
        );
    }

    private static void verificarPermisoBackground(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            new AlertDialog.Builder(activity)
                    .setTitle("Permiso en segundo plano")
                    .setMessage("Este permiso permite el seguimiento de ubicación incluso cuando la app no está en uso.")
                    .setPositiveButton("Conceder", (d, w) ->
                            ActivityCompat.requestPermissions(
                                    activity,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    REQUEST_CODE_BACKGROUND_UBICACION
                            )
                    )
                    .setNegativeButton("Más tarde", null)
                    .show();
        }
    }

    // =================================================================
    // Métodos para cámara
    // =================================================================
    public static boolean verificarPermisoCamara(Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_CAMARA
            );
            return false;
        }
        return true;
    }

    // =================================================================
    // Manejo centralizado de resultados
    // =================================================================
    public static void manejarResultadoPermiso(
            Activity activity,
            int requestCode,
            int[] grantResults,
            Runnable onUbicacionConcedida,
            Runnable onCamaraConcedida
    ) {
        switch (requestCode) {
            case REQUEST_CODE_UBICACION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    verificarPermisoBackground(activity);
                    if (onUbicacionConcedida != null) onUbicacionConcedida.run();
                } else {
                   // Toast.makeText(activity, "Funcionalidad limitada sin permisos de ubicación", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_BACKGROUND_UBICACION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   // Toast.makeText(activity, "Permiso en segundo plano concedido", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_CAMARA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (onCamaraConcedida != null) onCamaraConcedida.run();
                } else {
                    // Toast.makeText(activity, "No puedes tomar fotos sin este permiso", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}