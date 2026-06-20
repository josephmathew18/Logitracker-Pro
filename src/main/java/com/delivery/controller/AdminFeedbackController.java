package com.delivery.controller;

import com.delivery.model.Feedback;
import com.delivery.service.FeedbackService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Display Admin Feedback Dashboard with statistics, search, and filters.
     */
    @GetMapping("/feedback")
    public String adminFeedbackDashboard(
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        // Perform filtered search
        List<Feedback> filteredFeedback = feedbackService.searchAndFilterFeedback(rating, start, end, search);
        
        // Fetch dashboard statistics
        Map<String, Object> stats = feedbackService.getFeedbackStatistics();

        model.addAttribute("feedbacks", filteredFeedback);
        model.addAttribute("stats", stats);
        model.addAttribute("ratingFilter", rating);
        model.addAttribute("startDateFilter", startDate);
        model.addAttribute("endDateFilter", endDate);
        model.addAttribute("searchFilter", search);

        return "admin/feedback";
    }

    /**
     * Display Feedback Analytics Reports page.
     */
    @GetMapping("/feedback/reports")
    public String feedbackReports(Model model) {
        model.addAttribute("agentPerformance", feedbackService.getAgentPerformanceReport());
        model.addAttribute("customerSatisfaction", feedbackService.getCustomerSatisfactionReport());
        model.addAttribute("ratingAnalytics", feedbackService.getRatingAnalytics());
        model.addAttribute("monthlyReport", feedbackService.getMonthlyFeedbackReport());
        return "admin/feedback_reports";
    }
}
