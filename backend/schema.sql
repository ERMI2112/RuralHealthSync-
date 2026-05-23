CREATE DATABASE IF NOT EXISTS rural_health_db;
USE rural_health_db;

-- Table for Health Workers (Users)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'CHW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- Table for Patients
CREATE TABLE IF NOT EXISTS patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    worker_id INT,
    full_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10),
    phone VARCHAR(20),
    address TEXT,
    diagnosis TEXT,
    photo_url TEXT,
    created_at_mobile BIGINT NOT NULL,
    next_visit_date BIGINT DEFAULT 0,
    latitude DOUBLE DEFAULT 0.0,
    longitude DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (worker_id) REFERENCES users(id) ON DELETE CASCADE
);
