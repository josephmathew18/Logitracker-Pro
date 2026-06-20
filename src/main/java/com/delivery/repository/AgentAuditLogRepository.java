package com.delivery.repository;

import com.delivery.model.AgentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for AgentAuditLog entity.
 */
@Repository
public interface AgentAuditLogRepository extends JpaRepository<AgentAuditLog, Long> {
    List<AgentAuditLog> findByAgentIdOrderByActionDateDesc(String agentId);
}
