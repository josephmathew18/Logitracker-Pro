package com.delivery.config;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Automatically initializes the database with dummy profiles if it is empty.
 * Seeds data for the new modules (Attendance, Leave, Maintenance, Performance, etc.)
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AgentRepository agentRepository;
    private final VehicleRepository vehicleRepository;
    private final FuelExpenseRepository fuelExpenseRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AgentVehicleHistoryRepository agentVehicleHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository, CustomerRepository customerRepository,
                               AgentRepository agentRepository, VehicleRepository vehicleRepository,
                               FuelExpenseRepository fuelExpenseRepository,
                               LeaveBalanceRepository leaveBalanceRepository,
                               LeaveApplicationRepository leaveApplicationRepository,
                               VehicleMaintenanceRepository vehicleMaintenanceRepository,
                               DocumentRepository documentRepository,
                               NotificationRepository notificationRepository,
                               AuditLogRepository auditLogRepository,
                               AgentVehicleHistoryRepository agentVehicleHistoryRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.agentRepository = agentRepository;
        this.vehicleRepository = vehicleRepository;
        this.fuelExpenseRepository = fuelExpenseRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.vehicleMaintenanceRepository = vehicleMaintenanceRepository;
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.agentVehicleHistoryRepository = agentVehicleHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only run if the database has no users
        if (userRepository.count() == 0) {
            System.out.println(">>> Initializing database with dummy testing data...");

            // 1. Create Admin
            User adminUser = new User("admin", passwordEncoder.encode("admin123"), "ADMIN", true);
            userRepository.save(adminUser);

            // 2. Create Customer
            User customerUser = new User("customer", passwordEncoder.encode("customer123"), "CUSTOMER", true);
            Customer customer = new Customer(customerUser, "Demo Customer", "9876543210", "customer@logitrack.com", "123 Demo Street, Bangalore");
            customer.setCity("Bangalore");
            customer.setState("Karnataka");
            customer.setPincode("560001");
            customer = customerRepository.save(customer);
            User managedCustomerUser = customer.getUser();

            // 3. Create Vehicle
            Vehicle vehicle = new Vehicle("KA-01-ME-1234", "Ather 450X EV", "Bike", "ASSIGNED", "EV", "Policy: #ATH-90812 Exp: 2027-12-15", 25.0);
            vehicleRepository.save(vehicle);

            // 4. Create Agent 1001 (Demo Agent)
            User agentUser = new User("1001", passwordEncoder.encode("agent123"), "AGENT", true);
            Agent agent = new Agent();
            agent.setId("1001");
            agent.setUser(agentUser);
            agent.setName("Demo Agent");
            agent.setPhone("9876543211");
            agent.setEmail("agent@logitrack.com");
            agent.setStatus(AgentStatus.ACTIVE);
            agent.setPassword(agentUser.getPassword());
            agent.setVehicle(vehicle); // Assign Vehicle
            agent.setShiftType("Morning");
            agent.setShiftStartTime(java.time.LocalTime.now().minusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent.setShiftEndTime(java.time.LocalTime.now().plusHours(6).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent = agentRepository.save(agent);
            User managedAgentUser = agent.getUser();

            // Seed Agent 1002 (Yet to Start Shift)
            User agentUser2 = new User("1002", passwordEncoder.encode("agent123"), "AGENT", true);
            userRepository.save(agentUser2);
            Agent agent2 = new Agent("1002", agentUser2, "Agent YetToStart", "9876543212", "agent2@logitrack.com");
            agent2.setShiftType("Night");
            agent2.setShiftStartTime(java.time.LocalTime.now().plusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent2.setShiftEndTime(java.time.LocalTime.now().plusHours(10).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent2.setStatus(AgentStatus.ACTIVE);
            agent2.setPassword(agentUser2.getPassword());
            agentRepository.save(agent2);

            // Seed Agent 1003 (Off Shift)
            User agentUser3 = new User("1003", passwordEncoder.encode("agent123"), "AGENT", true);
            userRepository.save(agentUser3);
            Agent agent3 = new Agent("1003", agentUser3, "Agent OffShift", "9876543213", "agent3@logitrack.com");
            agent3.setShiftType("Morning");
            agent3.setShiftStartTime(java.time.LocalTime.now().minusHours(10).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent3.setShiftEndTime(java.time.LocalTime.now().minusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent3.setStatus(AgentStatus.ACTIVE);
            agent3.setPassword(agentUser3.getPassword());
            agentRepository.save(agent3);

            // Seed Agent 1004 (On Leave)
            User agentUser4 = new User("1004", passwordEncoder.encode("agent123"), "AGENT", true);
            userRepository.save(agentUser4);
            Agent agent4 = new Agent("1004", agentUser4, "Agent OnLeave", "9876543214", "agent4@logitrack.com");
            agent4.setShiftType("Morning");
            agent4.setShiftStartTime(java.time.LocalTime.now().minusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent4.setShiftEndTime(java.time.LocalTime.now().plusHours(6).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent4.setStatus(AgentStatus.ACTIVE);
            agent4.setPassword(agentUser4.getPassword());
            agent4 = agentRepository.save(agent4);

            // Seed approved Leave for Agent 1004 today
            LeaveApplication leaveToday = new LeaveApplication(agent4, LocalDate.now(), LocalDate.now(), "CASUAL", "Approved personal leave today");
            leaveToday.setStatus("APPROVED");
            leaveApplicationRepository.save(leaveToday);

            // Seed Agent 1005 (Suspended)
            User agentUser5 = new User("1005", passwordEncoder.encode("agent123"), "AGENT", true);
            userRepository.save(agentUser5);
            Agent agent5 = new Agent("1005", agentUser5, "Agent Suspended", "9876543215", "agent5@logitrack.com");
            agent5.setShiftType("Morning");
            agent5.setShiftStartTime(java.time.LocalTime.now().minusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent5.setShiftEndTime(java.time.LocalTime.now().plusHours(6).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent5.setStatus(AgentStatus.SUSPENDED);
            agent5.setPassword(agentUser5.getPassword());
            agentRepository.save(agent5);

            // Seed Agent 1006 (Terminated)
            User agentUser6 = new User("1006", passwordEncoder.encode("agent123"), "AGENT", false);
            userRepository.save(agentUser6);
            Agent agent6 = new Agent("1006", agentUser6, "Agent Terminated", "9876543216", "agent6@logitrack.com");
            agent6.setShiftType("Morning");
            agent6.setShiftStartTime(java.time.LocalTime.now().minusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent6.setShiftEndTime(java.time.LocalTime.now().plusHours(6).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent6.setStatus(AgentStatus.TERMINATED);
            agent6.setTerminationDate(LocalDateTime.now());
            agent6.setTerminationReason("End of contract");
            agent6.setPassword(agentUser6.getPassword());
            agentRepository.save(agent6);

            // Seed Agent 1007 (Absent)
            User agentUser7 = new User("1007", passwordEncoder.encode("agent123"), "AGENT", true);
            userRepository.save(agentUser7);
            Agent agent7 = new Agent("1007", agentUser7, "Agent Absent", "9876543217", "agent7@logitrack.com");
            agent7.setShiftType("Morning");
            agent7.setShiftStartTime(java.time.LocalTime.now().minusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent7.setShiftEndTime(java.time.LocalTime.now().plusHours(6).truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
            agent7.setStatus(AgentStatus.ACTIVE);
            agent7.setPassword(agentUser7.getPassword());
            agentRepository.save(agent7);

            // Update Vehicle back-link
            vehicle.setAgent(agent);
            vehicleRepository.save(vehicle);

            // Seed Agent Vehicle Assignment History
            AgentVehicleHistory vehicleHistory = new AgentVehicleHistory(
                "1001",
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                LocalDateTime.now().minusDays(10),
                "admin"
            );
            agentVehicleHistoryRepository.save(vehicleHistory);

            // 5. Create Fuel Expense
            FuelExpense expense = new FuelExpense(
                agent,
                LocalDate.now().minusDays(1),
                new BigDecimal("25.50"),
                new BigDecimal("105.00"),
                new BigDecimal("2677.50"),
                "/uploads/bills/1001_1d230230-1cac-4230-aa26-258e5e4432d2.jpg"
            );
            fuelExpenseRepository.save(expense);

            // 6. Seed Leave Balance
            LeaveBalance balance = new LeaveBalance(agent);
            balance.setCasualLeavesLeft(10);
            balance.setSickLeavesLeft(7);
            balance.setAnnualLeavesLeft(15);
            leaveBalanceRepository.save(balance);

            // 7. Seed Leave Application
            LeaveApplication leave1 = new LeaveApplication(agent, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), "CASUAL", "Family function");
            leave1.setStatus("PENDING");
            leaveApplicationRepository.save(leave1);

            LeaveApplication leave2 = new LeaveApplication(agent, LocalDate.now().minusDays(10), LocalDate.now().minusDays(9), "SICK", "Fever and cold");
            leave2.setStatus("APPROVED");
            leave2.setRemarks("Approved automatically upon verification");
            leaveApplicationRepository.save(leave2);

            // 8. Seed Vehicle Maintenance
            VehicleMaintenance maint1 = new VehicleMaintenance(vehicle, LocalDate.now().minusDays(30), "Battery health check");
            maint1.setCompletedDate(LocalDate.now().minusDays(30));
            maint1.setMaintenanceCost(new BigDecimal("1500.00"));
            maint1.setStatus("COMPLETED");
            vehicleMaintenanceRepository.save(maint1);

            VehicleMaintenance maint2 = new VehicleMaintenance(vehicle, LocalDate.now().plusDays(15), "Regular brake pad replacement");
            maint2.setStatus("SCHEDULED");
            vehicleMaintenanceRepository.save(maint2);

            // 9. Seed Documents
            // Agent 1001
            Document doc1 = new Document(agent, null, "DRIVING_LICENSE", "driving_license.pdf", "/uploads/documents/dl_sample.pdf", LocalDate.now().plusYears(3));
            doc1.setVerificationStatus("APPROVED");
            doc1.setRemarks("Valid driving license verified");
            documentRepository.save(doc1);

            Document doc2 = new Document(agent, null, "IDENTITY_PROOF", "aadhaar_card.png", "/uploads/documents/aadhaar_sample.png", null);
            doc2.setVerificationStatus("APPROVED");
            doc2.setRemarks("Aadhaar card verified");
            documentRepository.save(doc2);

            Document doc3 = new Document(agent, null, "VEHICLE_RC", "rc_sample.jpg", "/uploads/documents/rc_sample.jpg", null);
            doc3.setVerificationStatus("PENDING");
            doc3.setRemarks("Please verify registration number");
            documentRepository.save(doc3);

            Document doc4 = new Document(agent, null, "INSURANCE", "insurance_cert.pdf", "/uploads/documents/insurance_sample.pdf", LocalDate.now().plusMonths(6));
            doc4.setVerificationStatus("REJECTED");
            doc4.setRemarks("Insurance copy blurry");
            documentRepository.save(doc4);

            // Agent 1002 (Expiring Soon)
            Document doc5 = new Document(agent2, null, "DRIVING_LICENSE", "driving_license_1002.pdf", "/uploads/documents/dl_sample.pdf", LocalDate.now().plusDays(5));
            doc5.setVerificationStatus("APPROVED");
            doc5.setRemarks("Expiring soon");
            documentRepository.save(doc5);

            // Agent 1003 (Expired)
            Document doc6 = new Document(agent3, null, "DRIVING_LICENSE", "driving_license_1003.pdf", "/uploads/documents/dl_sample.pdf", LocalDate.now().minusDays(3));
            doc6.setVerificationStatus("APPROVED");
            doc6.setRemarks("Expired license");
            documentRepository.save(doc6);

            // 10. Seed Notifications
            Notification notif1 = new Notification(managedAgentUser, "Welcome!", "Welcome to LogiTrack Pro fleet operations portal.", "GENERAL");
            notificationRepository.save(notif1);

            Notification notif2 = new Notification(managedAgentUser, "Salary Deposited", "Your salary for May 2026 has been credited.", "SALARY");
            notificationRepository.save(notif2);

            Notification notif3 = new Notification(adminUser, "System Initialized", "All advanced modules and parameters set up successfully.", "GENERAL");
            notificationRepository.save(notif3);

            // 11. Seed Audit Logs
            AuditLog log1 = new AuditLog("admin", "ADMIN", "ASSIGN_VEHICLE", "Assigned Ather EV vehicle (KA-01-ME-1234) to Agent 1001.");
            auditLogRepository.save(log1);

            AuditLog log2 = new AuditLog("admin", "ADMIN", "LEAVE_APPROVAL", "Approved SICK leave for Agent 1001 (10-Jun to 11-Jun).");
            auditLogRepository.save(log2);

            System.out.println(">>> Database initialized and advanced modules seeded successfully!");
        } else {
            System.out.println(">>> Database already contains data. Skipping initialization.");
        }
    }
}
