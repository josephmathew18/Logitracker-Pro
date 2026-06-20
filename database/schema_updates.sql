-- Database Schema Updates for Delivery Service Provider Management System
-- These tables support the newly added advanced modules.
-- Since spring.jpa.hibernate.ddl-auto=update is enabled, Hibernate will auto-create these, 
-- but this script serves as a reference and manual backup.

-- 1. Attendance Management Table
CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    check_in_time DATETIME DEFAULT NULL,
    check_out_time DATETIME DEFAULT NULL,
    working_hours DOUBLE DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    is_late BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
);

-- 2. Leave Management Tables
CREATE TABLE IF NOT EXISTS leave_balances (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL UNIQUE,
    casual_leaves_left INT DEFAULT 12,
    sick_leaves_left INT DEFAULT 8,
    annual_leaves_left INT DEFAULT 15,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS leave_applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_type VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    remarks VARCHAR(500) DEFAULT NULL,
    applied_at DATETIME NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
);

-- 3. Vehicle Maintenance & Health Table
CREATE TABLE IF NOT EXISTS vehicle_maintenance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    scheduled_date DATE NOT NULL,
    completed_date DATE DEFAULT NULL,
    maintenance_cost DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    remarks VARCHAR(500) DEFAULT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

-- 4. Document Management Table
CREATE TABLE IF NOT EXISTS documents (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) DEFAULT NULL,
    vehicle_id INT DEFAULT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    expiry_date DATE DEFAULT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    remarks VARCHAR(500) DEFAULT NULL,
    uploaded_at DATETIME NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

-- 5. Notification Center Table
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 6. Audit Log System Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    username VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    remarks VARCHAR(500) DEFAULT NULL
);

-- 7. Agent Performance Metrics Cache Table
CREATE TABLE IF NOT EXISTS agent_performance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    success_rate DOUBLE DEFAULT 0.0,
    completed_count INT DEFAULT 0,
    cancelled_count INT DEFAULT 0,
    average_rating DOUBLE DEFAULT 0.0,
    performance_score DOUBLE DEFAULT 0.0,
    ranking INT DEFAULT 0,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE,
    UNIQUE KEY unique_agent_month_year (agent_id, month, year)
);

-- 8. Agent Vehicle Assignment History Table
CREATE TABLE IF NOT EXISTS agent_vehicle_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    vehicle_id INT NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL,
    assigned_at DATETIME NOT NULL,
    unassigned_at DATETIME DEFAULT NULL,
    assigned_by VARCHAR(100) DEFAULT 'SYSTEM',
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

-- 9. Add Agent Shift timing columns
ALTER TABLE agents ADD COLUMN IF NOT EXISTS shift_type VARCHAR(50) DEFAULT 'Morning';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS shift_start_time TIME DEFAULT '09:00:00';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS shift_end_time TIME DEFAULT '18:00:00';
