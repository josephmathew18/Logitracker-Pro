package com.delivery.service;

import com.delivery.model.Customer;
import com.delivery.model.User;
import com.delivery.repository.CustomerRepository;
import com.delivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Service to manage User and Customer accounts, registration, and profile lookup.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${upload.profile.dir:uploads/profile-images}")
    private String profileUploadDir;

    public UserService(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new customer into the database.
     */
    @Transactional
    public Customer registerCustomer(String username, String password, String name, String phone, String email, String address) {
        // Create User entity
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("CUSTOMER");
        user.setActive(true);

        // Create Customer profile linked to User
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(address);

        return customerRepository.save(customer);
    }

    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean emailExists(String email) {
        return customerRepository.findByEmail(email).isPresent();
    }

    public Optional<Customer> getCustomerByUsername(String username) {
        return customerRepository.findByUserUsername(username);
    }

    public Customer verifyCustomerForReset(String username, String phoneNumber) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with Username: " + username));
        
        String normalizedDbPhone = customer.getPhone().replaceAll("[^0-9]", "");
        String normalizedInputPhone = phoneNumber.replaceAll("[^0-9]", "");

        if (!normalizedDbPhone.equals(normalizedInputPhone)) {
            throw new IllegalArgumentException("Registered phone number does not match.");
        }
        return customer;
    }

    public String sendForgotPasswordOtp(Customer customer) {
        int otpNum = 100000 + new java.security.SecureRandom().nextInt(900000);
        String otp = String.valueOf(otpNum);

        // Fallback for customer testing
        if ("customer".equals(customer.getUser().getUsername())) {
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
            customer.getPhone(), otp
        );
        System.out.println(messageBody);

        return otp;
    }

    @Transactional
    public void resetCustomerPassword(String username, String newPassword) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with Username: " + username));
        User user = customer.getUser();

        // 1. Validate complexity
        if (!AgentService.isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("New password does not meet complexity requirements (min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special character).");
        }

        // 2. Prevent reuse of current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateCustomerProfile(Integer id, String name, String phone, String email, String address, String city, String state, String pincode, MultipartFile photoFile) throws java.io.IOException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone cannot be empty.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }

        // Check if email is taken by another customer
        Optional<Customer> emailOwner = customerRepository.findByEmail(email);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(id)) {
            throw new IllegalArgumentException("Email is already in use by another account.");
        }

        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(address);
        customer.setCity(city);
        customer.setState(state);
        customer.setPincode(pincode);

        // Handle profile photo upload
        if (photoFile != null && !photoFile.isEmpty()) {
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

            // Delete old photo if exists
            String currentPath = customer.getProfileImage();
            if (currentPath != null && !currentPath.isEmpty()) {
                try {
                    String oldFilename = currentPath.substring(currentPath.lastIndexOf("/") + 1);
                    Path oldPath = Paths.get(profileUploadDir, oldFilename);
                    Files.deleteIfExists(oldPath);
                } catch (Exception e) {
                    System.err.println("Could not delete old profile image file: " + e.getMessage());
                }
            }

            // Save photo file
            String filename = "customer_" + id + "_" + java.util.UUID.randomUUID().toString() + ext;
            Path path = Paths.get(profileUploadDir, filename);
            Files.write(path, photoFile.getBytes());

            // Save relative URL path
            customer.setProfileImage("/uploads/profile-images/" + filename);
        }

        customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomerProfilePhoto(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));

        String currentPath = customer.getProfileImage();
        if (currentPath != null && !currentPath.isEmpty()) {
            try {
                String filename = currentPath.substring(currentPath.lastIndexOf("/") + 1);
                Path path = Paths.get(profileUploadDir, filename);
                Files.deleteIfExists(path);
            } catch (Exception e) {
                System.err.println("Could not delete physical profile image file: " + e.getMessage());
            }
        }

        customer.setProfileImage(null);
        customerRepository.save(customer);
    }

    @Transactional
    public void updateCustomerPassword(Integer id, String currentPassword, String newPassword) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + id));
        User user = customer.getUser();

        // Check current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match.");
        }

        // Validate new password complexity
        if (!AgentService.isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("New password does not meet complexity requirements (min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special character).");
        }

        // Prevent reuse of current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
