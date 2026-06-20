package com.delivery.controller;

import com.delivery.model.Feedback;
import com.delivery.service.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerFeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerFeedbackController.class);

    private final FeedbackService feedbackService;

    public CustomerFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Display Customer Feedback History page.
     */
    @GetMapping("/feedback")
    public String feedbackHistoryPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<Feedback> feedbackList = feedbackService.getFeedbackByCustomer(username);
        model.addAttribute("feedbacks", feedbackList);
        return "customer/feedback";
    }

    /**
     * Submit or Edit Customer Feedback.
     */
    @PostMapping("/feedback/submit")
    public String submitFeedback(Authentication authentication,
                                 @RequestParam(value = "orderId", required = false) Integer orderId,
                                 @RequestParam(value = "feedbackId", required = false) Integer feedbackId,
                                 @RequestParam(value = "rating", required = false) Integer rating,
                                 @RequestParam("category") String category,
                                 @RequestParam(value = "comments", required = false) String comments,
                                 RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        logger.debug("[DEBUG LOG] Feedback submit request - orderId: {}, feedbackId: {}, rating: {}, category: {}, comments: {}",
                orderId, feedbackId, rating, category, comments);

        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid star rating (1-5).");
            return orderId != null ? "redirect:/customer/orders" : "redirect:/customer/feedback";
        }

        try {
            if (feedbackId != null) {
                // Editing existing feedback (must be within 24 hours)
                feedbackService.updateFeedback(feedbackId, rating, category, comments);
                redirectAttributes.addFlashAttribute("successMessage", "Feedback updated successfully!");
            } else if (orderId != null) {
                // Submitting new feedback
                feedbackService.submitFeedback(orderId, username, rating, category, comments);
                redirectAttributes.addFlashAttribute("successMessage", "Thank you for your feedback!");
            } else {
                throw new IllegalArgumentException("Either Order ID or Feedback ID must be provided.");
            }
        } catch (Exception e) {
            logger.error("[DEBUG LOG] Feedback submission failed", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/customer/feedback";
    }
}
