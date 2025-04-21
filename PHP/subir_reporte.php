<?php
header('Content-Type: application/json');

// Configuración de la base de datos
$DB_SERVER = "localhost";
$DB_USER = "Xapedrueza002";
$DB_PASS = "evYvBefGYz";
$DB_DATABASE = "Xapedrueza002_database";

try {
    // Conexión
    $con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);
    if (mysqli_connect_errno()) {
        throw new Exception('Error de conexión: ' . mysqli_connect_error());
    }

    // Validar que llegaron todos los campos
    if (!isset($_POST['usuario_id'], $_POST['latitud'], $_POST['longitud'], $_POST['imagen'], $_POST['observaciones'])) {
        throw new Exception('Datos incompletos');
    }

    $usuario_id = (int)$_POST['usuario_id'];
    $latitud = (float)$_POST['latitud'];
    $longitud = (float)$_POST['longitud'];
    $imagenBase64 = $_POST['imagen'];
    $observaciones = $_POST['observaciones'];

    // Decodificar la imagen Base64
    $imagenBinaria = base64_decode($imagenBase64);
    if ($imagenBinaria === false) {
        throw new Exception('Formato Base64 inválido');
    }

    // Preparar y ejecutar el INSERT
    $query = "INSERT INTO Xapedrueza002_reportes (usuario_id, latitud, longitud, imagen, observaciones, fecha) VALUES (?, ?, ?, ?, ?, NOW())";
    $stmt = mysqli_prepare($con, $query);
    if (!$stmt) {
        throw new Exception('Error al preparar la consulta: ' . mysqli_error($con));
    }

    mysqli_stmt_bind_param($stmt, "iddbs", $usuario_id, $latitud, $longitud, $null, $observaciones);
    
    mysqli_stmt_send_long_data($stmt, 3, $imagenBinaria);

    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception('Error al insertar en la base de datos: ' . mysqli_error($con));
    }

    echo json_encode(['success' => true, 'message' => 'Reporte subido']);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
} finally {
    if (isset($con)) {
        mysqli_close($con);
    }
}
?>