-- Drop tables if they exist to start fresh (reverse order of dependencies)
DROP TABLE IF EXISTS refunds;
DROP TABLE IF EXISTS order_cancellations;
DROP TABLE IF EXISTS salary_payments;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS salary;
DROP TABLE IF EXISTS agent_audit_log;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS tracking;
DROP TABLE IF EXISTS fuel_expenses;
DROP TABLE IF EXISTS deliveries;
DROP TABLE IF EXISTS agent_activity_log;
DROP TABLE IF EXISTS password_change_log;
DROP TABLE IF EXISTS agents;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;

-- 1. Users Table (shared login credentials and role-based details)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL, -- ADMIN, CUSTOMER, AGENT
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

-- 2. Customers Table
CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Vehicles Table
CREATE TABLE vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL, -- Bike, Car, Van, Truck
    fuel_type VARCHAR(50) NOT NULL,
    insurance_details VARCHAR(255) DEFAULT NULL,
    status VARCHAR(50) DEFAULT 'AVAILABLE', -- AVAILABLE, ASSIGNED, MAINTENANCE
    max_load_capacity DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- 4. Agents Table (Uses alphanumeric ID, e.g. AGENT-1001)
CREATE TABLE agents (
    agent_id VARCHAR(50) PRIMARY KEY, -- Generated ID, e.g. AGENT-1001
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    assigned_vehicle_id INT DEFAULT NULL,
    password VARCHAR(100) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    termination_date TIMESTAMP DEFAULT NULL,
    termination_reason VARCHAR(500) DEFAULT NULL,
    password_updated_at TIMESTAMP DEFAULT NULL,
    last_login TIMESTAMP DEFAULT NULL,
    dob DATE DEFAULT NULL,
    gender VARCHAR(20) DEFAULT NULL,
    address VARCHAR(255) DEFAULT NULL,
    city VARCHAR(100) DEFAULT NULL,
    state VARCHAR(100) DEFAULT NULL,
    pincode VARCHAR(20) DEFAULT NULL,
    license_number VARCHAR(50) DEFAULT NULL,
    profile_image VARCHAR(255) DEFAULT NULL,
    joining_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shift_type VARCHAR(50) DEFAULT 'Morning',
    shift_start_time TIME DEFAULT '09:00:00',
    shift_end_time TIME DEFAULT '18:00:00',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5. Deliveries Table
CREATE TABLE deliveries (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    agent_id VARCHAR(50) DEFAULT NULL,
    vehicle_id INT DEFAULT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    package_details VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, REJECTED
    total_cost DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    pickup_latitude DOUBLE DEFAULT NULL,
    pickup_longitude DOUBLE DEFAULT NULL,
    destination_latitude DOUBLE DEFAULT NULL,
    destination_longitude DOUBLE DEFAULT NULL,
    current_latitude DOUBLE DEFAULT NULL,
    current_longitude DOUBLE DEFAULT NULL,
    assigned_time TIMESTAMP DEFAULT NULL,
    pickup_time TIMESTAMP DEFAULT NULL,
    delivery_time TIMESTAMP DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE SET NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 6. Fuel Expenses Table
CREATE TABLE fuel_expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    expense_date DATE NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL, -- in Litres
    price DECIMAL(10, 2) NOT NULL, -- price per Litre
    total DECIMAL(10, 2) NOT NULL, -- quantity * price
    bill_image_path VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Tracking Table (Real-time GPS status log)
CREATE TABLE tracking (
    id INT AUTO_INCREMENT PRIMARY KEY,
    delivery_id INT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    status VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Feedback Table
CREATE TABLE feedback (
    id INT AUTO_INCREMENT PRIMARY KEY,
    delivery_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Agent Audit Log Table
CREATE TABLE agent_audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    action_by_admin VARCHAR(50) NOT NULL,
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500) DEFAULT NULL
) ENGINE=InnoDB;

-- 9a. Password Change Log Table
CREATE TABLE password_change_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9b. Agent Activity Log Table
CREATE TABLE agent_activity_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 10. Salary Table
CREATE TABLE salary (
    salary_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    salary_month VARCHAR(20) NOT NULL,
    salary_year INT NOT NULL,
    basic_salary DECIMAL(10, 2) NOT NULL,
    incentive DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    bonus DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    fuel_reimbursement DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    deductions DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    net_salary DECIMAL(10, 2) NOT NULL,
    salary_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. Orders Table
CREATE TABLE orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    product_name VARCHAR(255) DEFAULT NULL,
    product_category VARCHAR(100) DEFAULT NULL,
    product_price DECIMAL(10, 2) DEFAULT NULL,
    quantity INT DEFAULT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    delivery_address VARCHAR(255) NOT NULL,
    parcel_description VARCHAR(255) NOT NULL,
    parcel_weight DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    package_type VARCHAR(50) NOT NULL,
    delivery_charge DECIMAL(10, 2) NOT NULL,
    tax DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED (overall status for legacy compatibility)
    delivery_id INT DEFAULT NULL,
    assigned_agent_id VARCHAR(50) DEFAULT NULL,
    assigned_vehicle_id INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_agent_id) REFERENCES agents(agent_id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 12. Payments Table
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    customer_id INT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(100) DEFAULT NULL,
    razorpay_order_id VARCHAR(100) DEFAULT NULL,
    razorpay_payment_id VARCHAR(100) DEFAULT NULL,
    razorpay_signature VARCHAR(255) DEFAULT NULL,
    failure_reason VARCHAR(255) DEFAULT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 13. Salary Payments Table (Logs detailed transactions for payroll disbursements)
CREATE TABLE salary_payments (
    salary_payment_id INT AUTO_INCREMENT PRIMARY KEY,
    salary_id INT NOT NULL,
    agent_id VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED
    transaction_id VARCHAR(100) DEFAULT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (salary_id) REFERENCES salary(salary_id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agents(agent_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 14. Order Cancellations Table
CREATE TABLE order_cancellations (
    cancellation_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    customer_id INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    cancellation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 15. Refunds Table
CREATE TABLE refunds (
    refund_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_id INT DEFAULT NULL,
    order_id INT NOT NULL,
    customer_id INT NOT NULL,
    razorpay_payment_id VARCHAR(100) DEFAULT NULL,
    razorpay_refund_id VARCHAR(100) DEFAULT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    refund_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES payments(payment_id) ON DELETE SET NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB;
