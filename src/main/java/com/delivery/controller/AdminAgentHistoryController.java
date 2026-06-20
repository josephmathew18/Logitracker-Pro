package com.delivery.controller;

import com.delivery.model.*;
import com.delivery.service.*;
import com.delivery.repository.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/agents/history")
public class AdminAgentHistoryController {

    private final AgentService agentService;
    private final AgentHistoryService agentHistoryService;
    private final VehicleService vehicleService;
    private final DeliveryRepository deliveryRepository;
    private final FuelExpenseRepository fuelExpenseRepository;
    private final SalaryRepository salaryRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final FeedbackRepository feedbackRepository;
    private final DocumentRepository documentRepository;

    public AdminAgentHistoryController(AgentService agentService,
                                       AgentHistoryService agentHistoryService,
                                       VehicleService vehicleService,
                                       DeliveryRepository deliveryRepository,
                                       FuelExpenseRepository fuelExpenseRepository,
                                       SalaryRepository salaryRepository,
                                       AttendanceRepository attendanceRepository,
                                       LeaveApplicationRepository leaveApplicationRepository,
                                       FeedbackRepository feedbackRepository,
                                       DocumentRepository documentRepository) {
        this.agentService = agentService;
        this.agentHistoryService = agentHistoryService;
        this.vehicleService = vehicleService;
        this.deliveryRepository = deliveryRepository;
        this.fuelExpenseRepository = fuelExpenseRepository;
        this.salaryRepository = salaryRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.feedbackRepository = feedbackRepository;
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public String viewHistoryDashboard(@RequestParam(value = "search", required = false) String search,
                                       @RequestParam(value = "status", required = false) String status,
                                       @RequestParam(value = "startDate", required = false) String startDate,
                                       @RequestParam(value = "endDate", required = false) String endDate,
                                       Model model) {
        List<Agent> agents = agentHistoryService.getFilteredAgents(search, status, startDate, endDate);
        model.addAttribute("agents", agents);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin/agent_history";
    }

    @GetMapping("/{agentId}")
    public String viewAgentDetailHistory(@PathVariable("agentId") String agentId, Model model) {
        Agent agent = agentService.getAgentById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));
        
        model.addAttribute("agent", agent);
        model.addAttribute("summary", agentHistoryService.getAgentSummary(agentId));
        model.addAttribute("timeline", agentHistoryService.getAgentTimeline(agentId));
        
        // Tab Details
        model.addAttribute("vehicleHistory", agentHistoryService.getAgentTimeline(agentId).stream()
                .filter(e -> "vehicle".equals(e.getType()))
                .collect(java.util.stream.Collectors.toList()));
        model.addAttribute("deliveries", deliveryRepository.findByAgent(agent));
        model.addAttribute("fuelExpenses", fuelExpenseRepository.findByAgent(agent));
        model.addAttribute("salaries", salaryRepository.findByAgentId(agentId));
        model.addAttribute("attendances", attendanceRepository.findByAgentIdOrderByDateDesc(agentId));
        model.addAttribute("leaves", leaveApplicationRepository.findByAgentIdOrderByAppliedAtDesc(agentId));
        model.addAttribute("feedbacks", feedbackRepository.findByAgentIdOrderByCreatedAtDesc(agentId));
        model.addAttribute("documents", documentRepository.findByAgentId(agentId));
        model.addAttribute("activities", agentService.getActivityLogs(agentId));
        
        // Year & Month for Reports Selection
        LocalDate now = LocalDate.now();
        model.addAttribute("currentYear", now.getYear());
        model.addAttribute("currentMonth", now.getMonthValue());

        return "admin/agent_history_detail";
    }

    @GetMapping("/{agentId}/report/monthly")
    public ResponseEntity<byte[]> downloadMonthlyReport(@PathVariable("agentId") String agentId,
                                                         @RequestParam("year") int year,
                                                         @RequestParam("month") int month) {
        String csv = agentHistoryService.generateMonthlyReportCsv(agentId, year, month);
        byte[] output = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Monthly_Report_" + agentId + "_" + year + "_" + month + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }

    @GetMapping("/{agentId}/report/performance")
    public ResponseEntity<byte[]> downloadPerformanceReport(@PathVariable("agentId") String agentId) {
        String csv = agentHistoryService.generatePerformanceReportCsv(agentId);
        byte[] output = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Performance_Report_" + agentId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }

    @GetMapping("/{agentId}/report/attendance")
    public ResponseEntity<byte[]> downloadAttendanceReport(@PathVariable("agentId") String agentId) {
        String csv = agentHistoryService.generateAttendanceReportCsv(agentId);
        byte[] output = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Attendance_Report_" + agentId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }

    @GetMapping("/{agentId}/report/salary")
    public ResponseEntity<byte[]> downloadSalaryReport(@PathVariable("agentId") String agentId) {
        String csv = agentHistoryService.generateSalaryReportCsv(agentId);
        byte[] output = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Salary_Report_" + agentId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }

    // Excel Export via Excel-compatible CSV
    @GetMapping("/{agentId}/report/excel")
    public ResponseEntity<byte[]> downloadExcelReport(@PathVariable("agentId") String agentId) {
        StringBuilder sb = new StringBuilder();
        Agent agent = agentService.getAgentById(agentId).orElse(null);
        if (agent != null) {
            sb.append("FULL AGENT PROFILE HISTORY REPORT\n");
            sb.append("Agent ID: ,").append(agent.getId()).append("\n");
            sb.append("Agent Name: ,").append(agent.getName()).append("\n");
            sb.append("Phone Number: ,").append(agent.getPhone()).append("\n");
            sb.append("Email: ,").append(agent.getEmail()).append("\n");
            sb.append("Status: ,").append(agent.getStatus().name()).append("\n\n");
            
            sb.append("------------------------------------------\n");
            sb.append("ATTENDANCE HISTORY\n");
            sb.append(agentHistoryService.generateAttendanceReportCsv(agentId)).append("\n\n");
            
            sb.append("------------------------------------------\n");
            sb.append("SALARY HISTORY\n");
            sb.append(agentHistoryService.generateSalaryReportCsv(agentId)).append("\n\n");
            
            sb.append("------------------------------------------\n");
            sb.append("PERFORMANCE & RATINGS\n");
            sb.append(agentHistoryService.generatePerformanceReportCsv(agentId)).append("\n\n");
        }
        
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] csvBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] output = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, output, 0, bom.length);
        System.arraycopy(csvBytes, 0, output, bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Full_History_" + agentId + ".xls")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(output);
    }
}
