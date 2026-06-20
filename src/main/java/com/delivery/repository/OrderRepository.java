package com.delivery.repository;

import com.delivery.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByCustomerUserUsernameOrderByCreatedAtDesc(String username);
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);
    Optional<Order> findByDeliveryId(Integer deliveryId);
}
