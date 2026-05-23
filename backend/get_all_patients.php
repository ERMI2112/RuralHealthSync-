<?php
header("Content-Type: application/json");
require_once 'db_config.php';

$query = "SELECT id, full_name, age FROM patients ORDER BY id DESC LIMIT 50";
$result = $conn->query($query);
$patients = [];
if ($result) {
    while ($row = $result->fetch_assoc()) {
        $patients[] = [
            "id" => (int)$row['id'],
            "name" => $row['full_name'],
            "age" => (int)$row['age']
        ];
    }
}

send_json_response([
    "success" => true,
    "patients" => $patients
]);
?>
