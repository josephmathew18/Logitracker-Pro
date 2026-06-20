package com.delivery.controller;

import com.delivery.model.Agent;
import com.delivery.model.Delivery;
import com.delivery.model.Salary;
import com.delivery.service.AgentService;
import com.delivery.service.DeliveryService;
import com.delivery.service.ExpenseService;
import com.delivery.service.SalaryService;
import com.delivery.service.PdfGenerationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/agent")
public class AgentController {

    private final DeliveryService deliveryService;
    private final AgentService agentService;
    private final ExpenseService expenseService;
    private final SalaryService salaryService;
    private final PdfGenerationService pdfGenerationService;

    public AgentController(DeliveryService deliveryService, AgentService agentService, 
                           ExpenseService expenseService, SalaryService salaryService,
                           PdfGenerationService pdfGenerationService) {
        this.deliveryService = deliveryService;
        this.agentService = agentService;
        this.expenseService = expenseService;
        this.salaryService = salaryService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);

        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            model.addAttribute("agent", agent);
            List<Delivery> deliveries = deliveryService.getDeliveriesByAgent(username);
            model.addAttribute("deliveries", deliveries);

            // Compute Quick Stats
            long totalDeliveries = deliveries.size();
            long completedDeliveries = deliveries.stream()
                    .filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus()))
                    .count();
            long pendingDeliveries = deliveries.stream()
                    .filter(d -> !"DELIVERED".equalsIgnoreCase(d.getStatus()) && !"REJECTED".equalsIgnoreCase(d.getStatus()))
                    .count();

            // Monthly Salary (net salary of latest salary calculation or basic salary)
            List<Salary> salaryHistory = salaryService.getSalaryHistoryByAgent(agent.getId());
            BigDecimal monthlySalary = salaryHistory.isEmpty() ? BigDecimal.ZERO : salaryHistory.get(0).getNetSalary();

            // Total Fuel Claims Amount
            List<com.delivery.model.FuelExpense> expenses = expenseService.getExpensesByAgent(username);
            BigDecimal fuelClaims = expenses.stream()
                    .map(com.delivery.model.FuelExpense::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("totalDeliveries", totalDeliveries);
            model.addAttribute("completedDeliveries", completedDeliveries);
            model.addAttribute("pendingDeliveries", pendingDeliveries);
            model.addAttribute("monthlySalary", monthlySalary);
            model.addAttribute("fuelClaims", fuelClaims);
        }
        return "agent/dashboard";
    }

    @PostMapping("/deliveries/respond")
    public String respondToDelivery(@RequestParam("deliveryId") Integer deliveryId,
                                    @RequestParam("accept") boolean accept,
                                    RedirectAttributes redirectAttributes) {
        try {
            deliveryService.respondToDelivery(deliveryId, accept);
            String response = accept ? "accepted" : "rejected";
            redirectAttributes.addFlashAttribute("successMessage", "Delivery request " + response + " successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error responding to delivery: " + e.getMessage());
        }
        return "redirect:/agent/dashboard";
    }

    @PostMapping("/deliveries/update-status")
    public String updateStatus(@RequestParam("deliveryId") Integer deliveryId,
                               @RequestParam("status") String status,
                               @RequestParam("latitude") Double latitude,
                               @RequestParam("longitude") Double longitude,
                               RedirectAttributes redirectAttributes) {
        try {
            deliveryService.updateDeliveryStatus(deliveryId, status, latitude, longitude);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery status updated to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/agent/dashboard";
    }

    @GetMapping("/expenses")
    public String expensesPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("expenses", expenseService.getExpensesByAgent(username));
        return "agent/expenses";
    }

    @PostMapping("/expenses/upload")
    public String uploadExpense(Authentication authentication,
                                @RequestParam("expenseDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam("quantity") BigDecimal quantity,
                                @RequestParam("price") BigDecimal price,
                                @RequestParam("billFile") MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            expenseService.logFuelExpense(username, date, quantity, price, file);
            redirectAttributes.addFlashAttribute("successMessage", "Fuel expense and bill uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload expense: " + e.getMessage());
        }
        return "redirect:/agent/expenses";
    }

    @GetMapping("/salaries")
    public String salariesPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            model.addAttribute("agent", agent);
            model.addAttribute("salaries", salaryService.getSalaryHistoryByAgent(agent.getId()));
            
            // Current month completed deliveries and estimated incentive
            LocalDate now = LocalDate.now();
            int currentMonthValue = now.getMonthValue();
            int currentYear = now.getYear();
            
            // Format current month name
            String currentMonthName = now.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            
            long completedDeliveries = deliveryService.countCompletedDeliveries(agent, currentMonthValue, currentYear);
            BigDecimal estimatedIncentive = BigDecimal.valueOf(completedDeliveries).multiply(BigDecimal.valueOf(20.00));
            
            model.addAttribute("currentMonthName", currentMonthName);
            model.addAttribute("currentYear", currentYear);
            model.addAttribute("completedDeliveries", completedDeliveries);
            model.addAttribute("estimatedIncentive", estimatedIncentive);
        }
        return "agent/salaries";
    }

    @GetMapping("/salaries/slip/{id}")
    public String getSalarySlip(@PathVariable("id") Integer id, Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            Optional<Salary> salaryOpt = salaryService.getSalaryById(id);
            if (salaryOpt.isPresent()) {
                Salary salary = salaryOpt.get();
                // Ensure the salary slip belongs to the logged-in agent
                if (salary.getAgent().getId().equals(agent.getId())) {
                    model.addAttribute("salary", salary);
                    return "agent/salary-slip";
                }
            }
        }
        return "redirect:/agent/salaries";
    }

    @GetMapping("/salaries/slip/download/{id}")
    public void downloadSalarySlip(@PathVariable("id") Integer id, Authentication authentication,
                                   jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);
        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            Optional<Salary> salaryOpt = salaryService.getSalaryById(id);
            if (salaryOpt.isPresent()) {
                Salary salary = salaryOpt.get();
                if (salary.getAgent().getId().equals(agent.getId())) {
                    response.setContentType("application/pdf");
                    response.setHeader("Content-Disposition", "attachment; filename=salary_slip_" + id + ".pdf");
                    pdfGenerationService.generateSalarySlip(salary, response.getOutputStream());
                    return;
                }
            }
        }
        response.sendRedirect("/agent/salaries");
    }

    @GetMapping("/change-password")
    public String changePasswordPage() {
        return "redirect:/agent/profile?tab=password";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 jakarta.servlet.http.HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);
        Agent agent = agentOpt.orElse(null);
        if (agent == null) {
            return "redirect:/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Confirm Password does not match New Password.");
            return "redirect:/agent/profile?tab=password";
        }

        try {
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            agentService.changeAgentPassword(agent.getId(), currentPassword, newPassword, ipAddress);

            // Invalidate session and clear context
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            return "redirect:/login?passwordChanged=true";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/agent/profile?tab=password";
        }
    }

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);

        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            model.addAttribute("agent", agent);

            // Fetch activity history
            model.addAttribute("activities", agentService.getActivityLogs(agent.getId()));

            // Compute Quick Stats
            List<Delivery> deliveries = deliveryService.getDeliveriesByAgent(username);
            long totalDeliveries = deliveries.size();
            long completedDeliveries = deliveries.stream()
                    .filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus()))
                    .count();
            long pendingDeliveries = deliveries.stream()
                    .filter(d -> !"DELIVERED".equalsIgnoreCase(d.getStatus()) && !"REJECTED".equalsIgnoreCase(d.getStatus()))
                    .count();

            List<Salary> salaryHistory = salaryService.getSalaryHistoryByAgent(agent.getId());
            BigDecimal monthlySalary = salaryHistory.isEmpty() ? BigDecimal.ZERO : salaryHistory.get(0).getNetSalary();

            List<com.delivery.model.FuelExpense> expenses = expenseService.getExpensesByAgent(username);
            BigDecimal fuelClaims = expenses.stream()
                    .map(com.delivery.model.FuelExpense::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("totalDeliveries", totalDeliveries);
            model.addAttribute("completedDeliveries", completedDeliveries);
            model.addAttribute("pendingDeliveries", pendingDeliveries);
            model.addAttribute("monthlySalary", monthlySalary);
            model.addAttribute("fuelClaims", fuelClaims);
        }
        return "agent/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("email") String email,
                                @RequestParam(value = "dob", required = false) String dob,
                                @RequestParam(value = "gender", required = false) String gender,
                                @RequestParam(value = "licenseNumber", required = false) String licenseNumber,
                                @RequestParam("address") String address,
                                @RequestParam("city") String city,
                                @RequestParam("state") String state,
                                @RequestParam("pincode") String pincode,
                                @RequestParam(value = "profilePhoto", required = false) MultipartFile file,
                                jakarta.servlet.http.HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);

        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            try {
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }

                java.time.LocalDate dobVal = null;
                if (dob != null && !dob.trim().isEmpty()) {
                    dobVal = java.time.LocalDate.parse(dob);
                }

                agentService.updateAgentProfile(agent.getId(), name, phone, email, dobVal, gender, licenseNumber, 
                                                address, city, state, pincode, file, ipAddress);
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
            }
        }
        return "redirect:/agent/profile";
    }

    @PostMapping("/profile/delete-photo")
    public String deletePhoto(Authentication authentication,
                              jakarta.servlet.http.HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Agent> agentOpt = agentService.getAgentByUsername(username);

        if (agentOpt.isPresent()) {
            Agent agent = agentOpt.get();
            try {
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }

                agentService.deleteProfilePhoto(agent.getId(), ipAddress);
                redirectAttributes.addFlashAttribute("successMessage", "Profile photo deleted successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete profile photo: " + e.getMessage());
            }
        }
        return "redirect:/agent/profile";
    }
}
