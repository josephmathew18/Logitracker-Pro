package com.delivery.repository;

import com.delivery.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Integer> {
    List<Refund> findByCustomerUserUsernameOrderByRefundDateDesc(String username);
    List<Refund> findByRefundStatus(String refundStatus);
    List<Refund> findByRazorpayRefundId(String razorpayRefundId);
    long countByRefundStatus(String status);
}
