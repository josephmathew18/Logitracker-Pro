package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage customer feedback and reviews.
 */
@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           OrderRepository orderRepository,
                           CustomerRepository customerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Submits a customer feedback for a delivered order.
     */
    @Transactional
    public Feedback submitFeedback(Integer orderId, String customerUsername, Integer rating, String category, String comments) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating is mandatory and must be between 1 and 5 stars.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback category is mandatory.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!"DELIVERED".equalsIgnoreCase(order.getDeliveryStatus())) {
            throw new IllegalStateException("Feedback can only be submitted for completed (DELIVERED) orders.");
        }

        // Check if feedback already exists for this order
        Optional<Feedback> existing = feedbackRepository.findByOrderOrderId(orderId);
        if (existing.isPresent()) {
            throw new IllegalStateException("You have already submitted feedback for this order.");
        }

        Customer customer = customerRepository.findByUserUsername(customerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found."));

        Agent agent = (order.getDelivery() != null) ? order.getDelivery().getAgent() : null;

        Feedback feedback = new Feedback(order, customer, agent, rating, category.trim(), comments);
        return feedbackRepository.save(feedback);
    }

    /**
     * Updates an existing feedback if edited within 24 hours of creation.
     */
    @Transactional
    public Feedback updateFeedback(Integer feedbackId, Integer rating, String category, String comments) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating is mandatory and must be between 1 and 5 stars.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback category is mandatory.");
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback record not found."));

        // Limit edits to 24 hours
        if (LocalDateTime.now().isAfter(feedback.getCreatedAt().plusHours(24))) {
            throw new IllegalStateException("Feedback can only be modified within 24 hours of submission.");
        }

        feedback.setRating(rating);
        feedback.setCategory(category.trim());
        feedback.setComments(comments);
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Feedback> getFeedbackByOrderId(Integer orderId) {
        return feedbackRepository.findByOrderOrderId(orderId);
    }

    public List<Feedback> getFeedbackByCustomer(String username) {
        return feedbackRepository.findByCustomerUserUsernameOrderByCreatedAtDesc(username);
    }

    public List<Feedback> getFeedbackByAgent(String agentId) {
        return feedbackRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    public List<Feedback> searchAndFilterFeedback(Integer rating, LocalDateTime startDate, LocalDateTime endDate, String search) {
        return feedbackRepository.searchAndFilterFeedback(rating, startDate, endDate, search);
    }

    /**
     * Gets feedback dashboard statistics for Admin.
     */
    public Map<String, Object> getFeedbackStatistics() {
        List<Feedback> all = feedbackRepository.findAll();
        long total = all.size();
        
        double avg = all.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
        
        // Round to 2 decimal places
        BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        long fiveStar = all.stream().filter(f -> f.getRating() == 5).count();
        long negative = all.stream().filter(f -> f.getRating() <= 2).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedbacks", total);
        stats.put("averageRating", avgRounded);
        stats.put("fiveStarReviews", fiveStar);
        stats.put("negativeReviews", negative);
        return stats;
    }

    /**
     * Agent-specific statistics.
     */
    public Map<String, Object> getAgentFeedbackStatistics(String agentId) {
        List<Feedback> agentFeedback = feedbackRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
        long total = agentFeedback.size();
        
        double avg = agentFeedback.stream()
                .mapToInt(Feedback::getRating)
                .average()
                .orElse(0.0);
        BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedbacks", total);
        stats.put("averageRating", avgRounded);
        stats.put("history", agentFeedback);
        return stats;
    }

    /**
     * Generates report data for Agent Performance.
     */
    public List<Map<String, Object>> getAgentPerformanceReport() {
        List<Feedback> all = feedbackRepository.findAll();
        // Group by Agent ID (exclude null agent feedbacks)
        Map<Agent, List<Feedback>> grouped = all.stream()
                .filter(f -> f.getAgent() != null)
                .collect(Collectors.groupingBy(Feedback::getAgent));

        List<Map<String, Object>> report = new ArrayList<>();
        for (Map.Entry<Agent, List<Feedback>> entry : grouped.entrySet()) {
            Agent agent = entry.getKey();
            List<Feedback> list = entry.getValue();

            double avg = list.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
            BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> agentMap = new HashMap<>();
            agentMap.put("agentId", agent.getId());
            agentMap.put("agentName", agent.getName());
            agentMap.put("agentPhone", agent.getPhone());
            agentMap.put("totalFeedbacks", list.size());
            agentMap.put("averageRating", avgRounded);
            report.add(agentMap);
        }

        // Sort by average rating descending
        report.sort((a, b) -> ((BigDecimal) b.get("averageRating")).compareTo((BigDecimal) a.get("averageRating")));
        return report;
    }

    /**
     * Generates report data for Customer Satisfaction (Category breakdown).
     */
    public List<Map<String, Object>> getCustomerSatisfactionReport() {
        List<Feedback> all = feedbackRepository.findAll();
        Map<String, List<Feedback>> grouped = all.stream()
                .collect(Collectors.groupingBy(Feedback::getCategory));

        List<Map<String, Object>> report = new ArrayList<>();
        for (Map.Entry<String, List<Feedback>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<Feedback> list = entry.getValue();

            double avg = list.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
            BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("category", category);
            categoryMap.put("totalFeedbacks", list.size());
            categoryMap.put("averageRating", avgRounded);
            report.add(categoryMap);
        }
        
        report.sort((a, b) -> ((Integer) b.get("totalFeedbacks")).compareTo((Integer) a.get("totalFeedbacks")));
        return report;
    }

    /**
     * Generates report data for Rating Analytics (1 to 5 Star Distribution).
     */
    public Map<Integer, Long> getRatingAnalytics() {
        List<Feedback> all = feedbackRepository.findAll();
        Map<Integer, Long> distribution = all.stream()
                .collect(Collectors.groupingBy(Feedback::getRating, Collectors.counting()));

        // Fill in missing rating keys with 0
        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }
        return distribution;
    }

    /**
     * Generates Monthly Feedback Report (grouped by Month-Year).
     */
    public List<Map<String, Object>> getMonthlyFeedbackReport() {
        List<Feedback> all = feedbackRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, List<Feedback>> grouped = all.stream()
                .collect(Collectors.groupingBy(f -> f.getCreatedAt().format(formatter)));

        List<Map<String, Object>> report = new ArrayList<>();
        for (Map.Entry<String, List<Feedback>> entry : grouped.entrySet()) {
            String month = entry.getKey();
            List<Feedback> list = entry.getValue();

            double avg = list.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
            BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> monthMap = new HashMap<>();
            monthMap.put("month", month);
            monthMap.put("totalFeedbacks", list.size());
            monthMap.put("averageRating", avgRounded);
            report.add(monthMap);
        }

        // Sort chronologically ascending
        report.sort(Comparator.comparing(a -> (String) a.get("month")));
        return report;
    }
}
