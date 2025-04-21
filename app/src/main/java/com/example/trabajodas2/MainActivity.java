package com.example.trabajodas2;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE_NOTIFICACIONES = 101;
    private static final int PERMISSION_REQUEST_CODE_ALARMAS = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Solicitar permisos para notificaciones y alarmas
        PermisosHelper.solicitarPermisoNotificaciones(this);
        PermisosHelper.solicitarPermisoAlarma(this);

        // Programar la alarma si los permisos están concedidos
        PermisosHelper.programarAlarma(this);

        // Configuración de los elementos de la interfaz
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegistro = findViewById(R.id.btnRegistro);

        // Listeners para los clics en botones
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    // Metodo que maneja la respuesta de los diálogos de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Manejo de permisos de notificaciones
        if (requestCode == PERMISSION_REQUEST_CODE_NOTIFICACIONES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show();
            } else {
                // Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show();
            }
        }

        // Manejo de permisos de alarmas
        if (requestCode == PERMISSION_REQUEST_CODE_ALARMAS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Toast.makeText(this, "Permiso de alarmas concedido", Toast.LENGTH_SHORT).show();
            } else {
                // Toast.makeText(this, "Permiso de alarmas denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reprogramar alarma si el usuario concedió el permiso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            cancelarAlarmaExistente();
            PermisosHelper.programarAlarma(this);
        }
    }

    // Metodo para cancelar cualquier alarma existente
    private void cancelarAlarmaExistente() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
