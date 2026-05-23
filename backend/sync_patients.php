<?php
header("Content-Type: application/json");
require_once 'db_config.php';

$rawBody = file_get_contents("php://input");
$data = json_decode($rawBody, true);

if (!is_array($data)) {
    send_json_response([
        "success" => false,
        "message" => "Invalid JSON request body",
    ], 400);
}

if (empty($data['workerId'])) {
    send_json_response([
        "success" => false,
        "message" => "Unauthorized sync request",
    ], 400);
}

$workerId = (int)$data['workerId'];
$syncedIds = [];
$syncedRecords = [];

// Resolve role before deletes / sync
$roleQuery = $conn->prepare("SELECT role FROM users WHERE id = ?");
$roleQuery->bind_param("i", $workerId);
$roleQuery->execute();
$roleResult = $roleQuery->get_result();
$userRole = 'CHW';
if ($roleRow = $roleResult->fetch_assoc()) {
    $userRole = strtoupper((string)$roleRow['role']);
}
$roleQuery->close();

// 0. Apply deletions from mobile (so they are not returned on sync-down)
if (isset($data['deletedServerIds']) && is_array($data['deletedServerIds'])) {
    foreach ($data['deletedServerIds'] as $rawId) {
        $serverId = (int)$rawId;
        if ($serverId <= 0) {
            continue;
        }
        if ($userRole === 'ADMIN') {
            $del = $conn->prepare("DELETE FROM patients WHERE id = ?");
            $del->bind_param("i", $serverId);
        } else {
            $del = $conn->prepare("DELETE FROM patients WHERE id = ? AND worker_id = ?");
            $del->bind_param("ii", $serverId, $workerId);
        }
        $del->execute();
        $del->close();
    }
}

// 1. Sync Up: Save new patients from mobile to server
if (isset($data['patients']) && is_array($data['patients'])) {
    foreach ($data['patients'] as $patient) {
        if (!is_array($patient)) {
            continue;
        }

        $fullName = trim((string)($patient['fullName'] ?? ''));
        $age = (int)($patient['age'] ?? 0);
        $gender = trim((string)($patient['gender'] ?? ''));
        $phone = trim((string)($patient['phone'] ?? ''));
        $address = trim((string)($patient['address'] ?? ''));
        $diagnosis = trim((string)($patient['diagnosis'] ?? ''));
        $createdAtMobile = (int)($patient['createdAt'] ?? 0);
        $nextVisitDate = (int)($patient['nextVisitDate'] ?? 0);
        $latitude = (float)($patient['latitude'] ?? 0.0);
        $longitude = (float)($patient['longitude'] ?? 0.0);
        $localId = (int)($patient['id'] ?? 0);
        $serverId = 0;

        if ($fullName === '' || $age <= 0 || $localId <= 0) {
            continue;
        }

        // Global duplicate check: Avoid inserting if a patient with the exact same name and phone already exists anywhere in the system
        $check = $conn->prepare("SELECT id FROM patients WHERE full_name = ? AND phone = ?");
        $check->bind_param("ss", $fullName, $phone);
        $check->execute();
        $checkResult = $check->get_result();
        if ($row = $checkResult->fetch_assoc()) {
            $serverId = (int)$row['id'];
            $syncedIds[] = $localId;
        } else {
            $stmt = $conn->prepare("INSERT INTO patients (worker_id, full_name, age, gender, phone, address, diagnosis, created_at_mobile, next_visit_date, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->bind_param("isissssiidd", $workerId, $fullName, $age, $gender, $phone, $address, $diagnosis, $createdAtMobile, $nextVisitDate, $latitude, $longitude);
            if ($stmt->execute()) {
                $serverId = (int)$conn->insert_id;
                $syncedIds[] = $localId;
            }
            $stmt->close();
        }
        $check->close();

        if ($serverId > 0) {
            $syncedRecords[] = ["localId" => $localId, "serverId" => $serverId];
        }
    }
}

// 2. Sync Down: Fetch patients based on role
if ($userRole === 'ADMIN') {
    // Admins see all patients
    $query = "SELECT * FROM patients ORDER BY created_at_mobile DESC, id DESC";
    $stmt = $conn->prepare($query);
} else {
    // CHWs see only their own patients
    $query = "SELECT * FROM patients WHERE worker_id = ? ORDER BY created_at_mobile DESC, id DESC";
    $stmt = $conn->prepare($query);
    $stmt->bind_param("i", $workerId);
}

$stmt->execute();
$result = $stmt->get_result();
$allPatients = [];
while ($row = $result->fetch_assoc()) {
    $allPatients[] = [
        "serverId" => (int)$row['id'],
        "workerId" => (int)($row['worker_id'] ?? 0),
        "fullName" => $row['full_name'] ?? "",
        "age" => (int)($row['age'] ?? 0),
        "gender" => $row['gender'] ?? "",
        "phone" => $row['phone'] ?? "",
        "address" => $row['address'] ?? "",
        "diagnosis" => $row['diagnosis'] ?? "",
        "createdAt" => (int)($row['created_at_mobile'] ?? 0),
        "nextVisitDate" => (int)($row['next_visit_date'] ?? 0),
        "latitude" => (double)($row['latitude'] ?? 0.0),
        "longitude" => (double)($row['longitude'] ?? 0.0),
        "photoUrl" => ($row['photo_url'] ?? "")
    ];
}
$stmt->close();

send_json_response([
    "success" => true,
    "syncedIds" => $syncedIds,
    "syncedRecords" => $syncedRecords,
    "patients" => $allPatients,
    "message" => "Sync completed"
]);
