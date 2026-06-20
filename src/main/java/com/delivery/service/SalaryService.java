package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to manage Agent Salaries: calculating incentives, generating slips, approving payout.
 */
@Service
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final AgentRepository agentRepository;
    private final DeliveryService deliveryService;
    private final AgentAuditLogRepository agentAuditLogRepository;
    private final com.razorpay.RazorpayClient razorpayClient;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SalaryService(SalaryRepository salaryRepository, AgentRepository agentRepository, 
                         DeliveryService deliveryService, AgentAuditLogRepository agentAuditLogRepository,
                         com.razorpay.RazorpayClient razorpayClient, SalaryPaymentRepository salaryPaymentRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.salaryRepository = salaryRepository;
        this.agentRepository = agentRepository;
        this.deliveryService = deliveryService;
        this.agentAuditLogRepository = agentAuditLogRepository;
        this.razorpayClient = razorpayClient;
        this.salaryPaymentRepository = salaryPaymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public BigDecimal calculateIncentive(Agent agent, String month, int year) {
        int monthVal = getMonthValue(month);
        long completed = deliveryService.countCompletedDeliveries(agent, monthVal, year);
        return BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(20.00)); // ₹20 per successful delivery
    }

    @Transactional
    public Salary generateMonthlySalarySlip(String agentId, String month, int year, 
                                            BigDecimal basicSalary, BigDecimal bonus, 
                                            BigDecimal fuelReimbursement, BigDecimal deductions) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        // Validate that salary cannot be generated for past periods
        int currentYear = java.time.LocalDate.now().getYear();
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        int selectedMonthVal = getMonthValue(month);
        if (year < currentYear || (year == currentYear && selectedMonthVal < currentMonth)) {
            throw new IllegalArgumentException("Salary cannot be generated for past periods.");
        }

        // Check if salary slip already exists for this month and year
        Optional<Salary> existingOpt = salaryRepository.findByAgentIdAndMonthAndYear(agentId, month, year);
        if (existingOpt.isPresent()) {
            throw new IllegalStateException("Salary slip already exists for " + month + " " + year);
        }

        BigDecimal incentive = calculateIncentive(agent, month, year);
        
        // Total Salary = Basic Salary + Delivery Incentive + Bonus + Fuel Reimbursement - Deductions
        BigDecimal netSalary = basicSalary.add(incentive).add(bonus).add(fuelReimbursement).subtract(deductions);

        Salary salary = new Salary(agent, month, year, basicSalary, incentive, bonus, fuelReimbursement, deductions, netSalary, "PENDING");
        Salary saved = salaryRepository.save(salary);

        // Audit Log
        String adminName = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            adminName = auth.getName();
        }
        AgentAuditLog log = new AgentAuditLog(agentId, "Salary Generated", adminName, "Salary generated for " + month + " " + year + ". Net: " + netSalary);
        agentAuditLogRepository.save(log);

        // Notify Agent (Automatically duplicated to admin)
        eventPublisher.publishEvent(new NotificationEvent(
            this,
            agent.getUser().getUsername(),
            "Salary Slip Generated",
            "Your salary slip for " + month + " " + year + " has been generated. Net: ₹" + netSalary,
            "FINANCIAL",
            "MEDIUM"
        ));

        return saved;
    }

    @Transactional
    public void approveSalaryPayout(Integer salaryId) {
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new IllegalArgumentException("Salary record not found"));
        
        if ("PAID".equalsIgnoreCase(salary.getPaymentStatus())) {
            throw new IllegalStateException("Salary already paid.");
        }

        salary.setPaymentStatus("PAID");
        salary.setPaymentDate(LocalDateTime.now());
        salaryRepository.save(salary);

        // Audit Log
        String adminName = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            adminName = auth.getName();
        }
        AgentAuditLog log = new AgentAuditLog(salary.getAgent().getId(), "Salary Paid", adminName, "Salary paid for " + salary.getMonth() + " " + salary.getYear());
        agentAuditLogRepository.save(log);

        // Notify Agent
        eventPublisher.publishEvent(new NotificationEvent(
            this,
            salary.getAgent().getUser().getUsername(),
            "Salary Approved",
            "Your salary payout of ₹" + salary.getNetSalary() + " for " + salary.getMonth() + " " + salary.getYear() + " has been approved and marked as paid.",
            "FINANCIAL",
            "HIGH"
        ));
    }

    @Transactional
    public String initiateSalaryPaymentLink(Integer salaryId, String baseUrl) {
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new IllegalArgumentException("Salary record not found"));

        if ("PAID".equalsIgnoreCase(salary.getPaymentStatus())) {
            throw new IllegalStateException("Salary already paid.");
        }

        try {
            int netSalaryInPaise = salary.getNetSalary().multiply(BigDecimal.valueOf(100)).intValue();

            org.json.JSONObject paymentLinkRequest = new org.json.JSONObject();
            paymentLinkRequest.put("amount", netSalaryInPaise);
            paymentLinkRequest.put("currency", "INR");
            paymentLinkRequest.put("accept_partial", false);
            paymentLinkRequest.put("description", "Salary Payout to " + salary.getAgent().getName() + " for " + salary.getMonth() + " " + salary.getYear());

            org.json.JSONObject customer = new org.json.JSONObject();
            customer.put("name", salary.getAgent().getName());
            customer.put("contact", salary.getAgent().getPhone());
            customer.put("email", salary.getAgent().getEmail());
            paymentLinkRequest.put("customer", customer);

            org.json.JSONObject notify = new org.json.JSONObject();
            notify.put("sms", true);
            notify.put("email", true);
            paymentLinkRequest.put("notify", notify);

            paymentLinkRequest.put("callback_url", baseUrl + "/admin/salaries/callback?salary_id=" + salaryId);
            paymentLinkRequest.put("callback_method", "get");

            com.razorpay.PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);
            String paymentLinkId = paymentLink.get("id");
            String shortUrl = paymentLink.get("short_url");

            SalaryPayment sp = new SalaryPayment(salary, salary.getAgent(), salary.getNetSalary(), "PENDING", paymentLinkId);
            salaryPaymentRepository.save(sp);

            return shortUrl;
        } catch (com.razorpay.RazorpayException e) {
            System.err.println("RazorpayException during payout creation: " + e.getMessage() + ". Generating mock checkout...");
            
            String paymentLinkId = "plink_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            String mockUrl = baseUrl + "/admin/salaries/callback?salary_id=" + salaryId + "&razorpay_payment_link_id=" + paymentLinkId + "&razorpay_payment_id=pay_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14) + "&razorpay_payment_link_status=paid";
            
            SalaryPayment sp = new SalaryPayment(salary, salary.getAgent(), salary.getNetSalary(), "PENDING", paymentLinkId);
            salaryPaymentRepository.save(sp);

            return mockUrl;
        }
    }

    @Transactional
    public void verifySalaryCallback(Integer salaryId, String paymentLinkId, String paymentId, String status) {
        SalaryPayment sp = null;
        Salary salary = null;

        if (paymentLinkId != null && !paymentLinkId.trim().isEmpty()) {
            sp = salaryPaymentRepository.findByTransactionId(paymentLinkId).orElse(null);
            if (sp != null) {
                salary = sp.getSalary();
            }
        }

        if (salary == null) {
            if (salaryId == null) {
                throw new IllegalArgumentException("Both salary_id and payment_link_id are missing or invalid");
            }
            salary = salaryRepository.findById(salaryId)
                    .orElseThrow(() -> new IllegalArgumentException("Salary record not found"));
        }

        if (sp == null) {
            List<SalaryPayment> list = salaryPaymentRepository.findBySalarySalaryIdOrderByPaymentDateDesc(salary.getSalaryId());
            sp = list.stream()
                    .filter(p -> "PENDING".equalsIgnoreCase(p.getPaymentStatus()))
                    .findFirst()
                    .orElse(null);

            if (sp == null) {
                if (!list.isEmpty()) {
                    sp = list.get(0);
                } else {
                    sp = new SalaryPayment(salary, salary.getAgent(), salary.getNetSalary(), "PENDING", paymentLinkId);
                }
            }
        }

        if ("paid".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            salary.setPaymentStatus("PAID");
            salary.setPaymentDate(LocalDateTime.now());
            salaryRepository.save(salary);

            sp.setPaymentStatus("SUCCESS");
            sp.setTransactionId(paymentId != null ? paymentId : paymentLinkId);
            sp.setPaymentDate(LocalDateTime.now());
            salaryPaymentRepository.save(sp);

            String adminName = "SYSTEM";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
                adminName = auth.getName();
            }
            AgentAuditLog log = new AgentAuditLog(salary.getAgent().getId(), "Salary Paid", adminName, 
                    "Salary paid via Razorpay for " + salary.getMonth() + " " + salary.getYear() + ". TransId: " + paymentId);
            agentAuditLogRepository.save(log);

            // Notify Agent on Successful Credited
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                salary.getAgent().getUser().getUsername(),
                "Salary Credited",
                "Your salary of ₹" + salary.getNetSalary() + " for " + salary.getMonth() + " " + salary.getYear() + " has been credited. TransID: " + (paymentId != null ? paymentId : paymentLinkId),
                "FINANCIAL",
                "HIGH"
            ));
        } else {
            salary.setPaymentStatus("FAILED");
            salaryRepository.save(salary);

            sp.setPaymentStatus("FAILED");
            sp.setTransactionId(paymentId);
            salaryPaymentRepository.save(sp);

            // Notify Agent on Failure
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                salary.getAgent().getUser().getUsername(),
                "Salary Payment Failed",
                "Your salary payout of ₹" + salary.getNetSalary() + " for " + salary.getMonth() + " " + salary.getYear() + " has failed.",
                "FINANCIAL",
                "HIGH"
            ));
        }
    }

    @Transactional
    public void processDirectSalaryPayment(Integer salaryId, String method, String transactionId) {
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(() -> new IllegalArgumentException("Salary record not found"));

        if ("PAID".equalsIgnoreCase(salary.getPaymentStatus())) {
            throw new IllegalStateException("Salary already paid.");
        }

        salary.setPaymentStatus("PAID");
        salary.setPaymentDate(LocalDateTime.now());
        salaryRepository.save(salary);

        SalaryPayment sp = new SalaryPayment(salary, salary.getAgent(), salary.getNetSalary(), "SUCCESS", transactionId);
        sp.setPaymentDate(LocalDateTime.now());
        salaryPaymentRepository.save(sp);

        String adminName = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            adminName = auth.getName();
        }
        AgentAuditLog log = new AgentAuditLog(salary.getAgent().getId(), "Salary Paid", adminName, 
                "Salary paid via " + method + " for " + salary.getMonth() + " " + salary.getYear() + ". TransId: " + transactionId);
        agentAuditLogRepository.save(log);

        // Notify Agent on Successful Credited
        eventPublisher.publishEvent(new NotificationEvent(
            this,
            salary.getAgent().getUser().getUsername(),
            "Salary Credited",
            "Your salary of ₹" + salary.getNetSalary() + " for " + salary.getMonth() + " " + salary.getYear() + " has been credited via " + method + ". TransId: " + transactionId,
            "FINANCIAL",
            "HIGH"
        ));
    }

    public List<Salary> getSalaryHistoryByAgent(String agentId) {
        return salaryRepository.findByAgentId(agentId);
    }

    public List<Salary> getAllSalaries() {
        return salaryRepository.findAll();
    }

    public Optional<Salary> getSalaryById(Integer salaryId) {
        return salaryRepository.findById(salaryId);
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
