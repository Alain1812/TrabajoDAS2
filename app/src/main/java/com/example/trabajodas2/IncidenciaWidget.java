package com.example.trabajodas2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IncidenciaWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            actualizarWidgetDesdeServidor(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        // Realizar la actualización inicial de inmediato
        actualizarWidgetDesdeServidor(context);

        // Configurar AlarmManager para actualizaciones periódicas
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);

        // Usar FLAG_IMMUTABLE ya que no necesitas modificar el PendingIntent después de crearlo
        PendingIntent pi = PendingIntent.getBroadcast(context, 7475, intent, PendingIntent.FLAG_IMMUTABLE);

        // Configurar que se ejecute cada minuto
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 3000, 60000, pi);
    }


    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        // Cancelar la alarma al deshabilitar el widget
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 7475, intent, PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi); // Cancelar la alarma
    }

    private void actualizarWidgetDesdeServidor(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/apedrueza002/WEB/get_reporte_aleatorio.php");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                JSONObject incidencia = new JSONObject(response.toString());

                // Debug: Verificar si los valores se están extrayendo correctamente
                String observaciones = incidencia.optString("observaciones", "Sin datos");
                String fechaReporte = incidencia.optString("fecha", "Sin fecha");
                String imagenBase64 = incidencia.optString("imagen", "");

                Bitmap imagenBitmap = null;
                if (!imagenBase64.isEmpty()) {
                    byte[] decodedString = Base64.decode(imagenBase64, Base64.DEFAULT);
                    imagenBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    // Redimensionar la imagen si es demasiado grande
                    imagenBitmap = redimensionarImagen(imagenBitmap, 200, 200); // Redimensionar a 200x200
                }

                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                views.setTextViewText(R.id.widgetObservacion, observaciones);
                views.setTextViewText(R.id.widgetFecha, "Fecha: " + fechaReporte); // Usamos la fecha aquí

                // Si tienes la imagen, la setea en el widget
                if (imagenBitmap != null) {
                    views.setImageViewBitmap(R.id.widgetImage, imagenBitmap);
                }

                appWidgetManager.updateAppWidget(appWidgetId, views);

            } catch (Exception e) {
                e.printStackTrace();
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
                views.setTextViewText(R.id.widgetObservacion, "Error al conectar");
                views.setTextViewText(R.id.widgetFecha, "");
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }).start();
    }

    // Metodo para actualizar el widget
    private void actualizarWidgetDesdeServidor(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, IncidenciaWidget.class));
        for (int appWidgetId : appWidgetIds) {
            actualizarWidgetDesdeServidor(context, appWidgetManager, appWidgetId);
        }
    }

    // Cambiar tamaño de la imagen
    private Bitmap redimensionarImagen(Bitmap imagen, int ancho, int alto) {
        return Bitmap.createScaledBitmap(imagen, ancho, alto, false);
    }
}