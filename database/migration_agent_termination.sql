-- Migration Script: Agent Termination & Account Blocking

-- 1. Modify agents table
ALTER TABLE agents RENAME COLUMN id TO agent_id;
ALTER TABLE agents ADD COLUMN password VARCHAR(100) DEFAULT NULL;
ALTER TABLE agents ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE agents ADD COLUMN termination_date TIMESTAMP DEFAULT NULL;
ALTER TABLE agents ADD COLUMN termination_reason VARCHAR(500) DEFAULT NULL;

-- 2. Create agent_audit_log table
CREATE TABLE agent_audit_log (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    action_by_admin VARCHAR(50) NOT NULL,
    action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remarks VARCHAR(500) DEFAULT NULL
) ENGINE=InnoDB;

-- 3. Update foreign keys in other tables if migrating existing databases
-- Note: Depending on target DB (e.g. MySQL vs H2), dropping and re-creating FKs might be needed.
