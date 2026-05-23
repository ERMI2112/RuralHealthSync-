<?php
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
error_reporting(E_ALL);
ini_set('display_errors', '0');

if (!function_exists('send_json_response')) {
    function send_json_response(array $payload, int $statusCode = 200): void
    {
        if (!headers_sent()) {
            http_response_code($statusCode);
            header("Content-Type: application/json");
        }

        echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        exit;
    }
}

if (!function_exists('column_exists')) {
    function column_exists(mysqli $conn, string $tableName, string $columnName): bool
    {
        $stmt = $conn->prepare(
            "SELECT 1
             FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
             AND TABLE_NAME = ?
             AND COLUMN_NAME = ?
             LIMIT 1"
        );
        $stmt->bind_param("ss", $tableName, $columnName);
        $stmt->execute();
        $result = $stmt->get_result();
        $exists = $result->fetch_row() !== null;
        $stmt->close();
        return $exists;
    }
}

if (!function_exists('ensure_patient_schema')) {
    function ensure_patient_schema(mysqli $conn): void
    {
        $requiredColumns = [
            "photo_url" => "ALTER TABLE patients ADD COLUMN photo_url TEXT NULL AFTER diagnosis",
            "next_visit_date" => "ALTER TABLE patients ADD COLUMN next_visit_date BIGINT DEFAULT 0 AFTER created_at_mobile",
            "latitude" => "ALTER TABLE patients ADD COLUMN latitude DOUBLE DEFAULT 0.0 AFTER next_visit_date",
            "longitude" => "ALTER TABLE patients ADD COLUMN longitude DOUBLE DEFAULT 0.0 AFTER latitude",
        ];

        foreach ($requiredColumns as $columnName => $ddl) {
            if (!column_exists($conn, "patients", $columnName)) {
                $conn->query($ddl);
            }
        }
    }
}

set_error_handler(static function (int $severity, string $message, string $file, int $line): bool {
    throw new ErrorException($message, 0, $severity, $file, $line);
});

set_exception_handler(static function (Throwable $throwable): void {
    error_log($throwable->__toString());
    send_json_response([
        "success" => false,
        "message" => "Server error: " . $throwable->getMessage(),
    ], 500);
});

$host = "localhost";
$db_name = "rural_health_db";
$username = "root";
$password = ""; // Default XAMPP password is empty

$conn = new mysqli($host, $username, $password, $db_name);
$conn->set_charset("utf8mb4");
ensure_patient_schema($conn);
