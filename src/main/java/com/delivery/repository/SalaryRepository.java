package com.delivery.repository;

import com.delivery.model.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Salary entity.
 */
@Repository
public interface SalaryRepository extends JpaRepository<Salary, Integer> {
    List<Salary> findByAgentId(String agentId);
    Optional<Salary> findByAgentIdAndMonthAndYear(String agentId, String month, Integer year);
}
