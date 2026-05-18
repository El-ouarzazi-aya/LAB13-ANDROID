<?php
$host     = "localhost:3307";
$db_name  = "map_project";
$username = "root";
$password = "";

try {
    $conn = new PDO("mysql:host=$host;dbname=$db_name", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur de connexion: " . $e->getMessage()]);
    die();
}

try {
    $stmt = $conn->prepare("SELECT * FROM positions ORDER BY date DESC");
    $stmt->execute();
    $positions = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode(["success" => true, "positions" => $positions]);
} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur: " . $e->getMessage()]);
}
?>