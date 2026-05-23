<?php
header("Content-Type: application/json");
include 'db_config.php';

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data['adminId'])) {
    echo json_encode(["success" => false, "message" => "Unauthorized"]);
    exit;
}

$adminId = $data['adminId'];

// Verify Admin status
$roleCheck = $conn->prepare("SELECT role FROM users WHERE id = ?");
$roleCheck->bind_param("i", $adminId);
$roleCheck->execute();
$roleResult = $roleCheck->get_result();
if ($row = $roleResult->fetch_assoc()) {
    if ($row['role'] !== 'ADMIN') {
        echo json_encode(["success" => false, "message" => "Permission denied. Admins only."]);
        exit;
    }
} else {
    echo json_encode(["success" => false, "message" => "Admin user not found"]);
    exit;
}
$roleCheck->close();

$action = $data['action'] ?? 'list';

if ($action === 'list') {
    $query = "SELECT id, username, full_name, role, created_at FROM users";
    $result = $conn->query($query);
    $users = [];
    while ($row = $result->fetch_assoc()) {
        $users[] = $row;
    }
    echo json_encode(["success" => true, "users" => $users]);

} elseif ($action === 'create') {
    $username = $data['username'];
    $pass = password_hash($data['password'], PASSWORD_DEFAULT);
    $name = $data['full_name'];
    $role = $data['role'] ?? 'CHW';

    $stmt = $conn->prepare("INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("ssss", $username, $pass, $name, $role);
    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "User created successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Failed to create user: " . $stmt->error]);
    }
    $stmt->close();

} elseif ($action === 'delete') {
    $targetId = $data['targetId'];
    if ($targetId == $adminId) {
        echo json_encode(["success" => false, "message" => "Cannot delete yourself"]);
        exit;
    }
    $stmt = $conn->prepare("DELETE FROM users WHERE id = ?");
    $stmt->bind_param("i", $targetId);
    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "User deleted successfully"]);
    } else {
        echo json_encode(["success" => false, "message" => "Failed to delete user"]);
    }
    $stmt->close();
} else {
    echo json_encode(["success" => false, "message" => "Invalid action"]);
}

$conn->close();
?>
