package com.example.trabajodas2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class ElReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Intent recibido: " + intent.getAction());

        if (intent == null || !LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
            return;
        }

        // Verificar permisos de ubicación
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permisos de ubicación no concedidos");
            return;
        }

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;

        try {
            boolean isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            String status = (isGpsEnabled) ? "Activada" : "Desactivada";
            Log.d(TAG, "Estado actualizado: " + status);

            // Iniciar servicio en primer plano con el nuevo estado
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.putExtra("status", status);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Error de seguridad: " + e.getMessage());
        }
    }
}