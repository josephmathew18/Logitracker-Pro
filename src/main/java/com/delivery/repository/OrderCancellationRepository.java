package com.delivery.repository;

import com.delivery.model.OrderCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderCancellationRepository extends JpaRepository<OrderCancellation, Integer> {
    List<OrderCancellation> findByCustomerUserUsernameOrderByCancellationDateDesc(String username);
    List<OrderCancellation> findByOrderOrderId(Integer orderId);
    List<OrderCancellation> findByStatus(String status);
}
