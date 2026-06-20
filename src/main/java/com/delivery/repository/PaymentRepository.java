package com.delivery.repository;

import com.delivery.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByCustomerUserUsernameOrderByPaymentDateDesc(String username);
    List<Payment> findByCustomerIdOrderByPaymentDateDesc(Integer customerId);
}
