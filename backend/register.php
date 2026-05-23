<?php
header("Content-Type: application/json");
include 'db_config.php';

$data = json_decode(file_get_contents("php://input"), true);

if (isset($data['username']) && isset($data['password']) && isset($data['full_name'])) {
    $user = $data['username'];
    $pass = password_hash($data['password'], PASSWORD_DEFAULT);
    $name = $data['full_name'];
    $role = isset($data['role']) ? $data['role'] : 'CHW';

    $stmt = $conn->prepare("INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("ssss", $user, $pass, $name, $role);


    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "User registered successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Registration failed: " . $stmt->error]);
    }
    $stmt->close();
} else {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
}
$conn->close();
?>
