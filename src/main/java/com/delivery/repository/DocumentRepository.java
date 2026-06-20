package com.delivery.repository;

import com.delivery.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    List<Document> findByAgentId(String agentId);
    List<Document> findByVehicleId(Integer vehicleId);
    List<Document> findByVerificationStatus(String status);
    List<Document> findAllByOrderByUploadedAtDesc();
}
