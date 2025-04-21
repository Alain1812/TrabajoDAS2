package com.example.trabajodas2;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
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

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        // Hilo para conexión de red
        new Thread(() -> {
            try {
                // Configurar conexión HTTP
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

                // Obtener los valores de la incidencia aleatoria
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

                // Actualizar los valores en el widget
                remoteViews.setTextViewText(R.id.widgetObservacion, observaciones);
                remoteViews.setTextViewText(R.id.widgetFecha, "Fecha: " + fechaReporte);

                // Si tienes la imagen, la setea en el widget
                if (imagenBitmap != null) {
                    remoteViews.setImageViewBitmap(R.id.widgetImage, imagenBitmap);
                }

                // Actualizar el widget
                ComponentName widgetComponent = new ComponentName(context, IncidenciaWidget.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                manager.updateAppWidget(widgetComponent, remoteViews);

            } catch (Exception e) {
                e.printStackTrace();
                // En caso de error, establecer un mensaje de error en el widget
                remoteViews.setTextViewText(R.id.widgetObservacion, "Error al conectar");
                remoteViews.setTextViewText(R.id.widgetFecha, "");
                ComponentName widgetComponent = new ComponentName(context, IncidenciaWidget.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(context);
                manager.updateAppWidget(widgetComponent, remoteViews);
            }
        }).start();
    }

    // Cambia el tamaño de la imagen
    private Bitmap redimensionarImagen(Bitmap imagen, int ancho, int alto) {
        return Bitmap.createScaledBitmap(imagen, ancho, alto, false);
    }
}
