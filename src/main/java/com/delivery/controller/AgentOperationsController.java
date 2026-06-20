package com.delivery.controller;

import com.delivery.model.*;
import com.delivery.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/agent")
public class AgentOperationsController {

    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final DocumentService documentService;
    private final AgentService agentService;
    private final AuditLogService auditLogService;

    public AgentOperationsController(AttendanceService attendanceService,
                                     LeaveService leaveService,
                                     DocumentService documentService,
                                     AgentService agentService,
                                     AuditLogService auditLogService) {
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.documentService = documentService;
        this.agentService = agentService;
        this.auditLogService = auditLogService;
    }

    // ==========================================
    // 1. Agent Attendance Actions
    // ==========================================
    @GetMapping("/attendance")
    public String attendancePortal(Authentication authentication, Model model) {
        String username = authentication.getName();
        Agent agent = agentService.getAgentByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

        model.addAttribute("agent", agent);
        Optional<Attendance> todayRecord = attendanceService.getTodayAttendance(agent.getId());
        model.addAttribute("todayRecord", todayRecord.orElse(null));
        model.addAttribute("attendanceHistory", attendanceService.getAgentHistory(agent.getId()));
        return "agent/attendance";
    }

    @PostMapping("/attendance/check-in")
    public String checkIn(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Agent agent = agentService.getAgentByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

            Attendance att = attendanceService.checkIn(agent.getId());
            String statusMsg = att.isLate() ? "Checked in late at " + att.getCheckInTime().toLocalTime() : "Checked in successfully.";
            auditLogService.log(agent.getId(), "AGENT", "ATTENDANCE_CHECKIN", statusMsg);
            redirectAttributes.addFlashAttribute("successMessage", statusMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/agent/attendance";
    }

    @PostMapping("/attendance/check-out")
    public String checkOut(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Agent agent = agentService.getAgentByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

            Attendance att = attendanceService.checkOut(agent.getId());
            String statusMsg = "Checked out successfully. Working hours: " + att.getWorkingHours() + " hrs.";
            auditLogService.log(agent.getId(), "AGENT", "ATTENDANCE_CHECKOUT", statusMsg);
            redirectAttributes.addFlashAttribute("successMessage", statusMsg);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/agent/attendance";
    }

    // ==========================================
    // 2. Agent Leave Actions
    // ==========================================
    @GetMapping("/leaves")
    public String leavePortal(Authentication authentication, Model model) {
        String username = authentication.getName();
        Agent agent = agentService.getAgentByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

        model.addAttribute("agent", agent);
        model.addAttribute("balances", leaveService.getBalance(agent.getId()));
        model.addAttribute("leaveHistory", leaveService.getAgentHistory(agent.getId()));
        return "agent/leaves";
    }

    @PostMapping("/leaves/apply")
    public String applyLeave(Authentication authentication,
                             @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                             @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                             @RequestParam("leaveType") String leaveType,
                             @RequestParam("reason") String reason,
                             RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Agent agent = agentService.getAgentByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

            leaveService.applyLeave(agent.getId(), start, end, leaveType, reason);
            auditLogService.log(agent.getId(), "AGENT", "LEAVE_APPLICATION", "Applied for " + leaveType + " leave (" + start + " to " + end + ")");
            redirectAttributes.addFlashAttribute("successMessage", "Leave request submitted successfully. Awaiting Admin verification.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/agent/leaves";
    }

    // ==========================================
    // 3. Agent Document Upload Actions
    // ==========================================
    @GetMapping("/documents")
    public String documentPortal(Authentication authentication, Model model) {
        String username = authentication.getName();
        Agent agent = agentService.getAgentByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

        model.addAttribute("agent", agent);
        model.addAttribute("documents", documentService.getAgentDocuments(agent.getId()));
        return "agent/documents";
    }

    @PostMapping("/documents/upload")
    public String uploadDocument(Authentication authentication,
                                 @RequestParam("documentType") String documentType,
                                 @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                 @RequestParam("docFile") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Agent agent = agentService.getAgentByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

            documentService.uploadDocument(agent.getId(), null, documentType, expiryDate, file);
            auditLogService.log(agent.getId(), "AGENT", "DOCUMENT_UPLOAD", "Uploaded document: " + documentType);
            redirectAttributes.addFlashAttribute("successMessage", "Document uploaded successfully and pending approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload document: " + e.getMessage());
        }
        return "redirect:/agent/documents";
    }
}
