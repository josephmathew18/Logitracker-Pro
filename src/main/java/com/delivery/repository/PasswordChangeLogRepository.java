package com.delivery.repository;

import com.delivery.model.PasswordChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for PasswordChangeLog entity.
 */
@Repository
public interface PasswordChangeLogRepository extends JpaRepository<PasswordChangeLog, Long> {
    List<PasswordChangeLog> findByAgentIdOrderByChangeDateDesc(String agentId);
}
