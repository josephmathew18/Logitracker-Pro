package com.delivery.repository;

import com.delivery.model.AgentPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentPerformanceRepository extends JpaRepository<AgentPerformance, Integer> {
    Optional<AgentPerformance> findByAgentIdAndMonthAndYear(String agentId, Integer month, Integer year);
    List<AgentPerformance> findByMonthAndYearOrderByPerformanceScoreDesc(Integer month, Integer year);
    List<AgentPerformance> findByAgentIdOrderByYearDescMonthDesc(String agentId);
}
