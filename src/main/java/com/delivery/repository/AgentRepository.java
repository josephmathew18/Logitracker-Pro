package com.delivery.repository;

import com.delivery.model.Agent;
import com.delivery.model.User;
import com.delivery.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {
    Optional<Agent> findByUser(User user);
    Optional<Agent> findByUserUsername(String username);
    Optional<Agent> findByVehicle(Vehicle vehicle);
}
