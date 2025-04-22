<?php
header('Content-Type: application/json');

// Configuración de la base de datos
$DB_SERVER = "localhost";
$DB_USER = "Xapedrueza002";
$DB_PASS = "evYvBefGYz";
$DB_DATABASE = "Xapedrueza002_database";

$con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);
if (mysqli_connect_errno()) {
    echo json_encode(['error' => 'Error de conexión: ' . mysqli_connect_error()]);
    exit();
}

$query = "SELECT id, latitud, longitud, observaciones, fecha, imagen FROM Xapedrueza002_reportes ORDER BY RAND() LIMIT 1";
$result = mysqli_query($con, $query);

if ($row = mysqli_fetch_assoc($result)) {
    $imagenBase64 = base64_encode($row['imagen']);
    echo json_encode([
        'id' => $row['id'],
        'latitud' => $row['latitud'],
        'longitud' => $row['longitud'],
        'observaciones' => $row['observaciones'],
        'fecha' => $row['fecha'],
        'imagen' => $imagenBase64
    ]);
} else {
    echo json_encode(['error' => 'No hay incidencias']);
}

mysqli_close($con);
?>
