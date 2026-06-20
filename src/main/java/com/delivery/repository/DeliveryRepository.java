package com.delivery.repository;

import com.delivery.model.Customer;
import com.delivery.model.Agent;
import com.delivery.model.Delivery;
import com.delivery.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Integer> {
    List<Delivery> findByCustomer(Customer customer);
    List<Delivery> findByCustomerUserUsername(String username);
    List<Delivery> findByAgent(Agent agent);
    List<Delivery> findByAgentAndStatusIn(Agent agent, List<String> statuses);
    List<Delivery> findByAgentUserUsername(String username);
    List<Delivery> findByStatus(String status);
    long countByStatus(String status);
    List<Delivery> findByVehicle(Vehicle vehicle);
}
