package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.LeaveApplicationRepository;
import com.delivery.repository.LeaveBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AgentRepository agentRepository;
    private final NotificationService notificationService;

    public LeaveService(LeaveApplicationRepository leaveApplicationRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        AgentRepository agentRepository,
                        NotificationService notificationService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.agentRepository = agentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public LeaveBalance initializeBalance(Agent agent) {
        Optional<LeaveBalance> existing = leaveBalanceRepository.findByAgentId(agent.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        LeaveBalance balance = new LeaveBalance(agent);
        return leaveBalanceRepository.save(balance);
    }

    @Transactional
    public LeaveApplication applyLeave(String agentId, LocalDate startDate, LocalDate endDate, String leaveType, String reason) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after End date.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot apply leave for past dates.");
        }

        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

        // Get or initialize balance
        LeaveBalance balance = leaveBalanceRepository.findByAgentId(agentId)
                .orElseGet(() -> initializeBalance(agent));

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Verify balance
        if ("CASUAL".equalsIgnoreCase(leaveType) && balance.getCasualLeavesLeft() < days) {
            throw new IllegalArgumentException("Insufficient Casual Leave balance. Remaining: " + balance.getCasualLeavesLeft() + " days.");
        } else if ("SICK".equalsIgnoreCase(leaveType) && balance.getSickLeavesLeft() < days) {
            throw new IllegalArgumentException("Insufficient Sick Leave balance. Remaining: " + balance.getSickLeavesLeft() + " days.");
        } else if ("ANNUAL".equalsIgnoreCase(leaveType) && balance.getAnnualLeavesLeft() < days) {
            throw new IllegalArgumentException("Insufficient Annual Leave balance. Remaining: " + balance.getAnnualLeavesLeft() + " days.");
        }

        LeaveApplication leave = new LeaveApplication(agent, startDate, endDate, leaveType, reason);
        return leaveApplicationRepository.save(leave);
    }

    @Transactional
    public LeaveApplication approveLeave(Integer applicationId, String remarks) {
        final LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (!"PENDING".equalsIgnoreCase(leave.getStatus())) {
            throw new IllegalStateException("Leave application is already processed.");
        }

        LeaveBalance balance = leaveBalanceRepository.findByAgentId(leave.getAgent().getId())
                .orElseGet(() -> initializeBalance(leave.getAgent()));

        long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        String type = leave.getLeaveType();

        // Deduct balance
        if ("CASUAL".equalsIgnoreCase(type)) {
            balance.setCasualLeavesLeft((int) (balance.getCasualLeavesLeft() - days));
        } else if ("SICK".equalsIgnoreCase(type)) {
            balance.setSickLeavesLeft((int) (balance.getSickLeavesLeft() - days));
        } else if ("ANNUAL".equalsIgnoreCase(type)) {
            balance.setAnnualLeavesLeft((int) (balance.getAnnualLeavesLeft() - days));
        }
        leaveBalanceRepository.save(balance);

        leave.setStatus("APPROVED");
        leave.setRemarks(remarks);
        LeaveApplication saved = leaveApplicationRepository.save(leave);
        
        // Notify Agent
        notificationService.createNotification(
            leave.getAgent().getUser(), 
            "Leave Application Approved", 
            "Your leave request from " + leave.getStartDate() + " to " + leave.getEndDate() + " has been approved.", 
            "LEAVE"
        );

        return saved;
    }

    @Transactional
    public LeaveApplication rejectLeave(Integer applicationId, String remarks) {
        final LeaveApplication leave = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Leave application not found."));

        if (!"PENDING".equalsIgnoreCase(leave.getStatus())) {
            throw new IllegalStateException("Leave application is already processed.");
        }

        leave.setStatus("REJECTED");
        leave.setRemarks(remarks);
        LeaveApplication saved = leaveApplicationRepository.save(leave);

        // Notify Agent
        notificationService.createNotification(
            leave.getAgent().getUser(), 
            "Leave Application Rejected", 
            "Your leave request from " + leave.getStartDate() + " to " + leave.getEndDate() + " has been rejected. Remarks: " + remarks, 
            "LEAVE"
        );

        return saved;
    }

    public LeaveBalance getBalance(String agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found."));
        return leaveBalanceRepository.findByAgentId(agentId)
                .orElseGet(() -> initializeBalance(agent));
    }

    public List<LeaveApplication> getAgentHistory(String agentId) {
        return leaveApplicationRepository.findByAgentIdOrderByAppliedAtDesc(agentId);
    }

    public List<LeaveApplication> getPendingApplications() {
        return leaveApplicationRepository.findByStatusOrderByAppliedAtDesc("PENDING");
    }

    public List<LeaveApplication> getAllApplications() {
        return leaveApplicationRepository.findAllByOrderByAppliedAtDesc();
    }
}
