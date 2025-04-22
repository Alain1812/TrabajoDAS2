<?php
header('Content-Type: application/json');

// Configuración de la base de datos
$DB_SERVER   = "localhost";
$DB_USER     = "Xapedrueza002";
$DB_PASS     = "evYvBefGYz";
$DB_DATABASE = "Xapedrueza002_database";

try {
    // Conexión
    $con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);
    if (mysqli_connect_errno()) {
        throw new Exception('Error de conexión: ' . mysqli_connect_error());
    }

    // Validar que llegaron todos los campos
    if (!isset($_POST['id'], $_POST['usuario_id'], $_POST['imagen'], $_POST['observaciones'])) {
        throw new Exception('Datos incompletos');
    }

    $id            = (int)   $_POST['id'];
    $usuario_id    = (int)   $_POST['usuario_id'];
    $imagenBase64  =          $_POST['imagen'];
    $observaciones =          $_POST['observaciones'];

    // Decodificar la imagen Base64
    $imagenBinaria = base64_decode($imagenBase64);
    if ($imagenBinaria === false) {
        throw new Exception('Formato Base64 inválido');
    }

    // Preparar y ejecutar el UPDATE
    $sql = "UPDATE Xapedrueza002_reportes SET imagen = ?, observaciones = ? WHERE id = ? AND usuario_id = ?";
    $stmt = mysqli_prepare($con, $sql);
    if (!$stmt) {
        throw new Exception('Error al preparar la consulta: ' . mysqli_error($con));
    }

    // b = blob, s = string, i = int, i = int
    $null = NULL;
    mysqli_stmt_bind_param($stmt, "bsii", $null, $observaciones, $id, $usuario_id);
    mysqli_stmt_send_long_data($stmt, 0, $imagenBinaria);

    if (!mysqli_stmt_execute($stmt)) {
        throw new Exception('Error al ejecutar la actualización: ' . mysqli_stmt_error($stmt));
    }

    if (mysqli_stmt_affected_rows($stmt) > 0) {
        echo json_encode(['success' => true, 'message' => 'Reporte actualizado correctamente']);
    } else {
        http_response_code(404);
        echo json_encode(['error' => 'No se encontró el reporte o no tienes permiso para editarlo']);
    }

    mysqli_stmt_close($stmt);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
} finally {
    if (isset($con) && $con) {
        mysqli_close($con);
    }
}
?>
