package com.delivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Service handling database cleaning and resetting operations.
 */
@Service
public class DatabaseCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupService.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes the SQL statements in database/cleanup.sql to clean and reset database.
     */
    @Transactional
    public void cleanupDatabase() {
        logger.info("Starting database clean and reset procedure...");

        try {
            // Read cleanup script from project root database/cleanup.sql
            String scriptPath = "database/cleanup.sql";
            String content = Files.readString(Paths.get(scriptPath));
            
            // Clean comments from the script content first
            StringBuilder cleanSql = new StringBuilder();
            for (String line : content.split("\\r?\\n")) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("--")) {
                    continue;
                }
                // Strip inline comment if exists
                int commentIndex = line.indexOf("--");
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex);
                }
                cleanSql.append(line).append("\n");
            }
            
            // Split script by semicolon to execute statements individually
            String[] sqlStatements = cleanSql.toString().split(";");
            
            for (String sql : sqlStatements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    logger.debug("Executing SQL: {}", trimmedSql);
                    jdbcTemplate.execute(trimmedSql);
                }
            }
            
            logger.info("Database clean and reset procedure completed successfully.");
        } catch (IOException e) {
            logger.error("Failed to read cleanup.sql script. Falling back to programmatic cleanup.", e);
            runProgrammaticCleanup();
        } catch (Exception e) {
            logger.error("Error occurred during database reset", e);
            throw new RuntimeException("Database reset failed: " + e.getMessage(), e);
        }
    }

    /**
     * Programmatic fallback in case file read fails.
     */
    private void runProgrammaticCleanup() {
        String[] queries = {
            "SET REFERENTIAL_INTEGRITY FALSE",
            "DELETE FROM tracking",
            "DELETE FROM refunds",
            "DELETE FROM order_cancellations",
            "DELETE FROM payments",
            "DELETE FROM orders",
            "DELETE FROM feedback",
            "DELETE FROM deliveries",
            "DELETE FROM fuel_expenses",
            "DELETE FROM leave_applications",
            "DELETE FROM vehicle_maintenance",
            "DELETE FROM agent_audit_log",
            "DELETE FROM agent_activity_log",
            "DELETE FROM password_change_log",
            "DELETE FROM salary_payments",
            "DELETE FROM salary",
            "DELETE FROM agent_performance",
            "DELETE FROM agent_vehicle_history",
            "DELETE FROM notifications",
            "DELETE FROM audit_logs",
            "DELETE FROM attendance",
            "SET REFERENTIAL_INTEGRITY TRUE",
            "DELETE FROM leave_balances WHERE agent_id <> '1001'",
            "DELETE FROM documents WHERE agent_id <> '1001' OR agent_id IS NULL",
            "UPDATE agents SET assigned_vehicle_id = NULL WHERE agent_id <> '1001'",
            "DELETE FROM agents WHERE agent_id <> '1001'",
            "DELETE FROM vehicles WHERE vehicle_id NOT IN (SELECT COALESCE(assigned_vehicle_id, -1) FROM agents WHERE agent_id = '1001')",
            "UPDATE agents SET assigned_vehicle_id = NULL",
            "DELETE FROM users WHERE role = 'AGENT' AND username <> '1001'",
            "ALTER TABLE tracking ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE refunds ALTER COLUMN refund_id RESTART WITH 1",
            "ALTER TABLE order_cancellations ALTER COLUMN cancellation_id RESTART WITH 1",
            "ALTER TABLE payments ALTER COLUMN payment_id RESTART WITH 1",
            "ALTER TABLE orders ALTER COLUMN order_id RESTART WITH 1",
            "ALTER TABLE feedback ALTER COLUMN feedback_id RESTART WITH 1",
            "ALTER TABLE deliveries ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE fuel_expenses ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE leave_applications ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE vehicle_maintenance ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE agent_audit_log ALTER COLUMN log_id RESTART WITH 1",
            "ALTER TABLE agent_activity_log ALTER COLUMN log_id RESTART WITH 1",
            "ALTER TABLE password_change_log ALTER COLUMN log_id RESTART WITH 1",
            "ALTER TABLE salary_payments ALTER COLUMN salary_payment_id RESTART WITH 1",
            "ALTER TABLE salary ALTER COLUMN salary_id RESTART WITH 1",
            "ALTER TABLE agent_performance ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE agent_vehicle_history ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE notifications ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE audit_logs ALTER COLUMN id RESTART WITH 1",
            "ALTER TABLE attendance ALTER COLUMN id RESTART WITH 1"
        };

        for (String query : queries) {
            jdbcTemplate.execute(query);
        }
        logger.info("Programmatic fallback database reset completed successfully.");
    }
}
