package com.delivery.service;

import com.delivery.model.Agent;
import com.delivery.model.Attendance;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.AttendanceRepository;
import com.delivery.event.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AgentRepository agentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AttendanceService(AttendanceRepository attendanceRepository, AgentRepository agentRepository, ApplicationEventPublisher eventPublisher) {
        this.attendanceRepository = attendanceRepository;
        this.agentRepository = agentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Attendance checkIn(String agentId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByAgentIdAndDate(agentId, today);
        if (existing.isPresent()) {
            throw new IllegalStateException("Already checked in for today!");
        }

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        LocalDateTime now = LocalDateTime.now();
        LocalTime lateLimit = LocalTime.of(9, 30);
        boolean isLate = now.toLocalTime().isAfter(lateLimit);

        Attendance attendance = new Attendance();
        attendance.setAgent(agent);
        attendance.setDate(today);
        attendance.setCheckInTime(now);
        attendance.setLate(isLate);
        attendance.setStatus(isLate ? "LATE" : "PRESENT");

        Attendance saved = attendanceRepository.save(attendance);

        // Trigger notifications to Admin
        String agentName = agent.getName();
        String shiftType = agent.getShiftType() != null ? agent.getShiftType() : "Morning";
        eventPublisher.publishEvent(new NotificationEvent(this, "admin", "Agent Shift Started", "Agent " + agentName + " (" + agentId + ") has started their " + shiftType + " shift.", "ATTENDANCE", "MEDIUM"));
        eventPublisher.publishEvent(new NotificationEvent(this, "admin", "Agent Checked In", "Agent " + agentName + " (" + agentId + ") checked in at " + now.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")) + (isLate ? " (LATE)" : "") + ".", "ATTENDANCE", isLate ? "HIGH" : "LOW"));

        return saved;
    }

    @Transactional
    public Attendance checkOut(String agentId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByAgentIdAndDate(agentId, today)
                .orElseThrow(() -> new IllegalStateException("No check-in record found for today. Please check-in first."));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("Already checked out for today!");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutTime(now);

        // Calculate working hours
        long seconds = Duration.between(attendance.getCheckInTime(), now).getSeconds();
        double hours = (double) seconds / 3600.0;
        // Round to 2 decimal places
        hours = Math.round(hours * 100.0) / 100.0;
        attendance.setWorkingHours(hours);

        Attendance saved = attendanceRepository.save(attendance);

        // Trigger notifications to Admin
        Agent agent = attendance.getAgent();
        String agentName = agent.getName();
        String shiftType = agent.getShiftType() != null ? agent.getShiftType() : "Morning";
        eventPublisher.publishEvent(new NotificationEvent(this, "admin", "Agent Shift Ended", "Agent " + agentName + " (" + agentId + ") has ended their " + shiftType + " shift.", "ATTENDANCE", "MEDIUM"));
        eventPublisher.publishEvent(new NotificationEvent(this, "admin", "Agent Checked Out", "Agent " + agentName + " (" + agentId + ") checked out at " + now.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")) + ".", "ATTENDANCE", "LOW"));

        return saved;
    }

    public Optional<Attendance> getTodayAttendance(String agentId) {
        return attendanceRepository.findByAgentIdAndDate(agentId, LocalDate.now());
    }

    public List<Attendance> getAgentHistory(String agentId) {
        return attendanceRepository.findByAgentIdOrderByDateDesc(agentId);
    }

    public List<Attendance> getDailyAttendance(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    public List<Attendance> getMonthlyReport(String agentId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return attendanceRepository.findByAgentIdAndDateBetweenOrderByDateAsc(agentId, start, end);
    }
}
