package com.delivery.controller;

import com.delivery.model.Order;
import com.delivery.repository.OrderRepository;
import com.delivery.service.CancellationRefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/customer")
public class CustomerCancellationController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerCancellationController.class);

    private final CancellationRefundService cancellationRefundService;
    private final OrderRepository orderRepository;

    public CustomerCancellationController(CancellationRefundService cancellationRefundService, OrderRepository orderRepository) {
        this.cancellationRefundService = cancellationRefundService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/cancellations")
    public String cancellationsPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("cancellations", cancellationRefundService.getCancellationsByCustomer(username));
        model.addAttribute("refunds", cancellationRefundService.getRefundsByCustomer(username));
        return "customer/cancellations";
    }

    @PostMapping("/orders/cancel")
    public String cancelOrder(@RequestParam(value = "orderId", required = false) Integer orderId,
                              @RequestParam(value = "deliveryId", required = false) Integer deliveryId,
                              @RequestParam("reason") String reason,
                              @RequestHeader(value = "Referer", required = false) String referer,
                              RedirectAttributes redirectAttributes) {
        Integer targetOrderId = orderId;
        logger.debug("[DEBUG LOG] Received cancellation request parameter - orderId: {}, deliveryId: {}, reason: {}", orderId, deliveryId, reason);

        try {
            if (targetOrderId == null && deliveryId != null) {
                Optional<Order> orderOpt = orderRepository.findByDeliveryId(deliveryId);
                if (orderOpt.isPresent()) {
                    targetOrderId = orderOpt.get().getOrderId();
                } else {
                    throw new IllegalArgumentException("No order found associated with Delivery ID: " + deliveryId);
                }
            }

            if (targetOrderId == null) {
                throw new IllegalArgumentException("Order ID or Delivery ID must be provided for cancellation.");
            }

            // Load and log order details before execution
            Optional<Order> orderToLog = orderRepository.findById(targetOrderId);
            if (orderToLog.isPresent()) {
                Order order = orderToLog.get();
                logger.debug("[DEBUG LOG] Controller Cancellation Details - Order ID: {}, Delivery Status: {}, Payment Status: {}", 
                             order.getOrderId(), order.getDeliveryStatus(), order.getPaymentStatus());
            } else {
                logger.debug("[DEBUG LOG] Controller Cancellation Details - Order ID: {} not found in db", targetOrderId);
            }

            cancellationRefundService.cancelOrder(targetOrderId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Order cancellation request processed successfully!");
        } catch (Exception e) {
            logger.error("[DEBUG LOG] Cancellation request failed", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }

        // Conditional redirect based on referer
        String redirectTarget = "redirect:/customer/dashboard";
        if (referer != null && (referer.contains("/orders") || referer.contains("/orders/create"))) {
            redirectTarget = "redirect:/customer/orders";
        }
        return redirectTarget;
    }
}
