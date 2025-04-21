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

$query = "SELECT id, usuario_id, latitud, longitud, imagen, observaciones FROM Xapedrueza002_reportes";
$result = mysqli_query($con, $query);

$reportes = [];
while ($row = mysqli_fetch_assoc($result)) {
    $imagenBase64 = base64_encode($row['imagen']);
    $reportes[] = [
        'id' => $row['id'],
        'usuario_id' => $row['usuario_id'], 
        'latitud' => $row['latitud'],
        'longitud' => $row['longitud'],
        'imagen' => $imagenBase64,
        'observaciones' => $row['observaciones'] 
    ];
}

echo json_encode($reportes);

mysqli_close($con);
?>