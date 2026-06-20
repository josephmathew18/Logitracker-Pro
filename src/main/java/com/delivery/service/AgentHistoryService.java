package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentHistoryService {

    private final AgentRepository agentRepository;
    private final AgentVehicleHistoryRepository agentVehicleHistoryRepository;
    private final DeliveryRepository deliveryRepository;
    private final FuelExpenseRepository fuelExpenseRepository;
    private final SalaryRepository salaryRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final FeedbackRepository feedbackRepository;
    private final DocumentRepository documentRepository;
    private final AgentActivityLogRepository agentActivityLogRepository;

    public AgentHistoryService(AgentRepository agentRepository,
                               AgentVehicleHistoryRepository agentVehicleHistoryRepository,
                               DeliveryRepository deliveryRepository,
                               FuelExpenseRepository fuelExpenseRepository,
                               SalaryRepository salaryRepository,
                               AttendanceRepository attendanceRepository,
                               LeaveApplicationRepository leaveApplicationRepository,
                               FeedbackRepository feedbackRepository,
                               DocumentRepository documentRepository,
                               AgentActivityLogRepository agentActivityLogRepository) {
        this.agentRepository = agentRepository;
        this.agentVehicleHistoryRepository = agentVehicleHistoryRepository;
        this.deliveryRepository = deliveryRepository;
        this.fuelExpenseRepository = fuelExpenseRepository;
        this.salaryRepository = salaryRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.feedbackRepository = feedbackRepository;
        this.documentRepository = documentRepository;
        this.agentActivityLogRepository = agentActivityLogRepository;
    }

    public static class TimelineEvent {
        private LocalDateTime timestamp;
        private String action;
        private String description;
        private String icon;
        private String type;

        public TimelineEvent(LocalDateTime timestamp, String action, String description, String icon, String type) {
            this.timestamp = timestamp;
            this.action = action;
            this.description = description;
            this.icon = icon;
            this.type = type;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getAction() {
            return action;
        }

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return icon;
        }

        public String getType() {
            return type;
        }
    }

    public static class AgentSummary {
        private long totalDeliveries;
        private long completedDeliveries;
        private long cancelledDeliveries;
        private BigDecimal totalSalaryPaid;
        private BigDecimal totalFuelReimbursement;
        private double averageRating;
        private double attendancePercentage;

        public AgentSummary() {}

        public long getTotalDeliveries() { return totalDeliveries; }
        public void setTotalDeliveries(long totalDeliveries) { this.totalDeliveries = totalDeliveries; }

        public long getCompletedDeliveries() { return completedDeliveries; }
        public void setCompletedDeliveries(long completedDeliveries) { this.completedDeliveries = completedDeliveries; }

        public long getCancelledDeliveries() { return cancelledDeliveries; }
        public void setCancelledDeliveries(long cancelledDeliveries) { this.cancelledDeliveries = cancelledDeliveries; }

        public BigDecimal getTotalSalaryPaid() { return totalSalaryPaid; }
        public void setTotalSalaryPaid(BigDecimal totalSalaryPaid) { this.totalSalaryPaid = totalSalaryPaid; }

        public BigDecimal getTotalFuelReimbursement() { return totalFuelReimbursement; }
        public void setTotalFuelReimbursement(BigDecimal totalFuelReimbursement) { this.totalFuelReimbursement = totalFuelReimbursement; }

        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

        public double getAttendancePercentage() { return attendancePercentage; }
        public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }
    }

    /**
     * Aggregates all events of an agent into a sorted chronological timeline.
     */
    public List<TimelineEvent> getAgentTimeline(String agentId) {
        List<TimelineEvent> events = new ArrayList<>();
        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) return events;

        // 1. Vehicle Assignments/Unassignments
        List<AgentVehicleHistory> vehicleHistories = agentVehicleHistoryRepository.findByAgentIdOrderByAssignedAtDesc(agentId);
        for (AgentVehicleHistory vh : vehicleHistories) {
            events.add(new TimelineEvent(
                    vh.getAssignedAt(),
                    "Vehicle Assigned",
                    "Vehicle " + vh.getVehicleNumber() + " assigned to agent by " + vh.getAssignedBy() + ".",
                    "bi-link-45deg text-primary",
                    "vehicle"
            ));
            if (vh.getUnassignedAt() != null) {
                events.add(new TimelineEvent(
                        vh.getUnassignedAt(),
                        "Vehicle Unassigned",
                        "Vehicle " + vh.getVehicleNumber() + " unassigned from agent.",
                        "bi-unlink text-secondary",
                        "vehicle"
                ));
            }
        }

        // 2. Deliveries
        List<Delivery> deliveries = deliveryRepository.findByAgent(agent);
        for (Delivery d : deliveries) {
            if (d.getAssignedTime() != null) {
                events.add(new TimelineEvent(
                        d.getAssignedTime(),
                        "Delivery Assigned",
                        "Delivery of '" + d.getPackageDetails() + "' to '" + d.getDeliveryAddress() + "' assigned. Status: " + d.getStatus() + ".",
                        "bi-box-seam text-info",
                        "delivery"
                ));
            }
            if ("DELIVERED".equals(d.getStatus()) && d.getDeliveryTime() != null) {
                events.add(new TimelineEvent(
                        d.getDeliveryTime(),
                        "Delivery Completed",
                        "Delivery of '" + d.getPackageDetails() + "' to '" + d.getDeliveryAddress() + "' completed successfully.",
                        "bi-check-circle-fill text-success",
                        "delivery"
                ));
            } else if ("REJECTED".equals(d.getStatus())) {
                LocalDateTime rejectTime = d.getDeliveryTime() != null ? d.getDeliveryTime() : d.getCreatedAt();
                events.add(new TimelineEvent(
                        rejectTime,
                        "Delivery Cancelled/Rejected",
                        "Delivery of '" + d.getPackageDetails() + "' to '" + d.getDeliveryAddress() + "' was rejected or cancelled.",
                        "bi-x-circle-fill text-danger",
                        "delivery"
                ));
            }
        }

        // 3. Fuel Claim Submissions
        List<FuelExpense> fuelExpenses = fuelExpenseRepository.findByAgent(agent);
        for (FuelExpense fe : fuelExpenses) {
            events.add(new TimelineEvent(
                    fe.getCreatedAt(),
                    "Fuel Claim Submitted",
                    "Fuel reimbursement claim for " + fe.getQuantity() + " Litres (Total: ₹" + fe.getTotal() + ") submitted.",
                    "bi-fuel-pump text-warning",
                    "fuel"
            ));
        }

        // 4. Salary Generated
        List<Salary> salaries = salaryRepository.findByAgentId(agentId);
        for (Salary s : salaries) {
            LocalDateTime salTime = s.getPaymentDate() != null ? s.getPaymentDate() : LocalDateTime.now();
            events.add(new TimelineEvent(
                    salTime,
                    "Salary Slip Generated (" + s.getSalaryStatus() + ")",
                    "Net Salary of ₹" + s.getNetSalary() + " generated for " + s.getMonth() + " " + s.getYear() + ".",
                    "bi-cash-coin text-success",
                    "salary"
            ));
        }

        // 5. Leave requested / Approved / Rejected
        List<LeaveApplication> leaves = leaveApplicationRepository.findByAgentIdOrderByAppliedAtDesc(agentId);
        for (LeaveApplication l : leaves) {
            events.add(new TimelineEvent(
                    l.getAppliedAt(),
                    "Leave Requested (" + l.getStatus() + ")",
                    l.getLeaveType() + " leave requested from " + l.getStartDate() + " to " + l.getEndDate() + ". Reason: " + l.getReason() + ".",
                    "bi-calendar-minus text-danger",
                    "leave"
            ));
        }

        // 6. Compliance Documents Uploaded
        List<Document> docs = documentRepository.findByAgentId(agentId);
        for (Document doc : docs) {
            events.add(new TimelineEvent(
                    doc.getUploadedAt(),
                    "Document Uploaded",
                    doc.getDocumentType() + " document '" + doc.getFileName() + "' uploaded. Verification status: " + doc.getVerificationStatus() + ".",
                    "bi-file-earmark-medical text-primary",
                    "document"
            ));
        }

        // 7. Login Activities
        List<AgentActivityLog> activities = agentActivityLogRepository.findByAgentIdOrderByTimestampDesc(agentId);
        for (AgentActivityLog act : activities) {
            events.add(new TimelineEvent(
                    act.getTimestamp(),
                    act.getAction(),
                    "Login recorded from IP Address: " + act.getIpAddress(),
                    "bi-box-arrow-in-right text-dark",
                    "login"
            ));
        }

        // Sort descending by timestamp
        events.sort((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()));
        return events;
    }

    /**
     * Compiles summary card metrics for an agent.
     */
    public AgentSummary getAgentSummary(String agentId) {
        AgentSummary summary = new AgentSummary();
        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent == null) return summary;

        // Deliveries metrics
        List<Delivery> deliveries = deliveryRepository.findByAgent(agent);
        summary.setTotalDeliveries(deliveries.size());
        summary.setCompletedDeliveries(deliveries.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count());
        summary.setCancelledDeliveries(deliveries.stream().filter(d -> "REJECTED".equals(d.getStatus())).count());

        // Salaries metrics (Paid Net Salary)
        List<Salary> salaries = salaryRepository.findByAgentId(agentId);
        BigDecimal totalSalary = salaries.stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getSalaryStatus()))
                .map(Salary::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalSalaryPaid(totalSalary);

        // Fuel Reimbursements metrics
        List<FuelExpense> fuelExpenses = fuelExpenseRepository.findByAgent(agent);
        BigDecimal totalFuel = fuelExpenses.stream()
                .map(FuelExpense::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalFuelReimbursement(totalFuel);

        // Feedback ratings metrics
        List<Feedback> feedbacks = feedbackRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
        double avgRating = feedbacks.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
        summary.setAverageRating(Math.round(avgRating * 100.0) / 100.0);

        // Attendance Percentage
        List<Attendance> attendanceList = attendanceRepository.findByAgentIdOrderByDateDesc(agentId);
        long presentDays = attendanceList.stream()
                .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()) || "LATE".equalsIgnoreCase(a.getStatus()))
                .count();
        double attendancePercent = attendanceList.isEmpty() ? 0.0 : (presentDays * 100.0) / attendanceList.size();
        summary.setAttendancePercentage(Math.round(attendancePercent * 100.0) / 100.0);

        return summary;
    }

    /**
     * Helper to retrieve all records of an agent for granular reports.
     */
    public List<Agent> getFilteredAgents(String search, String status, String startDateStr, String endDateStr) {
        List<Agent> agents = agentRepository.findAll();

        if (search != null && !search.trim().isEmpty()) {
            String q = search.toLowerCase();
            agents = agents.stream().filter(a ->
                    a.getId().toLowerCase().contains(q) ||
                    a.getName().toLowerCase().contains(q) ||
                    a.getPhone().contains(q) ||
                    (a.getVehicle() != null && a.getVehicle().getVehicleNumber().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            agents = agents.stream()
                    .filter(a -> a.getStatus().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }

        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            LocalDate start = LocalDate.parse(startDateStr);
            agents = agents.stream()
                    .filter(a -> a.getJoiningDate() != null && !a.getJoiningDate().toLocalDate().isBefore(start))
                    .collect(Collectors.toList());
        }

        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            LocalDate end = LocalDate.parse(endDateStr);
            agents = agents.stream()
                    .filter(a -> a.getJoiningDate() != null && !a.getJoiningDate().toLocalDate().isAfter(end))
                    .collect(Collectors.toList());
        }

        return agents;
    }

    /**
     * Generates CSV for Monthly Report.
     */
    public String generateMonthlyReportCsv(String agentId, int year, int month) {
        Agent agent = agentRepository.findById(agentId).orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        List<Delivery> deliveries = deliveryRepository.findByAgent(agent).stream()
                .filter(d -> d.getCreatedAt().getYear() == year && d.getCreatedAt().getMonthValue() == month)
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Monthly Agent Report for ").append(agent.getName()).append(" (ID: ").append(agentId).append(")\n");
        csv.append("Month/Year: ").append(month).append("/").append(year).append("\n\n");
        csv.append("Delivery ID,Customer Name,Pickup Address,Delivery Address,Package,Status,Cost (₹),Created At\n");

        for (Delivery d : deliveries) {
            csv.append(d.getId()).append(",")
               .append(escapeCsv(d.getCustomer().getName())).append(",")
               .append(escapeCsv(d.getPickupAddress())).append(",")
               .append(escapeCsv(d.getDeliveryAddress())).append(",")
               .append(escapeCsv(d.getPackageDetails())).append(",")
               .append(d.getStatus()).append(",")
               .append(d.getTotalCost()).append(",")
               .append(d.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        return csv.toString();
    }

    /**
     * Generates CSV for Performance Report.
     */
    public String generatePerformanceReportCsv(String agentId) {
        Agent agent = agentRepository.findById(agentId).orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        List<Feedback> feedbacks = feedbackRepository.findByAgentIdOrderByCreatedAtDesc(agentId);

        StringBuilder csv = new StringBuilder();
        csv.append("Performance & Feedback Report for ").append(agent.getName()).append(" (ID: ").append(agentId).append(")\n\n");
        csv.append("Feedback ID,Customer Name,Rating (Stars),Category,Comments,Date\n");

        for (Feedback f : feedbacks) {
            csv.append(f.getFeedbackId()).append(",")
               .append(escapeCsv(f.getCustomer().getName())).append(",")
               .append(f.getRating()).append(",")
               .append(escapeCsv(f.getCategory())).append(",")
               .append(escapeCsv(f.getComments())).append(",")
               .append(f.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        }
        return csv.toString();
    }

    /**
     * Generates CSV for Attendance Report.
     */
    public String generateAttendanceReportCsv(String agentId) {
        Agent agent = agentRepository.findById(agentId).orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        List<Attendance> attendances = attendanceRepository.findByAgentIdOrderByDateDesc(agentId);

        StringBuilder csv = new StringBuilder();
        csv.append("Attendance Report for ").append(agent.getName()).append(" (ID: ").append(agentId).append(")\n\n");
        csv.append("Date,Check-In Time,Check-Out Time,Working Hours,Status,Late Arrival\n");

        for (Attendance a : attendances) {
            String checkIn = a.getCheckInTime() != null ? a.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A";
            String checkOut = a.getCheckOutTime() != null ? a.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A";
            csv.append(a.getDate().toString()).append(",")
               .append(checkIn).append(",")
               .append(checkOut).append(",")
               .append(a.getWorkingHours()).append(",")
               .append(a.getStatus()).append(",")
               .append(a.isLate() ? "YES" : "NO").append("\n");
        }
        return csv.toString();
    }

    /**
     * Generates CSV for Salary Report.
     */
    public String generateSalaryReportCsv(String agentId) {
        Agent agent = agentRepository.findById(agentId).orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        List<Salary> salaries = salaryRepository.findByAgentId(agentId);

        StringBuilder csv = new StringBuilder();
        csv.append("Salary Disbursement Report for ").append(agent.getName()).append(" (ID: ").append(agentId).append(")\n\n");
        csv.append("Salary ID,Month,Year,Basic Salary (₹),Incentive (₹),Bonus (₹),Fuel (₹),Deductions (₹),Net Salary (₹),Status,Payment Date\n");

        for (Salary s : salaries) {
            String payDate = s.getPaymentDate() != null ? s.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A";
            csv.append(s.getSalaryId()).append(",")
               .append(s.getMonth()).append(",")
               .append(s.getYear()).append(",")
               .append(s.getBasicSalary()).append(",")
               .append(s.getIncentive()).append(",")
               .append(s.getBonus()).append(",")
               .append(s.getFuelReimbursement()).append(",")
               .append(s.getDeductions()).append(",")
               .append(s.getNetSalary()).append(",")
               .append(s.getSalaryStatus()).append(",")
               .append(payDate).append("\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
