package com.delivery.repository;

import com.delivery.model.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Integer> {
    List<SalaryPayment> findByAgentIdOrderByPaymentDateDesc(String agentId);
    List<SalaryPayment> findBySalarySalaryIdOrderByPaymentDateDesc(Integer salaryId);
    java.util.Optional<SalaryPayment> findByTransactionId(String transactionId);
}
