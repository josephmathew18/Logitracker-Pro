package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AgentPerformanceService {

    private final AgentPerformanceRepository agentPerformanceRepository;
    private final AgentRepository agentRepository;
    private final DeliveryRepository deliveryRepository;
    private final FeedbackRepository feedbackRepository;

    public AgentPerformanceService(AgentPerformanceRepository agentPerformanceRepository,
                                   AgentRepository agentRepository,
                                   DeliveryRepository deliveryRepository,
                                   FeedbackRepository feedbackRepository) {
        this.agentPerformanceRepository = agentPerformanceRepository;
        this.agentRepository = agentRepository;
        this.deliveryRepository = deliveryRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public AgentPerformance calculatePerformance(String agentId, int month, int year) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found."));

        // Fetch deliveries for this agent
        List<Delivery> deliveries = deliveryRepository.findByAgentUserUsername(agent.getUser().getUsername());

        // Filter by month and year
        List<Delivery> monthlyDeliveries = deliveries.stream()
                .filter(d -> d.getCreatedAt() != null &&
                             d.getCreatedAt().getMonthValue() == month &&
                             d.getCreatedAt().getYear() == year)
                .collect(Collectors.toList());

        int completed = 0;
        int cancelled = 0;
        for (Delivery d : monthlyDeliveries) {
            if ("DELIVERED".equalsIgnoreCase(d.getStatus())) {
                completed++;
            } else if ("REJECTED".equalsIgnoreCase(d.getStatus())) {
                cancelled++;
            }
        }

        int totalAssigned = monthlyDeliveries.size();
        double successRate = totalAssigned > 0 ? ((double) completed / totalAssigned) * 100.0 : 100.0;

        // Fetch ratings
        List<Feedback> feedbackList = feedbackRepository.findAll().stream()
                .filter(f -> f.getAgent() != null && f.getAgent().getId().equals(agentId))
                .collect(Collectors.toList());

        double avgRating = 5.0; // default rating
        if (!feedbackList.isEmpty()) {
            double totalStars = feedbackList.stream().mapToDouble(Feedback::getRating).sum();
            avgRating = totalStars / feedbackList.size();
        }

        // Performance Score = (Success Rate * 0.6) + ((Avg Rating / 5.0) * 40.0)
        double score = (successRate * 0.6) + ((avgRating / 5.0) * 40.0);
        score = Math.round(score * 100.0) / 100.0; // Round to 2 decimal places

        AgentPerformance perf = agentPerformanceRepository.findByAgentIdAndMonthAndYear(agentId, month, year)
                .orElse(new AgentPerformance(agent, month, year));

        perf.setCompletedCount(completed);
        perf.setCancelledCount(cancelled);
        perf.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
        perf.setAverageRating(Math.round(avgRating * 100.0) / 100.0);
        perf.setPerformanceScore(score);

        return agentPerformanceRepository.save(perf);
    }

    @Transactional
    public List<AgentPerformance> generateLeaderboard(int month, int year) {
        List<Agent> agents = agentRepository.findAll();
        for (Agent agent : agents) {
            calculatePerformance(agent.getId(), month, year);
        }

        List<AgentPerformance> leaderboard = agentPerformanceRepository.findByMonthAndYearOrderByPerformanceScoreDesc(month, year);

        // Sort and apply rankings
        for (int i = 0; i < leaderboard.size(); i++) {
            AgentPerformance perf = leaderboard.get(i);
            perf.setRanking(i + 1);
            agentPerformanceRepository.save(perf);
        }

        return agentPerformanceRepository.findByMonthAndYearOrderByPerformanceScoreDesc(month, year);
    }

    public List<AgentPerformance> getPerformanceHistory(String agentId) {
        return agentPerformanceRepository.findByAgentIdOrderByYearDescMonthDesc(agentId);
    }

    public AgentPerformance getLatestPerformance(String agentId) {
        List<AgentPerformance> history = getPerformanceHistory(agentId);
        if (!history.isEmpty()) {
            return history.get(0);
        }
        // Fallback dummy
        Agent agent = agentRepository.findById(agentId).orElse(null);
        if (agent != null) {
            AgentPerformance dummy = new AgentPerformance(agent, LocalDate.now().getMonthValue(), LocalDate.now().getYear());
            dummy.setPerformanceScore(85.0);
            dummy.setSuccessRate(100.0);
            dummy.setAverageRating(4.5);
            dummy.setRanking(1);
            return dummy;
        }
        return null;
    }
}
