package com.delivery.controller;

import com.delivery.model.*;
import com.delivery.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AgentService agentService;
    private final VehicleService vehicleService;
    private final DeliveryService deliveryService;
    private final ExpenseService expenseService;
    private final FeedbackService feedbackService;
    private final SalaryService salaryService;
    private final PaymentService paymentService;
    private final PdfGenerationService pdfGenerationService;
    private final AuditLogService auditLogService;
    private final AttendanceService attendanceService;
    private final DatabaseCleanupService databaseCleanupService;

    public AdminController(AgentService agentService, VehicleService vehicleService,
                           DeliveryService deliveryService, ExpenseService expenseService,
                           FeedbackService feedbackService, SalaryService salaryService,
                           PaymentService paymentService, PdfGenerationService pdfGenerationService,
                           AuditLogService auditLogService, AttendanceService attendanceService,
                           DatabaseCleanupService databaseCleanupService) {
        this.agentService = agentService;
        this.vehicleService = vehicleService;
        this.deliveryService = deliveryService;
        this.expenseService = expenseService;
        this.feedbackService = feedbackService;
        this.salaryService = salaryService;
        this.paymentService = paymentService;
        this.pdfGenerationService = pdfGenerationService;
        this.auditLogService = auditLogService;
        this.attendanceService = attendanceService;
        this.databaseCleanupService = databaseCleanupService;
    }

    @GetMapping("/payments")
    public String paymentsPage(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        model.addAttribute("salaries", salaryService.getAllSalaries());
        model.addAttribute("reports", paymentService.getFinancialReports());
        model.addAttribute("agents", agentService.getAllAgents());
        return "admin/payments";
    }

    @PostMapping("/salaries/generate")
    public String generateSalarySlip(@RequestParam("agentId") String agentId,
                                     @RequestParam("month") String month,
                                     @RequestParam("year") Integer year,
                                     @RequestParam("basicSalary") BigDecimal basicSalary,
                                     @RequestParam("bonus") BigDecimal bonus,
                                     @RequestParam("fuelReimbursement") BigDecimal fuelReimbursement,
                                     @RequestParam("deductions") BigDecimal deductions,
                                     RedirectAttributes redirectAttributes) {
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int selectedMonthVal = getMonthValue(month);
        if (year < currentYear || (year == currentYear && selectedMonthVal < currentMonth)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Salary cannot be generated for past periods.");
            return "redirect:/admin/payments";
        }

        try {
            salaryService.generateMonthlySalarySlip(agentId, month, year, basicSalary, bonus, fuelReimbursement, deductions);
            auditLogService.log("admin", "ADMIN", "GENERATE_SALARY", "Generated salary for Agent " + agentId + " for " + month + "/" + year);
            redirectAttributes.addFlashAttribute("successMessage", "Salary slip generated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to generate salary slip: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/salaries/approve/{id}")
    public String approveSalary(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            salaryService.approveSalaryPayout(id);
            auditLogService.log("admin", "ADMIN", "APPROVE_SALARY", "Approved salary payout ID " + id);
            redirectAttributes.addFlashAttribute("successMessage", "Salary payout approved and marked as PAID.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval failed: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/payments/refund/{id}")
    public String refundPayment(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.refundPayment(id);
            auditLogService.log("admin", "ADMIN", "REFUND_PAYMENT", "Refunded payment ID " + id);
            redirectAttributes.addFlashAttribute("successMessage", "Refund processed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Refund failed: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @PostMapping("/salaries/pay/{id}")
    public String paySalary(@PathVariable("id") Integer id,
                            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
                            @RequestParam(value = "transactionId", required = false) String transactionId,
                            RedirectAttributes redirectAttributes) {
        try {
            if ("CARD".equalsIgnoreCase(paymentMethod)) {
                salaryService.processDirectSalaryPayment(id, "CARD", transactionId);
                redirectAttributes.addFlashAttribute("successMessage", "Salary Payment Verified and Payout Completed!");
                return "redirect:/admin/payments";
            }
            String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            String shortUrl = salaryService.initiateSalaryPaymentLink(id, baseUrl);
            return "redirect:" + shortUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to pay salary: " + e.getMessage());
            return "redirect:/admin/payments";
        }
    }

    @GetMapping("/salaries/callback")
    public String salaryCallback(@RequestParam(value = "salary_id", required = false) Integer salaryId,
                                 @RequestParam(value = "razorpay_payment_link_id", required = false) String paymentLinkId,
                                 @RequestParam(value = "razorpay_payment_id", required = false) String paymentId,
                                 @RequestParam(value = "razorpay_payment_link_status", required = false) String linkStatus,
                                 RedirectAttributes redirectAttributes) {
        try {
            salaryService.verifySalaryCallback(salaryId, paymentLinkId, paymentId, linkStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Salary Payment Verified and Payout Completed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Callback verification failed: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @GetMapping("/reports/download")
    public void downloadTransactionReport(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transaction_report.csv");
        java.io.PrintWriter writer = response.getWriter();
        writer.println("Payment ID,Order ID,Customer Name,Amount,Payment Method,Status,Transaction ID,Payment Date");
        List<Payment> payments = paymentService.getAllPayments();
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Payment p : payments) {
            String formattedDate = p.getPaymentDate() != null ? p.getPaymentDate().format(dtf) : "N/A";
            writer.println(String.format("%d,%d,%s,%s,%s,%s,%s,%s",
                    p.getPaymentId(),
                    p.getOrder().getOrderId(),
                    p.getCustomer().getName(),
                    p.getAmount().toString(),
                    p.getPaymentMethod(),
                    p.getPaymentStatus(),
                    p.getTransactionId() != null ? p.getTransactionId() : "N/A",
                    formattedDate
            ));
        }
        writer.flush();
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = deliveryService.getDashboardStatistics();
        model.addAllAttributes(stats);
        model.addAttribute("recentDeliveries", deliveryService.getAllDeliveries());
        model.addAttribute("recentFeedback", feedbackService.getAllFeedback());

        // Agent-vehicle mapping (list of agents with assigned vehicles)
        List<Agent> agentsWithVehicles = agentService.getAllAgents().stream()
                .filter(a -> a.getVehicle() != null)
                .collect(Collectors.toList());
        model.addAttribute("agentVehicleMappings", agentsWithVehicles);

        // Vehicle Capacity Utilization
        List<Map<String, Object>> capacityUtilizationList = new java.util.ArrayList<>();
        for (Agent agent : agentService.getAllAgents()) {
            Vehicle vehicle = agent.getVehicle();
            if (vehicle != null) {
                // Get active deliveries for this agent
                List<Delivery> active = deliveryService.getDeliveriesByAgent(agent.getUser().getUsername()).stream()
                        .filter(d -> java.util.Arrays.asList("ASSIGNED", "PICKED_UP", "IN_TRANSIT").contains(d.getStatus()))
                        .collect(Collectors.toList());
                
                double activeWeight = 0.0;
                for (Delivery d : active) {
                    Order order = deliveryService.getDeliveryById(d.getId()).flatMap(del -> 
                        paymentService.getAllOrders().stream()
                                .filter(o -> o.getDelivery() != null && o.getDelivery().getId().equals(del.getId()))
                                .findFirst()
                    ).orElse(null);
                    if (order != null && order.getParcelWeight() != null) {
                        activeWeight += order.getParcelWeight().doubleValue();
                    }
                }
                
                double capacity = vehicle.getMaxLoadCapacity() != null ? vehicle.getMaxLoadCapacity() : 0.0;
                double utilizationPercent = capacity > 0 ? (activeWeight / capacity) * 100 : 0.0;
                
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("agentName", agent.getName());
                map.put("profileImage", agent.getProfileImage());
                map.put("vehicleNumber", vehicle.getVehicleNumber());
                map.put("vehicleType", vehicle.getVehicleType());
                map.put("capacity", capacity);
                map.put("activeWeight", activeWeight);
                map.put("utilizationPercent", Math.min(utilizationPercent, 100.0));
                capacityUtilizationList.add(map);
            }
        }
        model.addAttribute("capacityUtilization", capacityUtilizationList);

        // Parcel Weight Reports
        List<Order> allOrders = paymentService.getAllOrders();
        long cat1 = 0, cat2 = 0, cat3 = 0, cat4 = 0, cat5 = 0;
        for (Order o : allOrders) {
            BigDecimal w = o.getParcelWeight();
            if (w != null) {
                double wd = w.doubleValue();
                if (wd >= 0 && wd <= 1) cat1++;
                else if (wd > 1 && wd <= 5) cat2++;
                else if (wd > 5 && wd <= 10) cat3++;
                else if (wd > 10 && wd <= 20) cat4++;
                else if (wd > 20) cat5++;
            }
        }
        Map<String, Long> weightReport = new java.util.HashMap<>();
        weightReport.put("cat0_1", cat1);
        weightReport.put("cat1_5", cat2);
        weightReport.put("cat5_10", cat3);
        weightReport.put("cat10_20", cat4);
        weightReport.put("cat20_plus", cat5);
        model.addAttribute("weightReport", weightReport);

        return "admin/dashboard";
    }

    @PostMapping("/database/reset")
    public String resetDatabase(RedirectAttributes redirectAttributes) {
        try {
            databaseCleanupService.cleanupDatabase();
            auditLogService.log("admin", "ADMIN", "DATABASE_RESET", "Cleaned and reset system database, preserving Agent 1001, Customers, and Admin accounts.");
            redirectAttributes.addFlashAttribute("successMessage", "Database cleaned and reset successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Database reset failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/agents")
    public String agentsPage(Model model) {
        model.addAttribute("agents", agentService.getAllAgents());
        model.addAttribute("vehicles", vehicleService.getAvailableVehicles());
        model.addAttribute("activeAgents", agentService.getActiveAgents());
        return "admin/agents";
    }

    @PostMapping("/agents/register")
    public String registerAgent(@RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("email") String email,
                                @RequestParam("password") String password,
                                RedirectAttributes redirectAttributes) {
        try {
            Agent agent = agentService.registerAgent(name, phone, email, password);
            auditLogService.log("admin", "ADMIN", "REGISTER_AGENT", "Registered Agent: " + name + " (" + agent.getId() + ")");
            
            // Format phone number by keeping only digits
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            
            String messageText = "Welcome to LogiTrack Pro!\n" +
                                 "Your Agent account has been registered successfully.\n\n" +
                                 "Agent ID (Username): " + agent.getId() + "\n" +
                                 "Password: " + password + "\n\n" +
                                 "Please login at: http://localhost:8080/login";
            
            String encodedMessage = java.net.URLEncoder.encode(messageText, java.nio.charset.StandardCharsets.UTF_8.toString());
            String whatsappUrl = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + encodedMessage;
            String smsUrl = "sms:" + cleanPhone + "?body=" + encodedMessage;
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Agent registered successfully! Generated ID: " + agent.getId() + ".");
            redirectAttributes.addFlashAttribute("whatsappUrl", whatsappUrl);
            redirectAttributes.addFlashAttribute("smsUrl", smsUrl);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error registering agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/assign-vehicle")
    public String assignVehicleToAgent(@RequestParam("agentId") String agentId,
                                       @RequestParam("vehicleId") Integer vehicleId,
                                       RedirectAttributes redirectAttributes) {
        try {
            agentService.assignVehicle(agentId, vehicleId);
            auditLogService.log("admin", "ADMIN", "ASSIGN_VEHICLE", "Assigned vehicle ID " + vehicleId + " to Agent " + agentId);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle assigned to agent successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error assigning vehicle: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/unassign-vehicle")
    public String unassignVehicleFromAgent(@RequestParam("agentId") String agentId,
                                           RedirectAttributes redirectAttributes) {
        try {
            agentService.removeVehicleAssignment(agentId);
            auditLogService.log("admin", "ADMIN", "UNASSIGN_VEHICLE", "Unassigned vehicle from Agent " + agentId);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle unassigned from agent successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error unassigning vehicle: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @GetMapping("/agents/{agentId}/vehicle-details")
    @ResponseBody
    public ResponseEntity<?> getAgentVehicleDetails(@PathVariable("agentId") String agentId) {
        try {
            Agent agent = agentService.getAgentById(agentId)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("profileImage", agent.getProfileImage() != null ? agent.getProfileImage() : "/images/default-avatar.png");
            Vehicle vehicle = agent.getVehicle();
            if (vehicle != null) {
                result.put("assigned", true);
                result.put("vehicleNumber", vehicle.getVehicleNumber());
                result.put("vehicleType", vehicle.getVehicleType());
                result.put("fuelType", vehicle.getFuelType());
                result.put("status", vehicle.getStatus());
                result.put("maxLoadCapacity", vehicle.getMaxLoadCapacity());
            } else {
                result.put("assigned", false);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/agents/suspend")
    public String suspendAgent(@RequestParam("agentId") String agentId,
                               @RequestParam(value = "remarks", defaultValue = "") String remarks,
                               RedirectAttributes redirectAttributes) {
        try {
            agentService.updateAgentStatus(agentId, AgentStatus.SUSPENDED, remarks);
            auditLogService.log("admin", "ADMIN", "SUSPEND_AGENT", "Suspended Agent " + agentId + ". Remarks: " + remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Agent " + agentId + " suspended successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to suspend agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/deactivate")
    public String deactivateAgent(@RequestParam("agentId") String agentId,
                                 @RequestParam(value = "remarks", defaultValue = "") String remarks,
                                 RedirectAttributes redirectAttributes) {
        try {
            agentService.updateAgentStatus(agentId, AgentStatus.INACTIVE, remarks);
            auditLogService.log("admin", "ADMIN", "DEACTIVATE_AGENT", "Deactivated Agent " + agentId + ". Remarks: " + remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Agent " + agentId + " deactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to deactivate agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/reactivate")
    public String reactivateAgent(@RequestParam("agentId") String agentId,
                                 RedirectAttributes redirectAttributes) {
        try {
            agentService.updateAgentStatus(agentId, AgentStatus.ACTIVE, "Reactivated by admin");
            auditLogService.log("admin", "ADMIN", "REACTIVATE_AGENT", "Reactivated Agent " + agentId);
            redirectAttributes.addFlashAttribute("successMessage", "Agent " + agentId + " reactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reactivate agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @PostMapping("/agents/terminate")
    public String terminateAgent(@RequestParam("agentId") String agentId,
                                 @RequestParam("reason") String reason,
                                 @RequestParam(value = "reassignToAgentId", required = false) String reassignToAgentId,
                                 RedirectAttributes redirectAttributes) {
        try {
            agentService.terminateAgent(agentId, reason, reassignToAgentId);
            auditLogService.log("admin", "ADMIN", "TERMINATE_AGENT", "Terminated Agent " + agentId + ". Reason: " + reason);
            redirectAttributes.addFlashAttribute("successMessage", "Agent " + agentId + " terminated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to terminate agent: " + e.getMessage());
        }
        return "redirect:/admin/agents";
    }

    @GetMapping("/agents/{agentId}/pending-deliveries")
    @ResponseBody
    public ResponseEntity<?> getPendingDeliveries(@PathVariable("agentId") String agentId) {
        try {
            List<Delivery> pending = deliveryService.getPendingDeliveriesForAgent(agentId);
            List<Map<String, Object>> result = pending.stream().map(d -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", d.getId());
                map.put("pickupAddress", d.getPickupAddress());
                map.put("deliveryAddress", d.getDeliveryAddress());
                map.put("status", d.getStatus());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/agents/{agentId}/audit-log")
    @ResponseBody
    public ResponseEntity<?> getAgentAuditLog(@PathVariable("agentId") String agentId) {
        try {
            List<AgentAuditLog> history = agentService.getAuditHistory(agentId);
            List<Map<String, Object>> result = history.stream().map(log -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("logId", log.getLogId());
                map.put("agentId", log.getAgentId());
                map.put("action", log.getAction());
                map.put("actionByAdmin", log.getActionByAdmin());
                map.put("actionDate", log.getActionDate().toString());
                map.put("remarks", log.getRemarks());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/vehicles")
    public String vehiclesPage(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/vehicles";
    }

    @PostMapping("/vehicles/add")
    public String addVehicle(@ModelAttribute Vehicle vehicle, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.addVehicle(vehicle);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding vehicle: " + e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/vehicles/update/{id}")
    public String updateVehicle(@PathVariable("id") Integer id, @ModelAttribute Vehicle vehicle, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.updateVehicle(id, vehicle);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating vehicle: " + e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @GetMapping("/vehicles/delete/{id}")
    public String deleteVehicle(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            vehicleService.deleteVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting vehicle: " + e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @GetMapping("/assign")
    public String assignPage(Model model) {
        model.addAttribute("pendingDeliveries", deliveryService.getPendingDeliveries());
        
        List<Agent> allAgents = agentService.getAllAgents();
        model.addAttribute("agents", allAgents);
        
        // Filter agents that are AVAILABLE
        List<Agent> availableAgents = allAgents.stream()
                .filter(a -> agentService.getAgentAvailabilityStatus(a) == AgentAvailabilityStatus.AVAILABLE)
                .collect(Collectors.toList());
        model.addAttribute("availableAgents", availableAgents);

        // Build list of availability information maps
        List<Map<String, Object>> agentAvailabilityList = allAgents.stream().map(a -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("agentId", a.getId());
            map.put("agentName", a.getName());
            map.put("shiftType", a.getShiftType());
            map.put("shiftStartTime", a.getShiftStartTime() != null ? a.getShiftStartTime().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")) : "N/A");
            map.put("shiftEndTime", a.getShiftEndTime() != null ? a.getShiftEndTime().format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")) : "N/A");
            
            // Check attendance for today
            Optional<Attendance> attOpt = attendanceService.getTodayAttendance(a.getId());
            String attStatus = attOpt.map(Attendance::getStatus).orElse("ABSENT");
            if (attOpt.isPresent() && attOpt.get().getCheckOutTime() != null) {
                attStatus = "COMPLETED";
            }
            map.put("attendanceStatus", attStatus);
            
            // Availability status
            map.put("availabilityStatus", agentService.getAgentAvailabilityStatus(a).name());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("agentAvailabilityList", agentAvailabilityList);

        model.addAttribute("vehicles", vehicleService.getAllVehicles().stream()
                .filter(v -> !"MAINTENANCE".equalsIgnoreCase(v.getStatus()))
                .collect(Collectors.toList()));
        return "admin/assign";
    }

    @PostMapping("/assign")
    public String assignDeliveryRequest(@RequestParam("deliveryId") Integer deliveryId,
                                        @RequestParam("agentId") String agentId,
                                        @RequestParam(value = "vehicleId", required = false) Integer vehicleId,
                                        RedirectAttributes redirectAttributes) {
        try {
            if (vehicleId != null) {
                deliveryService.assignDelivery(deliveryId, agentId, vehicleId);
            } else {
                deliveryService.assignDelivery(deliveryId, agentId);
            }
            auditLogService.log("admin", "ADMIN", "ASSIGN_DELIVERY", "Assigned delivery ID " + deliveryId + " to Agent " + agentId + (vehicleId != null ? " with vehicle ID " + vehicleId : ""));
            redirectAttributes.addFlashAttribute("successMessage", "Delivery request successfully allocated to agent.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Allocation failed: " + e.getMessage());
        }
        return "redirect:/admin/assign";
    }

    @GetMapping("/monitor")
    public String monitorPage(Model model) {
        model.addAttribute("deliveries", deliveryService.getAllDeliveries());
        return "admin/monitor";
    }

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        model.addAttribute("deliveries", deliveryService.getAllDeliveries());
        model.addAttribute("expenses", expenseService.getAllExpenses());
        return "admin/reports";
    }

    private int getMonthValue(String monthName) {
        if (monthName == null) return 1;
        switch (monthName.trim().toLowerCase()) {
            case "january": case "jan": return 1;
            case "february": case "feb": return 2;
            case "march": case "mar": return 3;
            case "april": case "apr": return 4;
            case "may": return 5;
            case "june": case "jun": return 6;
            case "july": case "jul": return 7;
            case "august": case "aug": return 8;
            case "september": case "sep": case "sept": return 9;
            case "october": case "oct": return 10;
            case "november": case "nov": return 11;
            case "december": case "dec": return 12;
            default:
                try {
                    return Integer.parseInt(monthName.trim());
                } catch (Exception e) {
                    return 1;
                }
        }
    }
}
