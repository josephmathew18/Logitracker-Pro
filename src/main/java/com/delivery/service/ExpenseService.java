package com.delivery.service;

import com.delivery.model.Agent;
import com.delivery.model.FuelExpense;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.FuelExpenseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service to manage fuel expenses, bill image file uploads, and logs.
 */
@Service
public class ExpenseService {

    private final FuelExpenseRepository fuelExpenseRepository;
    private final AgentRepository agentRepository;

    @Value("${upload.dir}")
    private String uploadDir;

    public ExpenseService(FuelExpenseRepository fuelExpenseRepository, AgentRepository agentRepository) {
        this.fuelExpenseRepository = fuelExpenseRepository;
        this.agentRepository = agentRepository;
    }

    /**
     * Uploads fuel bill image and stores the expense record.
     */
    @Transactional
    public FuelExpense logFuelExpense(String agentUsername, LocalDate date, BigDecimal quantity,
                                      BigDecimal price, MultipartFile billFile) throws IOException {
        Agent agent = agentRepository.findByUserUsername(agentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found for username: " + agentUsername));

        String savedFilePath = null;

        // Process file upload if file is not empty
        if (billFile != null && !billFile.isEmpty()) {
            // Ensure directory exists
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create a unique filename to prevent collisions
            String fileExtension = "";
            String originalFilename = billFile.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = agent.getId() + "_" + UUID.randomUUID().toString() + fileExtension;

            // Save file to disk
            Path path = Paths.get(uploadDir, uniqueFilename);
            Files.write(path, billFile.getBytes());

            // Save relative URL path to database
            savedFilePath = "/uploads/bills/" + uniqueFilename;
        }

        // Calculate total cost
        BigDecimal total = quantity.multiply(price);

        FuelExpense expense = new FuelExpense();
        expense.setAgent(agent);
        expense.setExpenseDate(date);
        expense.setQuantity(quantity);
        expense.setPrice(price);
        expense.setTotal(total);
        expense.setBillImagePath(savedFilePath);

        return fuelExpenseRepository.save(expense);
    }

    public List<FuelExpense> getExpensesByAgent(String username) {
        return fuelExpenseRepository.findByAgentUserUsername(username);
    }

    public List<FuelExpense> getAllExpenses() {
        return fuelExpenseRepository.findAll();
    }
}
