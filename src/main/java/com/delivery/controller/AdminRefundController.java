package com.delivery.controller;

import com.delivery.service.CancellationRefundService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminRefundController {

    private final CancellationRefundService cancellationRefundService;

    public AdminRefundController(CancellationRefundService cancellationRefundService) {
        this.cancellationRefundService = cancellationRefundService;
    }

    @GetMapping("/refunds")
    public String refundsPage(Model model) {
        Map<String, Object> stats = cancellationRefundService.getRefundStatistics();
        model.addAllAttributes(stats);
        model.addAttribute("cancellations", cancellationRefundService.getAllCancellations());
        model.addAttribute("refunds", cancellationRefundService.getAllRefunds());
        return "admin/refunds";
    }

    @PostMapping("/refunds/approve/{id}")
    public String approveRefund(@PathVariable("id") Integer refundId, RedirectAttributes redirectAttributes) {
        try {
            cancellationRefundService.approveManualRefund(refundId);
            redirectAttributes.addFlashAttribute("successMessage", "Refund approved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval failed: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    @PostMapping("/refunds/reject/{id}")
    public String rejectRefund(@PathVariable("id") Integer refundId, RedirectAttributes redirectAttributes) {
        try {
            cancellationRefundService.rejectManualRefund(refundId);
            redirectAttributes.addFlashAttribute("successMessage", "Refund request rejected successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rejection failed: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}
