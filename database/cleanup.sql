-- Disable foreign key checks temporarily for bulk deletions
SET REFERENTIAL_INTEGRITY FALSE;

-- Delete all records from transactional tables
DELETE FROM tracking;
DELETE FROM refunds;
DELETE FROM order_cancellations;
DELETE FROM payments;
DELETE FROM orders;
DELETE FROM feedback;
DELETE FROM deliveries;
DELETE FROM fuel_expenses;
DELETE FROM leave_applications;
DELETE FROM vehicle_maintenance;
DELETE FROM agent_audit_log;
DELETE FROM agent_activity_log;
DELETE FROM password_change_log;
DELETE FROM salary_payments;
DELETE FROM salary;
DELETE FROM agent_performance;
DELETE FROM agent_vehicle_history;
DELETE FROM notifications;
DELETE FROM audit_logs;
DELETE FROM attendance;

-- Re-enable foreign key checks
SET REFERENTIAL_INTEGRITY TRUE;

-- Delete partial data from tables that preserve specific records
DELETE FROM leave_balances WHERE agent_id <> '1001';
DELETE FROM documents WHERE agent_id <> '1001' OR agent_id IS NULL;

-- Manage vehicle assignments and delete other vehicles
UPDATE agents SET assigned_vehicle_id = NULL WHERE agent_id <> '1001';

-- Delete all agents except '1001'
DELETE FROM agents WHERE agent_id <> '1001';

-- Keep only the vehicle assigned to 1001, delete others
DELETE FROM vehicles WHERE vehicle_id NOT IN (
    SELECT COALESCE(assigned_vehicle_id, -1) FROM agents WHERE agent_id = '1001'
);

-- Remove vehicle assignments for remaining agents (Agent 1001)
UPDATE agents SET assigned_vehicle_id = NULL;

-- Delete user credentials for deleted agents
DELETE FROM users WHERE role = 'AGENT' AND username <> '1001';

-- Reset auto-increment columns using the correct primary key columns
ALTER TABLE tracking ALTER COLUMN id RESTART WITH 1;
ALTER TABLE refunds ALTER COLUMN refund_id RESTART WITH 1;
ALTER TABLE order_cancellations ALTER COLUMN cancellation_id RESTART WITH 1;
ALTER TABLE payments ALTER COLUMN payment_id RESTART WITH 1;
ALTER TABLE orders ALTER COLUMN order_id RESTART WITH 1;
ALTER TABLE feedback ALTER COLUMN feedback_id RESTART WITH 1;
ALTER TABLE deliveries ALTER COLUMN id RESTART WITH 1;
ALTER TABLE fuel_expenses ALTER COLUMN id RESTART WITH 1;
ALTER TABLE leave_applications ALTER COLUMN id RESTART WITH 1;
ALTER TABLE vehicle_maintenance ALTER COLUMN id RESTART WITH 1;
ALTER TABLE agent_audit_log ALTER COLUMN log_id RESTART WITH 1;
ALTER TABLE agent_activity_log ALTER COLUMN log_id RESTART WITH 1;
ALTER TABLE password_change_log ALTER COLUMN log_id RESTART WITH 1;
ALTER TABLE salary_payments ALTER COLUMN salary_payment_id RESTART WITH 1;
ALTER TABLE salary ALTER COLUMN salary_id RESTART WITH 1;
ALTER TABLE agent_performance ALTER COLUMN id RESTART WITH 1;
ALTER TABLE agent_vehicle_history ALTER COLUMN id RESTART WITH 1;
ALTER TABLE notifications ALTER COLUMN id RESTART WITH 1;
ALTER TABLE audit_logs ALTER COLUMN id RESTART WITH 1;
ALTER TABLE attendance ALTER COLUMN id RESTART WITH 1;
