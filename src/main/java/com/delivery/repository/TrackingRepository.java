package com.delivery.repository;

import com.delivery.model.Delivery;
import com.delivery.model.Tracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackingRepository extends JpaRepository<Tracking, Integer> {
    List<Tracking> findByDeliveryIdOrderByUpdatedAtDesc(Integer deliveryId);
    List<Tracking> findByDeliveryOrderByUpdatedAtAsc(Delivery delivery);
}
