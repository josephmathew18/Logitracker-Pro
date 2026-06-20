package com.delivery.repository;

import com.delivery.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    
    Optional<Feedback> findByOrderOrderId(Integer orderId);
    
    List<Feedback> findByCustomerUserUsernameOrderByCreatedAtDesc(String username);
    
    List<Feedback> findByAgentIdOrderByCreatedAtDesc(String agentId);

    @Query("SELECT f FROM Feedback f WHERE " +
           "(:rating IS NULL OR f.rating = :rating) AND " +
           "(:startDate IS NULL OR f.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR f.createdAt <= :endDate) AND " +
           "(:search IS NULL OR LOWER(f.comments) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(f.customer.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR (f.agent IS NOT NULL AND LOWER(f.agent.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "OR (f.order.productName IS NOT NULL AND LOWER(f.order.productName) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY f.createdAt DESC")
    List<Feedback> searchAndFilterFeedback(
            @Param("rating") Integer rating,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search);
}
