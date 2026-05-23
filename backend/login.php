<?php
header("Content-Type: application/json");
include 'db_config.php';

$data = json_decode(file_get_contents("php://input"), true);

if (isset($data['username']) && isset($data['password'])) {
    $user = $data['username'];
    $pass = $data['password'];

    $stmt = $conn->prepare("SELECT id, password, full_name, role FROM users WHERE username = ?");
    $stmt->bind_param("s", $user);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($row = $result->fetch_assoc()) {
        if (password_verify($pass, $row['password'])) {
            echo json_encode([
                "success" => true,
                "userId" => $row['id'],
                "fullName" => $row['full_name'],
                "role" => $row['role'],
                "message" => "Login successful"
            ]);
        } else {
            echo json_encode(["success" => false, "message" => "Invalid password"]);
        }
    } else {
        echo json_encode(["success" => false, "message" => "User not found"]);
    }
    $stmt->close();
} else {
    echo json_encode(["success" => false, "message" => "Missing credentials"]);
}
$conn->close();
?>
