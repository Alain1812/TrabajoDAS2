package com.example.trabajodas2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HomeActivity extends AppCompatActivity {

    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FloatingActionButton fabAddReport;
    private Uri imageUri;
    private int userID;
    private Location currentLocation;
    private Marker marcadorUbicacion;
    private String observacionesTexto = "";
    private static final int GALLERY_REQUEST = 2;
    private boolean modoEdicion = false;
    private int reporteIdEnEdicion;
    private String observacionesEnEdicion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_home);

        crearCanalDeNotificaciones();
        iniciarServicioForeground();

        // Configuración inicial
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userID = prefs.getInt("user_id", 0);
        map = findViewById(R.id.mapView);
        fabAddReport = findViewById(R.id.fabAddReport);

        // Configurar componentes
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Establecer centro y zoom
        map.getController().setZoom(15.0);
        map.setExpectedCenter(new GeoPoint(0.0, 0.0));

        // Configurar FAB
        fabAddReport.setOnClickListener(v -> handleFabClick());

        // Comprobar permisos de ubicacion
        if (PermisosHelper.tienePermisosUbicacion(this)) {
            startLocationUpdates();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(loc -> {
                        if (loc != null) {
                            currentLocation = loc;
                            updateMapPosition(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                        }
                    });
        } else {
            PermisosHelper.verificarPermisosUbicacion(this);
        }

        // Cargar puntos de reportes
        cargarReportesEnMapa();
    }

    private void handleFabClick() {
        if (currentLocation != null) {
            showImagePickerDialog();
        } else {
            getCurrentLocation();
        }
    }


    // Actualizar ubicaciones
    private void startLocationUpdates() {
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                currentLocation = locationResult.getLastLocation();
                if (currentLocation != null) {
                    updateMapPosition(new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
                }
            }
        };
        if (PermisosHelper.tienePermisosUbicacion(this)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    // Obtener ubicacion actual
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                updateMapPosition(new GeoPoint(location.getLatitude(), location.getLongitude()));
                showImagePickerDialog();
            }
        });
    }

    // Actualizar tu ubicacion actual
    private void updateMapPosition(GeoPoint newPosition) {
        runOnUiThread(() -> {
            if (marcadorUbicacion == null) {
                marcadorUbicacion = new Marker(map);
                marcadorUbicacion.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlays().add(marcadorUbicacion);
            }
            marcadorUbicacion.setPosition(newPosition);
            marcadorUbicacion.setTitle("Tu ubicación actual");
            map.getController().setCenter(newPosition);
            map.getController().setZoom(15.0);
            map.invalidate();
        });
    }

    // Dialogo para subir incidencias
    private void showImagePickerDialog() {
        final EditText input = new EditText(this);
        input.setHint("Escribe tus observaciones");

        new AlertDialog.Builder(this)
                .setTitle("Reportar incidencia")
                .setMessage("Seleccione el origen de la evidencia fotográfica")
                .setView(input)
                .setPositiveButton("Cámara", (dialog, which) -> {
                    observacionesTexto = input.getText().toString();
                    if (PermisosHelper.verificarPermisoCamara(this)) {
                        openCamera();
                    }
                })
                .setNegativeButton("Galería", (dialog, which) -> {
                    observacionesTexto = input.getText().toString();
                    openGallery();
                })
                .show();
    }

    // Metodo para abrir la camara
    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, PermisosHelper.REQUEST_CODE_CAMARA);
            } else {
                Toast.makeText(this, "Error al crear archivo", Toast.LENGTH_LONG).show();
            }
        } catch (IOException | SecurityException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("CAMERA_ERROR", "Error en la cámara", e);
        }
    }

    // Crear archivo para la imagen
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "INCIDENCIA_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio");
        }

        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    // Abrir galeria
    private void openGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, GALLERY_REQUEST);
    }

    // Gestionar resultados para subir y editar reprotes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PermisosHelper.REQUEST_CODE_CAMARA || requestCode == GALLERY_REQUEST) {
                if (requestCode == GALLERY_REQUEST && data != null) {
                    imageUri = data.getData();
                }

                if (imageUri != null) {
                    if (modoEdicion) {
                        actualizarReporte();
                        modoEdicion = false;
                    } else {
                        subirReporte();
                    }
                }
            }
        }
    }

    // Metodo para crear el canal de notificaciones
    private void crearCanalDeNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    "foreground_channel",
                    "Estado de Ubicación",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(canal);
        }
    }

    // Metodo para inciar el servicio foreground
    private void iniciarServicioForeground() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        String status = (isGpsEnabled) ? "Activada" : "Desactivada";

        Intent intent = new Intent(getApplicationContext(), ForegroundService.class);
        intent.putExtra("status", status);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // Metodo para actualizar los reportes
    private void actualizarReporte() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                // Procesar la imagen
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                // Preparar la conexión
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/apedrueza002/WEB/editar_reporte.php");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                // Construir los parámetros
                String parametros = "id=" + URLEncoder.encode(String.valueOf(reporteIdEnEdicion), "UTF-8")
                        + "&usuario_id=" + URLEncoder.encode(String.valueOf(userID), "UTF-8")
                        + "&imagen=" + URLEncoder.encode(imageBase64, "UTF-8")
                        + "&observaciones=" + URLEncoder.encode(observacionesEnEdicion, "UTF-8");

                // Escribirlos en el body
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(parametros.getBytes(StandardCharsets.UTF_8));
                }

                // Manejar la respuesta exactamente
                handleResponse(conn.getResponseCode(), conn);

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // MEtodo para subir un reporte
    private void subirReporte() {
        new Thread(() -> {
            try {
                // Cargar el bitmap desde el archivo guardado
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Reducir calidad para evitar problemas de memoria
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP); // Sin saltos de línea

                // Obtener observaciones del campo de texto
                String observaciones = observacionesTexto != null ? observacionesTexto : "";

                // Configurar conexión HTTP
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/apedrueza002/WEB/subir_reporte.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Crear parámetros POST
                String parametros = "usuario_id=" + userID
                        + "&latitud=" + currentLocation.getLatitude()
                        + "&longitud=" + currentLocation.getLongitude()
                        + "&imagen=" + URLEncoder.encode(imageBase64, "UTF-8")
                        + "&observaciones=" + URLEncoder.encode(observaciones, "UTF-8");

                // Enviar datos
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(parametros.getBytes());
                }

                // Manejar respuesta
                handleResponse(conn.getResponseCode(), conn);

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // MEtodo para cargar en el mapa los reportes de la base de datos
    private void cargarReportesEnMapa() {
        new Thread(() -> {
            try {
                // Preparar la conexion
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/apedrueza002/WEB/get_reportes.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) {
                    response.append(linea);
                }
                reader.close();

                JSONArray reportes = new JSONArray(response.toString());
                runOnUiThread(() -> {
                    // Guardar el marcador de ubicación actual
                    Marker marcadorUsuario = marcadorUbicacion;

                    // Limpiar todos los overlays
                    map.getOverlays().clear();

                    // Restaurar el marcador de ubicación si existe
                    if (marcadorUsuario != null) {
                        map.getOverlays().add(marcadorUsuario);
                    }

                    for (int i = 0; i < reportes.length(); i++) {
                        try {
                            JSONObject reporte = reportes.getJSONObject(i);
                            int id = reporte.getInt("id");
                            int usuarioId = reporte.getInt("usuario_id");
                            double latitud = reporte.getDouble("latitud");
                            double longitud = reporte.getDouble("longitud");
                            String imagenBase64 = reporte.getString("imagen");
                            String observaciones = reporte.getString("observaciones");

                            GeoPoint punto = new GeoPoint(latitud, longitud);
                            Marker marcador = new Marker(map);
                            marcador.setPosition(punto);
                            marcador.setTitle("Reporte #" + id);
                            marcador.setIcon(getResources().getDrawable(R.drawable.ic_pin));

                            marcador.setOnMarkerClickListener((marker, mapView) -> {
                                // Decodificar la imagen
                                byte[] imageBytes = Base64.decode(imagenBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                                // ScrollView de contención
                                ScrollView scroll = new ScrollView(HomeActivity.this);
                                scroll.setLayoutParams(new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                ));

                                // Layout vertical dentro del ScrollView
                                LinearLayout layout = new LinearLayout(HomeActivity.this);
                                layout.setOrientation(LinearLayout.VERTICAL);
                                layout.setPadding(20, 20, 20, 20);
                                scroll.addView(layout);

                                // ImageView
                                ImageView imageView = new ImageView(HomeActivity.this);
                                imageView.setImageBitmap(bitmap);
                                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                imageView.setLayoutParams(imgParams);
                                layout.addView(imageView);

                                // TextView con las observaciones
                                TextView textView = new TextView(HomeActivity.this);
                                textView.setText("Observaciones: " + observaciones);
                                textView.setPadding(0, 10, 0, 0);
                                layout.addView(textView);

                                // Construir y mostrar el diálogo
                                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this)
                                        .setTitle("Reporte #" + id)
                                        .setView(scroll);

                                // Si eres el creador puedes borrar y editar
                                if (userID == usuarioId) {
                                    builder.setNegativeButton("Borrar", (d, w) -> borrarReporte(id))
                                            .setNeutralButton("Editar", (d, w) -> {
                                                reporteIdEnEdicion = id;
                                                observacionesEnEdicion = observaciones;
                                                editarReporte();
                                            });
                                }

                                builder.setPositiveButton("Cerrar", (d, w) -> d.dismiss())
                                        .show();

                                return true;
                            });
                            map.getOverlays().add(marcador);
                        } catch (JSONException e) {
                            Log.e("CARGAR_REPORTES", "Error al parsear el JSON: " + e.getMessage());
                        }
                    }
                    map.invalidate();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, "Error al cargar reportes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("CARGAR_REPORTES", "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    // Metodo para borrar reporte
    private void borrarReporte(int reporteId) {
        new Thread(() -> {
            try {
                // Preparar la conexion
                URL url = new URL("http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/apedrueza002/WEB/borrar_reporte.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String parametros = "{\"id\":" + reporteId + ",\"usuario_id\":" + userID + "}";

                conn.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(parametros.getBytes());
                }

                // Manejar respuesta exitosa
                if (conn.getResponseCode() == 200) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Reporte eliminado", Toast.LENGTH_SHORT).show();
                        cargarReportesEnMapa(); // Actualizar el mapa
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al borrar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // Merodo para el dialogo de editar reporte
    private void editarReporte() {
        modoEdicion = true;

        final EditText input = new EditText(this);
        input.setText(observacionesEnEdicion);
        input.setHint("Editar observaciones");

        new AlertDialog.Builder(this)
                .setTitle("Editar reporte")
                .setMessage("Seleccione el origen de la nueva imagen")
                .setView(input)
                .setPositiveButton("Cámara", (dialog, which) -> {
                    observacionesEnEdicion = input.getText().toString();
                    if (PermisosHelper.verificarPermisoCamara(this)) {
                        openCamera();
                    }
                })
                .setNegativeButton("Galería", (dialog, which) -> {
                    observacionesEnEdicion = input.getText().toString();
                    openGallery();
                })
                .show();
    }


    // Metodo para manejar las respuestas
    private void handleResponse(int responseCode, HttpURLConnection conn) {
        runOnUiThread(() -> {
            try {
                InputStream is = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                if (response.length() == 0) {
                    throw new Exception("Respuesta vacía del servidor");
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.has("success")) {
                    Toast.makeText(this, "Reporte enviado", Toast.LENGTH_SHORT).show();
                    // Recargar los reportes después de subir uno nuevo
                    cargarReportesEnMapa();
                } else {
                    String error = jsonResponse.getString("error");
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Toast.makeText(this, "Error en el formato JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error al procesar respuesta: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Metodo para gestionar si aceptan los permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermisosHelper.manejarResultadoPermiso(
                this,
                requestCode,
                grantResults,
                this::startLocationUpdates, // Si se conceden permisos de ubicación
                this::openCamera    // Si se concede permiso de cámara
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
        map.onDetach();
    }

}