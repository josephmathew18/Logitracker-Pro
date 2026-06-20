package com.delivery.controller;

import com.delivery.model.*;
import com.delivery.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.delivery.dto.AgentDocumentStatusDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminModuleController {

    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final VehicleMaintenanceService vehicleMaintenanceService;
    private final AgentPerformanceService agentPerformanceService;
    private final AnalyticsService analyticsService;
    private final AuditLogService auditLogService;
    private final DocumentService documentService;
    private final AgentService agentService;
    private final VehicleService vehicleService;

    public AdminModuleController(AttendanceService attendanceService,
                                 LeaveService leaveService,
                                 VehicleMaintenanceService vehicleMaintenanceService,
                                 AgentPerformanceService agentPerformanceService,
                                 AnalyticsService analyticsService,
                                 AuditLogService auditLogService,
                                 DocumentService documentService,
                                 AgentService agentService,
                                 VehicleService vehicleService) {
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.vehicleMaintenanceService = vehicleMaintenanceService;
        this.agentPerformanceService = agentPerformanceService;
        this.analyticsService = analyticsService;
        this.auditLogService = auditLogService;
        this.documentService = documentService;
        this.agentService = agentService;
        this.vehicleService = vehicleService;
    }

    // ==========================================
    // 1. Attendance Management
    // ==========================================
    @GetMapping("/attendance")
    public String attendanceDashboard(@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      Model model) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        model.addAttribute("selectedDate", targetDate);
        List<Attendance> dailyAttendance = attendanceService.getDailyAttendance(targetDate);
        long lateCount = dailyAttendance.stream().filter(Attendance::isLate).count();
        model.addAttribute("dailyAttendance", dailyAttendance);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("agents", agentService.getAllAgents());
        return "admin/attendance";
    }

    @GetMapping("/attendance/report")
    public String attendanceMonthlyReport(@RequestParam("agentId") String agentId,
                                          @RequestParam("year") int year,
                                          @RequestParam("month") int month,
                                          Model model) {
        model.addAttribute("reportAgent", agentService.getAgentById(agentId).orElse(null));
        model.addAttribute("records", attendanceService.getMonthlyReport(agentId, year, month));
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        List<Attendance> dailyAttendance = attendanceService.getDailyAttendance(LocalDate.now());
        long lateCount = dailyAttendance.stream().filter(Attendance::isLate).count();
        model.addAttribute("dailyAttendance", dailyAttendance);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("agents", agentService.getAllAgents());
        model.addAttribute("selectedDate", LocalDate.now());
        return "admin/attendance";
    }

    // ==========================================
    // 2. Leave Management
    // ==========================================
    @GetMapping("/leaves")
    public String leaveDashboard(Model model) {
        model.addAttribute("pendingLeaves", leaveService.getPendingApplications());
        model.addAttribute("allLeaves", leaveService.getAllApplications());
        model.addAttribute("agents", agentService.getAllAgents());
        return "admin/leaves";
    }

    @PostMapping("/leaves/approve/{id}")
    public String approveLeave(@PathVariable("id") Integer id,
                               @RequestParam(value = "remarks", defaultValue = "") String remarks,
                               RedirectAttributes redirectAttributes) {
        try {
            leaveService.approveLeave(id, remarks);
            auditLogService.log("admin", "ADMIN", "LEAVE_APPROVAL", "Approved leave application ID: " + id + ". Remarks: " + remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Leave request approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval failed: " + e.getMessage());
        }
        return "redirect:/admin/leaves";
    }

    @PostMapping("/leaves/reject/{id}")
    public String rejectLeave(@PathVariable("id") Integer id,
                              @RequestParam(value = "remarks", defaultValue = "") String remarks,
                              RedirectAttributes redirectAttributes) {
        try {
            leaveService.rejectLeave(id, remarks);
            auditLogService.log("admin", "ADMIN", "LEAVE_REJECTION", "Rejected leave application ID: " + id + ". Remarks: " + remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Leave request rejected successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rejection failed: " + e.getMessage());
        }
        return "redirect:/admin/leaves";
    }

    // ==========================================
    // 3. Vehicle Maintenance Management
    // ==========================================
    @GetMapping("/maintenance")
    public String maintenanceDashboard(Model model) {
        model.addAttribute("allMaintenance", vehicleMaintenanceService.getAllMaintenance());
        model.addAttribute("activeMaintenance", vehicleMaintenanceService.getActiveMaintenance());
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        model.addAttribute("vehicles", vehicles);
        long vehiclesInMaintenance = vehicles.stream()
                .filter(v -> "MAINTENANCE".equalsIgnoreCase(v.getStatus()))
                .count();
        model.addAttribute("vehiclesInMaintenance", vehiclesInMaintenance);
        return "admin/maintenance";
    }

    @PostMapping("/maintenance/schedule")
    public String scheduleMaintenance(@RequestParam("vehicleId") Integer vehicleId,
                                      @RequestParam("scheduledDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam("remarks") String remarks,
                                      RedirectAttributes redirectAttributes) {
        try {
            vehicleMaintenanceService.scheduleMaintenance(vehicleId, date, remarks);
            auditLogService.log("admin", "ADMIN", "SCHEDULE_MAINTENANCE", "Scheduled maintenance for vehicle ID " + vehicleId + " on " + date);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle maintenance scheduled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Scheduling failed: " + e.getMessage());
        }
        return "redirect:/admin/maintenance";
    }

    @PostMapping("/maintenance/complete/{id}")
    public String completeMaintenance(@PathVariable("id") Integer id,
                                      @RequestParam("completedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam("maintenanceCost") BigDecimal cost,
                                      @RequestParam("remarks") String remarks,
                                      RedirectAttributes redirectAttributes) {
        try {
            vehicleMaintenanceService.completeMaintenance(id, date, cost, remarks);
            auditLogService.log("admin", "ADMIN", "COMPLETE_MAINTENANCE", "Completed maintenance log ID " + id + " with cost: ₹" + cost);
            redirectAttributes.addFlashAttribute("successMessage", "Maintenance marked as completed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Action failed: " + e.getMessage());
        }
        return "redirect:/admin/maintenance";
    }

    // ==========================================
    // 4. Agent Performance
    // ==========================================
    @GetMapping("/performance")
    public String performanceDashboard(Model model) {
        LocalDate now = LocalDate.now();
        List<AgentPerformance> leaderboard = agentPerformanceService.generateLeaderboard(now.getMonthValue(), now.getYear());
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("month", now.getMonthValue());
        model.addAttribute("year", now.getYear());
        return "admin/performance";
    }

    @GetMapping("/performance/filter")
    public String filterPerformance(@RequestParam("month") int month,
                                    @RequestParam("year") int year,
                                    Model model) {
        List<AgentPerformance> leaderboard = agentPerformanceService.generateLeaderboard(month, year);
        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("month", month);
        model.addAttribute("year", year);
        return "admin/performance";
    }

    // ==========================================
    // 5. Advanced Analytics Dashboard
    // ==========================================
    @GetMapping("/analytics")
    public String analyticsDashboard(Model model) {
        Map<String, Object> financial = analyticsService.getFinancialSummary();
        Map<String, List<Object>> monthly = analyticsService.getMonthlyTrends();
        Map<String, List<Object>> delivery = analyticsService.getDeliveryTrends();
        Map<String, List<Object>> growth = analyticsService.getCustomerGrowth();
        Map<String, Object> util = analyticsService.getVehicleUtilization();

        model.addAttribute("financial", financial);
        model.addAttribute("monthlyTrends", monthly);
        model.addAttribute("deliveryTrends", delivery);
        model.addAttribute("customerGrowth", growth);
        model.addAttribute("utilization", util);
        return "admin/analytics";
    }

    // ==========================================
    // 6. Notification Center (View Logs)
    // ==========================================
    @GetMapping("/audit-logs")
    public String auditLogDashboard(Model model) {
        model.addAttribute("logs", auditLogService.getAllLogs());
        return "admin/audit_logs";
    }

    // ==========================================
    // 7. Document Management
    // ==========================================
    @GetMapping("/documents")
    public String documentDashboard(@RequestParam(value = "search", required = false) String search,
                                    @RequestParam(value = "filter", required = false) String filter,
                                    Model model) {
        List<Agent> agents = agentService.getAllAgents();
        java.util.List<AgentDocumentStatusDTO> agentStatusList = new java.util.ArrayList<>();

        int totalDocs = 0;
        int pendingDocs = 0;
        int approvedDocs = 0;
        int rejectedDocs = 0;
        int expiringSoonDocs = 0;

        LocalDate today = LocalDate.now();

        for (Agent agent : agents) {
            AgentDocumentStatusDTO dto = documentService.getAgentDocumentStatus(agent);
            
            totalDocs += dto.getTotalDocs();
            pendingDocs += dto.getPendingDocs();
            approvedDocs += dto.getApprovedDocs();
            rejectedDocs += dto.getRejectedDocs();
            expiringSoonDocs += dto.getExpiringSoonDocs();

            boolean matchesSearch = true;
            if (search != null && !search.trim().isEmpty()) {
                String term = search.trim().toLowerCase();
                matchesSearch = agent.getName().toLowerCase().contains(term) || agent.getId().toLowerCase().contains(term);
            }

            boolean matchesFilter = true;
            if (filter != null && !filter.trim().isEmpty()) {
                if ("PENDING".equalsIgnoreCase(filter)) {
                    matchesFilter = dto.getPendingDocs() > 0;
                } else if ("APPROVED".equalsIgnoreCase(filter)) {
                    matchesFilter = "APPROVED".equalsIgnoreCase(dto.getDocumentStatus());
                } else if ("REJECTED".equalsIgnoreCase(filter)) {
                    matchesFilter = dto.getRejectedDocs() > 0;
                } else if ("EXPIRED".equalsIgnoreCase(filter)) {
                    matchesFilter = dto.getExpiredDocs() > 0;
                }
            }

            if (matchesSearch && matchesFilter) {
                agentStatusList.add(dto);
            }
        }

        model.addAttribute("agentStatusList", agentStatusList);
        model.addAttribute("totalDocs", totalDocs);
        model.addAttribute("pendingDocs", pendingDocs);
        model.addAttribute("approvedDocs", approvedDocs);
        model.addAttribute("rejectedDocs", rejectedDocs);
        model.addAttribute("expiringSoonDocs", expiringSoonDocs);
        model.addAttribute("search", search);
        model.addAttribute("filter", filter);

        return "admin/documents";
    }

    @GetMapping("/documents/agent/{agentId}")
    public String agentDocuments(@PathVariable("agentId") String agentId, Model model) {
        Agent agent = agentService.getAgentById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        List<Document> allDocs = documentService.getAgentDocuments(agentId);
        if (agent.getVehicle() != null) {
            List<Document> vehicleDocs = documentService.getVehicleDocuments(agent.getVehicle().getId());
            for (Document vd : vehicleDocs) {
                if (allDocs.stream().noneMatch(d -> d.getId().equals(vd.getId()))) {
                    allDocs.add(vd);
                }
            }
        }

        Document identityProof = null;
        Document drivingLicense = null;
        Document vehicleRc = null;
        Document insurance = null;
        Document pollutionCert = null;

        for (Document doc : allDocs) {
            if ("IDENTITY_PROOF".equalsIgnoreCase(doc.getDocumentType())) {
                identityProof = doc;
            } else if ("DRIVING_LICENSE".equalsIgnoreCase(doc.getDocumentType())) {
                drivingLicense = doc;
            } else if ("VEHICLE_RC".equalsIgnoreCase(doc.getDocumentType())) {
                vehicleRc = doc;
            } else if ("INSURANCE".equalsIgnoreCase(doc.getDocumentType())) {
                insurance = doc;
            } else if ("POLLUTION_CERT".equalsIgnoreCase(doc.getDocumentType())) {
                pollutionCert = doc;
            }
        }

        AgentDocumentStatusDTO summary = documentService.getAgentDocumentStatus(agent);

        model.addAttribute("agent", agent);
        model.addAttribute("identityProof", identityProof);
        model.addAttribute("drivingLicense", drivingLicense);
        model.addAttribute("vehicleRc", vehicleRc);
        model.addAttribute("insurance", insurance);
        model.addAttribute("pollutionCert", pollutionCert);
        model.addAttribute("summary", summary);
        model.addAttribute("today", LocalDate.now());

        return "admin/agent_documents";
    }

    @PostMapping("/documents/verify/{id}")
    public String verifyDocument(@PathVariable("id") Integer id,
                                 @RequestParam("status") String status,
                                 @RequestParam(value = "remarks", defaultValue = "") String remarks,
                                 RedirectAttributes redirectAttributes) {
        try {
            Document doc = documentService.verifyDocument(id, status, remarks);
            auditLogService.log("admin", "ADMIN", "VERIFY_DOCUMENT", "Verified document ID " + id + " as " + status + ". Remarks: " + remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Document verification status updated successfully.");
            
            if (doc.getAgent() != null) {
                return "redirect:/admin/documents/agent/" + doc.getAgent().getId();
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Verification failed: " + e.getMessage());
        }
        return "redirect:/admin/documents";
    }
}
