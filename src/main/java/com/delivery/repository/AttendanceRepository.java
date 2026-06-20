package com.delivery.repository;

import com.delivery.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    Optional<Attendance> findByAgentIdAndDate(String agentId, LocalDate date);
    List<Attendance> findByAgentIdOrderByDateDesc(String agentId);
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByDateAndStatus(LocalDate date, String status);
    List<Attendance> findByAgentIdAndDateBetweenOrderByDateAsc(String agentId, LocalDate start, LocalDate end);
    long countByAgentIdAndStatus(String agentId, String status);
    long countByAgentIdAndDateBetween(String agentId, LocalDate start, LocalDate end);
}
