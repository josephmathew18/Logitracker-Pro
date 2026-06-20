package com.delivery.repository;

import com.delivery.model.Agent;
import com.delivery.model.FuelExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FuelExpenseRepository extends JpaRepository<FuelExpense, Integer> {
    List<FuelExpense> findByAgent(Agent agent);
    List<FuelExpense> findByAgentUserUsername(String username);
}
