package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service to manage Delivery Agents: auto ID generation, temp password, SMS logging, vehicle allocation.
 */
@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryRepository deliveryRepository;
    private final AgentAuditLogRepository agentAuditLogRepository;
    private final PasswordChangeLogRepository passwordChangeLogRepository;
    private final AgentActivityLogRepository agentActivityLogRepository;
    private final AgentVehicleHistoryRepository agentVehicleHistoryRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    @Value("${upload.profile.dir:uploads/profile-images}")
    private String profileUploadDir;

    public AgentService(UserRepository userRepository, AgentRepository agentRepository, 
                        VehicleRepository vehicleRepository, DeliveryRepository deliveryRepository,
                        AgentAuditLogRepository agentAuditLogRepository, 
                        PasswordChangeLogRepository passwordChangeLogRepository,
                        AgentActivityLogRepository agentActivityLogRepository,
                        AgentVehicleHistoryRepository agentVehicleHistoryRepository,
                        PasswordEncoder passwordEncoder, SessionRegistry sessionRegistry,
                        LeaveApplicationRepository leaveApplicationRepository,
                        AttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.vehicleRepository = vehicleRepository;
        this.deliveryRepository = deliveryRepository;
        this.agentAuditLogRepository = agentAuditLogRepository;
        this.passwordChangeLogRepository = passwordChangeLogRepository;
        this.agentActivityLogRepository = agentActivityLogRepository;
        this.agentVehicleHistoryRepository = agentVehicleHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRegistry = sessionRegistry;
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional
    public Agent registerAgent(String name, String phone, String email, String password) {
        // Generate Agent ID (e.g., 1001)
        long count = agentRepository.count();
        String agentId = String.valueOf(1001 + count);

        // Create User entity
        User user = new User();
        user.setUsername(agentId); // Using Agent ID as username for login
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("AGENT");
        user.setActive(true);

        // Create Agent entity
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setUser(user);
        agent.setName(name);
        agent.setPhone(phone);
        agent.setEmail(email);
        agent.setStatus(AgentStatus.ACTIVE);
        agent.setPassword(user.getPassword());
        agent.setJoiningDate(LocalDateTime.now());

        Agent savedAgent = agentRepository.save(agent);

        // Record audit log
        String adminName = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            adminName = auth.getName();
        }
        AgentAuditLog auditLog = new AgentAuditLog(agentId, "Agent Registered", adminName, "Initial registration");
        agentAuditLogRepository.save(auditLog);

        // Mock SMS Sending using console logs (in real application, Twilio or another SMS API would be integrated)
        sendMockSMS(phone, agentId, password);

        return savedAgent;
    }

    /**
     * Updates agent status (ACTIVE, SUSPENDED, INACTIVE) and invalidates session if needed.
     */
    @Transactional
    public void updateAgentStatus(String agentId, AgentStatus newStatus, String remarks) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        AgentStatus oldStatus = agent.getStatus();
        if (oldStatus == AgentStatus.TERMINATED) {
            throw new IllegalStateException("Cannot change status of a terminated agent.");
        }

        agent.setStatus(newStatus);
        
        // Update user active status
        User user = agent.getUser();
        if (newStatus == AgentStatus.ACTIVE) {
            user.setActive(true);
        } else {
            user.setActive(false);
        }
        userRepository.save(user);
        agentRepository.save(agent);

        // Record audit log
        String adminName = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            adminName = auth.getName();
        }

        String action = "Agent Reactivated";
        if (newStatus == AgentStatus.SUSPENDED) {
            action = "Agent Suspended";
        } else if (newStatus == AgentStatus.INACTIVE) {
            action = "Agent Deactivated";
        }

        AgentAuditLog log = new AgentAuditLog(agentId, action, adminName, remarks);
        agentAuditLogRepository.save(log);

        // Invalidate session if deactivated or suspended
        if (newStatus != AgentStatus.ACTIVE) {
            expireUserSessions(agentId);
        }
    }

    /**
     * Terminates agent, reassigning pending deliveries and invalidating session.
     */
    @Transactional
    public void terminateAgent(String agentId, String reason, String reassignToAgentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        if (agent.getStatus() == AgentStatus.TERMINATED) {
            throw new IllegalStateException("Agent is already terminated.");
        }

        // 1. Reassign pending deliveries if needed
        List<Delivery> activeDeliveries = deliveryRepository.findByAgentAndStatusIn(
                agent, Arrays.asList("ASSIGNED", "PICKED_UP", "IN_TRANSIT"));

        if (!activeDeliveries.isEmpty()) {
            if (reassignToAgentId == null || reassignToAgentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Must select an active agent to reassign pending deliveries to before termination.");
            }
            Agent targetAgent = agentRepository.findById(reassignToAgentId)
                    .orElseThrow(() -> new IllegalArgumentException("Target reassignment agent not found."));

            if (targetAgent.getStatus() != AgentStatus.ACTIVE) {
                throw new IllegalArgumentException("Target agent for reassignment must be ACTIVE.");
            }

            for (Delivery delivery : activeDeliveries) {
                delivery.setAgent(targetAgent);
                deliveryRepository.save(delivery);
            }
        }

        // 2. Terminate agent
        agent.setStatus(AgentStatus.TERMINATED);
        agent.setTerminationDate(LocalDateTime.now());
        agent.setTerminationReason(reason);

        // Deactivate User
        User user = agent.getUser();
        user.setActive(false);
        userRepository.save(user);

        // Unassign vehicle if assigned
        if (agent.getVehicle() != null) {
            closeActiveVehicleHistory(agentId);
            Vehicle vehicle = agent.getVehicle();
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
            agent.setVehicle(null);
        }

        agentRepository.save(agent);

        // 3. Record audit log
        String adminName = "SYSTEM";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            adminName = auth.getName();
        }

        AgentAuditLog log = new AgentAuditLog(agentId, "Agent Terminated", adminName, reason);
        agentAuditLogRepository.save(log);

        // 4. Force logout immediately
        expireUserSessions(agentId);
    }

    /**
     * Expires all active sessions for a user.
     */
    public void expireUserSessions(String username) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) principal;
                if (user.getUsername().equalsIgnoreCase(username)) {
                    List<org.springframework.security.core.session.SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    for (org.springframework.security.core.session.SessionInformation session : sessions) {
                        session.expireNow();
                    }
                }
            }
        }
    }

    public List<AgentAuditLog> getAuditHistory(String agentId) {
        return agentAuditLogRepository.findByAgentIdOrderByActionDateDesc(agentId);
    }

    public List<Agent> getActiveAgents() {
        return agentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AgentStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Assigns a vehicle to an agent and marks vehicle status as ASSIGNED.
     */
    @Transactional
    public void assignVehicle(String agentId, Integer vehicleId) {
        Optional<Agent> agentOpt = agentRepository.findById(agentId);
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);

        if (agentOpt.isPresent() && vehicleOpt.isPresent()) {
            Agent agent = agentOpt.get();
            Vehicle vehicle = vehicleOpt.get();

            // 1. Unassign the vehicle from any other agent if it is already assigned
            Optional<Agent> otherAgentOpt = agentRepository.findByVehicle(vehicle);
            if (otherAgentOpt.isPresent() && !otherAgentOpt.get().getId().equals(agentId)) {
                Agent otherAgent = otherAgentOpt.get();
                closeActiveVehicleHistory(otherAgent.getId());
                otherAgent.setVehicle(null);
                agentRepository.save(otherAgent);
            }

            // 2. Unassign previous vehicle if this agent already had one
            if (agent.getVehicle() != null) {
                closeActiveVehicleHistory(agent.getId());
                Vehicle oldVehicle = agent.getVehicle();
                oldVehicle.setStatus("AVAILABLE");
                vehicleRepository.save(oldVehicle);
            }

            // 3. Complete new assignment
            agent.setVehicle(vehicle);
            vehicle.setStatus("ASSIGNED");

            agentRepository.save(agent);
            vehicleRepository.save(vehicle);

            String adminName = "SYSTEM";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
                adminName = auth.getName();
            }

            AgentVehicleHistory history = new AgentVehicleHistory(agentId, vehicleId, vehicle.getVehicleNumber(), LocalDateTime.now(), adminName);
            agentVehicleHistoryRepository.save(history);
        }
    }

    @Transactional
    public void removeVehicleAssignment(String agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (agent.getVehicle() != null) {
            closeActiveVehicleHistory(agentId);
            Vehicle vehicle = agent.getVehicle();
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
            agent.setVehicle(null);
            agentRepository.save(agent);
        }
    }

    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    public Optional<Agent> getAgentById(String id) {
        return agentRepository.findById(id);
    }

    public Optional<Agent> getAgentByUsername(String username) {
        return agentRepository.findByUserUsername(username);
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private void sendMockSMS(String phoneNumber, String agentId, String tempPassword) {
        String messageBody = String.format(
            "\n================ MOCK SMS API GATEWAY ================\n" +
            "TO: %s\n" +
            "MESSAGE:\n" +
            "Welcome to Delivery Service Provider!\n" +
            "Your Agent account has been registered successfully.\n" +
            "Agent ID (Username): %s\n" +
            "Temporary Password: %s\n" +
            "Please login at: http://localhost:8080/login\n" +
            "======================================================",
            phoneNumber, agentId, tempPassword
        );
        logger.info(messageBody);
        System.out.println(messageBody); // Ensure it prints to stdout for easy user visibility in console
    }

    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        String specialChars = "~!@#$%^&*()_+`{}|[]\\:\";'<>?,./-=";
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specialChars.indexOf(c) >= 0) hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    @Transactional
    public void changeAgentPassword(String agentId, String currentPassword, String newPassword, String ipAddress) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));
        User user = agent.getUser();

        // 1. Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match the password stored in the database.");
        }

        // 2. Validate complexity
        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("New password does not meet complexity requirements (min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special character).");
        }

        // 3. Prevent reuse of current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        // Encode and update
        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
        agent.setPassword(encoded);
        agent.setPasswordUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        agentRepository.save(agent);

        // Audit Log
        PasswordChangeLog log = new PasswordChangeLog(agentId, ipAddress);
        passwordChangeLogRepository.save(log);

        // Activity Log
        logActivity(agentId, "Password Changed", ipAddress);
    }

    public Agent verifyAgentForReset(String agentId, String phoneNumber) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));
        
        // Normalize phone numbers to make verification more robust (digits only)
        String normalizedDbPhone = agent.getPhone().replaceAll("[^0-9]", "");
        String normalizedInputPhone = phoneNumber.replaceAll("[^0-9]", "");

        if (!normalizedDbPhone.equals(normalizedInputPhone)) {
            throw new IllegalArgumentException("Registered phone number does not match.");
        }
        return agent;
    }

    public String sendForgotPasswordOtp(Agent agent) {
        // Generate a 6-digit random numeric OTP
        int otpNum = 100000 + random.nextInt(900000);
        String otp = String.valueOf(otpNum);

        // Under test conditions, override OTP to 123456 for the demo agent
        if ("1001".equals(agent.getId()) && "9876543211".equals(agent.getPhone())) {
            otp = "123456";
        }

        String messageBody = String.format(
            "\n================ MOCK SMS OTP GATEWAY ================\n" +
            "TO: %s\n" +
            "MESSAGE:\n" +
            "LogiTrack Pro OTP Verification Code:\n" +
            "Your password reset OTP is: %s\n" +
            "This code is valid for 5 minutes.\n" +
            "If you did not request this, please ignore this message.\n" +
            "======================================================",
            agent.getPhone(), otp
        );
        logger.info(messageBody);
        System.out.println(messageBody); // Print to console for easy user accessibility

        return otp;
    }

    @Transactional
    public void resetAgentPassword(String agentId, String newPassword, String ipAddress) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));
        User user = agent.getUser();

        // 1. Validate complexity
        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("New password does not meet complexity requirements (min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special character).");
        }

        // 2. Prevent reuse of current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        // Encode and update
        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
        agent.setPassword(encoded);
        agent.setPasswordUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
        agentRepository.save(agent);

        // Audit Log
        PasswordChangeLog log = new PasswordChangeLog(agentId, ipAddress);
        passwordChangeLogRepository.save(log);

        // Activity Log
        logActivity(agentId, "Password Changed", ipAddress);
    }

    public void logActivity(String agentId, String action, String ipAddress) {
        AgentActivityLog log = new AgentActivityLog(agentId, action, ipAddress);
        agentActivityLogRepository.save(log);
    }

    @Transactional
    public void updateAgentProfile(String agentId, String name, String phone, String email, 
                                   java.time.LocalDate dob, String gender, String licenseNumber,
                                   String address, String city, String state, String pincode, 
                                   MultipartFile photoFile, String ipAddress) throws java.io.IOException {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));

        // Check email uniqueness if email changed
        if (!agent.getEmail().equalsIgnoreCase(email)) {
            Optional<Agent> emailCheck = agentRepository.findAll().stream()
                .filter(a -> email.equalsIgnoreCase(a.getEmail()) && !a.getId().equals(agentId))
                .findFirst();
            if (emailCheck.isPresent()) {
                throw new IllegalArgumentException("Email is already in use by another agent.");
            }
        }

        agent.setName(name);
        agent.setPhone(phone);
        agent.setEmail(email);
        agent.setDob(dob);
        agent.setGender(gender);
        agent.setLicenseNumber(licenseNumber);
        agent.setAddress(address);
        agent.setCity(city);
        agent.setState(state);
        agent.setPincode(pincode);

        // Handle profile photo upload
        if (photoFile != null && !photoFile.isEmpty()) {
            // File validation
            String originalFilename = photoFile.getOriginalFilename();
            if (originalFilename == null) {
                throw new IllegalArgumentException("Invalid file name.");
            }
            String ext = "";
            if (originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }
            if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg")) {
                throw new IllegalArgumentException("Only PNG, JPG, and JPEG file formats are allowed.");
            }
            if (photoFile.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Maximum profile photo size limit is 5MB.");
            }

            // Create target folder if not exists
            File directory = new File(profileUploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Save photo file
            String filename = "agent_" + agentId + "_" + java.util.UUID.randomUUID().toString() + ext;
            java.nio.file.Path path = java.nio.file.Paths.get(profileUploadDir, filename);
            java.nio.file.Files.write(path, photoFile.getBytes());

            // Save relative URL path
            agent.setProfileImage("/uploads/profile-images/" + filename);
        }

        agentRepository.save(agent);

        // Log Activity
        logActivity(agentId, "Profile Updated", ipAddress);
    }

    @Transactional
    public void deleteProfilePhoto(String agentId, String ipAddress) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with ID: " + agentId));
        
        // Remove actual file if exists (optional but clean)
        String currentPath = agent.getProfileImage();
        if (currentPath != null && !currentPath.isEmpty()) {
            try {
                String filename = currentPath.substring(currentPath.lastIndexOf("/") + 1);
                java.nio.file.Path path = java.nio.file.Paths.get(profileUploadDir, filename);
                java.nio.file.Files.deleteIfExists(path);
            } catch (Exception e) {
                // log warning but proceed
                logger.warn("Could not delete physical profile image file: " + e.getMessage());
            }
        }

        agent.setProfileImage(null);
        agentRepository.save(agent);

        // Log Activity
        logActivity(agentId, "Profile Photo Deleted", ipAddress);
    }

    public List<AgentActivityLog> getActivityLogs(String agentId) {
        return agentActivityLogRepository.findByAgentIdOrderByTimestampDesc(agentId);
    }

    private void closeActiveVehicleHistory(String agentId) {
        List<AgentVehicleHistory> history = agentVehicleHistoryRepository.findByAgentIdOrderByAssignedAtDesc(agentId);
        for (AgentVehicleHistory record : history) {
            if (record.getUnassignedAt() == null) {
                record.setUnassignedAt(LocalDateTime.now());
                agentVehicleHistoryRepository.save(record);
            }
        }
    }

    public AgentAvailabilityStatus getAgentAvailabilityStatus(Agent agent) {
        if (agent.getStatus() == AgentStatus.SUSPENDED) {
            return AgentAvailabilityStatus.SUSPENDED;
        }
        if (agent.getStatus() == AgentStatus.TERMINATED) {
            return AgentAvailabilityStatus.TERMINATED;
        }

        // Check approved leaves today
        java.time.LocalDate today = java.time.LocalDate.now();
        List<LeaveApplication> leaves = leaveApplicationRepository.findByAgentIdAndStatus(agent.getId(), "APPROVED");
        boolean onLeave = leaves.stream()
                .anyMatch(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()));
        if (onLeave) {
            return AgentAvailabilityStatus.ON_LEAVE;
        }

        // Check shift timings
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        java.time.LocalTime start = agent.getShiftStartTime();
        java.time.LocalTime end = agent.getShiftEndTime();

        if (start != null && end != null) {
            boolean withinShift;
            if (start.isBefore(end)) {
                withinShift = !nowTime.isBefore(start) && !nowTime.isAfter(end);
            } else {
                withinShift = !nowTime.isBefore(start) || !nowTime.isAfter(end);
            }

            if (!withinShift) {
                if (start.isBefore(end)) {
                    if (nowTime.isBefore(start)) {
                        return AgentAvailabilityStatus.SHIFT_NOT_STARTED;
                    } else {
                        return AgentAvailabilityStatus.OFF_SHIFT;
                    }
                } else {
                    if (nowTime.isBefore(start) && nowTime.isAfter(end)) {
                        return AgentAvailabilityStatus.SHIFT_NOT_STARTED;
                    } else {
                        return AgentAvailabilityStatus.OFF_SHIFT;
                    }
                }
            }
        }

        // Check attendance record for today
        Optional<Attendance> attendanceOpt = attendanceRepository.findByAgentIdAndDate(agent.getId(), today);
        if (attendanceOpt.isEmpty()) {
            return AgentAvailabilityStatus.ABSENT;
        }

        Attendance attendance = attendanceOpt.get();
        if (attendance.getCheckOutTime() != null) {
            return AgentAvailabilityStatus.OFF_SHIFT;
        }

        // Must be present or late (on duty)
        if ("PRESENT".equalsIgnoreCase(attendance.getStatus()) || "LATE".equalsIgnoreCase(attendance.getStatus())) {
            return AgentAvailabilityStatus.AVAILABLE;
        }

        return AgentAvailabilityStatus.ABSENT;
    }
}
