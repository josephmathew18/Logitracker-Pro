package com.delivery.repository;

import com.delivery.model.LeaveApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Integer> {
    List<LeaveApplication> findByAgentIdOrderByAppliedAtDesc(String agentId);
    List<LeaveApplication> findByStatusOrderByAppliedAtDesc(String status);
    List<LeaveApplication> findAllByOrderByAppliedAtDesc();
    List<LeaveApplication> findByAgentIdAndStatus(String agentId, String status);
}
