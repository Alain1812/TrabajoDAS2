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

// Obtener datos del POST
$datos = json_decode(file_get_contents("php://input"), true);

// Validar datos recibidos
if (!isset($datos['id']) || !isset($datos['usuario_id'])) {
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

$id = $datos['id'];
$usuario_id = $datos['usuario_id'];

try {
    // Preparar consulta segura con sentencias preparadas
    $stmt = $con->prepare("DELETE FROM Xapedrueza002_reportes WHERE id = ? AND usuario_id = ?");
    
    // Vincular parámetros
    $stmt->bind_param("ii", $id, $usuario_id);
    
    // Ejecutar consulta
    if ($stmt->execute()) {
        if ($stmt->affected_rows > 0) {
            echo json_encode(['success' => true]);
        } else {
            echo json_encode(['error' => 'No se encontró el reporte o no tienes permiso']);
        }
    } else {
        echo json_encode(['error' => 'Error en la ejecución: ' . $stmt->error]);
    }
    
    $stmt->close();
    
} catch (Exception $e) {
    echo json_encode(['error' => 'Excepción: ' . $e->getMessage()]);
} finally {
    $con->close();
}
?>