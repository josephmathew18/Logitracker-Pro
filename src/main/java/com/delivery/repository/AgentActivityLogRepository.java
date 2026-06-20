package com.delivery.repository;

import com.delivery.model.AgentActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for AgentActivityLog entity.
 */
@Repository
public interface AgentActivityLogRepository extends JpaRepository<AgentActivityLog, Long> {
    List<AgentActivityLog> findByAgentIdOrderByTimestampDesc(String agentId);
}
