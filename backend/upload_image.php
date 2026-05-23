<?php
header("Content-Type: application/json");
include 'db_config.php';

$target_dir = "uploads/";
if (!file_exists($target_dir)) {
    mkdir($target_dir, 0777, true);
}

if (isset($_FILES["image"]) && isset($_POST["patientName"]) && isset($_POST["timestamp"])) {
    $patientName = preg_replace("/[^a-zA-Z0-9]/", "_", $_POST["patientName"]);
    $timestamp = $_POST["timestamp"];
    $extension = pathinfo($_FILES["image"]["name"], PATHINFO_EXTENSION);
    
    $filename = "patient_" . $patientName . "_" . $timestamp . "." . $extension;
    $target_file = $target_dir . $filename;

    if (move_uploaded_file($_FILES["image"]["tmp_name"], $target_file)) {
        // Base URL for the image
        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
        $baseUrl = $protocol . "://" . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . "/";
        $photoUrl = $baseUrl . $target_file;

        // Update the database if we have a server_id (optional, the app can also send it in sync)
        if (isset($_POST["serverId"])) {
            $serverId = $_POST["serverId"];
            $stmt = $conn->prepare("UPDATE patients SET photo_url = ? WHERE id = ?");
            $stmt->bind_param("si", $photoUrl, $serverId);
            $stmt->execute();
            $stmt->close();
        }

        echo json_encode([
            "success" => true,
            "photoUrl" => $photoUrl,
            "message" => "Image uploaded successfully"
        ]);
    } else {
        echo json_encode(["success" => false, "message" => "Failed to move uploaded file"]);
    }
} else {
    echo json_encode(["success" => false, "message" => "Missing file or metadata"]);
}
$conn->close();
?>
