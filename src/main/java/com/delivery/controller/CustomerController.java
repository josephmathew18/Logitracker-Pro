package com.delivery.controller;

import com.delivery.model.*;
import com.delivery.service.*;
import com.delivery.repository.OrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final DeliveryService deliveryService;
    private final UserService userService;
    private final FeedbackService feedbackService;
    private final PaymentService paymentService;
    private final PdfGenerationService pdfGenerationService;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CustomerController(DeliveryService deliveryService, UserService userService, 
                              FeedbackService feedbackService, PaymentService paymentService,
                              PdfGenerationService pdfGenerationService, OrderRepository orderRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.deliveryService = deliveryService;
        this.userService = userService;
        this.feedbackService = feedbackService;
        this.paymentService = paymentService;
        this.pdfGenerationService = pdfGenerationService;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<Delivery> history = deliveryService.getDeliveriesByCustomer(username);
        Optional<Customer> customerOpt = userService.getCustomerByUsername(username);

        customerOpt.ifPresent(customer -> model.addAttribute("customer", customer));
        model.addAttribute("deliveries", history);
        return "customer/dashboard";
    }

    @GetMapping("/track/{id}")
    public String trackDelivery(@PathVariable("id") Integer id, Model model) {
        Optional<Delivery> deliveryOpt = deliveryService.getDeliveryById(id);
        if (deliveryOpt.isPresent()) {
            model.addAttribute("delivery", deliveryOpt.get());
            model.addAttribute("trackingLogs", deliveryService.getTrackingHistory(id));
            return "customer/track";
        }
        return "redirect:/customer/dashboard";
    }

    @PostMapping("/feedback")
    public String submitFeedback(Authentication authentication,
                                 @RequestParam("deliveryId") Integer deliveryId,
                                 @RequestParam("rating") Integer rating,
                                 @RequestParam("comments") String comments,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            Order order = orderRepository.findByDeliveryId(deliveryId)
                    .orElseThrow(() -> new IllegalArgumentException("No order associated with this delivery ID."));
            feedbackService.submitFeedback(order.getOrderId(), username, rating, "Delivery Service", comments);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for your feedback!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit feedback: " + e.getMessage());
        }
        return "redirect:/customer/dashboard";
    }

    @GetMapping("/orders")
    public String ordersPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("orders", paymentService.getOrdersByCustomer(username));
        return "customer/orders";
    }

    @PostMapping("/orders/create")
    public String createOrder(Authentication authentication,
                              @RequestParam(value = "productName", required = false) String productName,
                              @RequestParam(value = "productCategory", required = false) String productCategory,
                              @RequestParam(value = "productPrice", required = false) BigDecimal productPrice,
                              @RequestParam(value = "quantity", required = false) Integer quantity,
                              @RequestParam("pickupAddress") String pickupAddress,
                              @RequestParam("deliveryAddress") String deliveryAddress,
                              @RequestParam(value = "packageDescription", required = false) String packageDescription,
                              @RequestParam(value = "parcelDescription", required = false) String parcelDescription,
                              @RequestParam(value = "packageWeight", required = false) BigDecimal packageWeight,
                              @RequestParam(value = "parcelWeight", required = false) BigDecimal parcelWeight,
                              @RequestParam(value = "packageType", required = false) String packageType,
                              @RequestParam("paymentMethod") String paymentMethod,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            String finalDesc = parcelDescription != null ? parcelDescription : packageDescription;
            BigDecimal finalWeight = parcelWeight != null ? parcelWeight : packageWeight;
            String finalType = packageType != null ? packageType : "Document";

            Order order = paymentService.createOrder(username, productName, productCategory, productPrice, quantity, pickupAddress, deliveryAddress, finalDesc, finalWeight, finalType);
            if ("COD".equalsIgnoreCase(paymentMethod)) {
                paymentService.processPayment(order.getOrderId(), "COD", "COD-" + System.currentTimeMillis());
                redirectAttributes.addFlashAttribute("successMessage", "Delivery order successfully created! Payment mode: Cash On Delivery.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Order registered. Launching Razorpay test transaction...");
                return "redirect:/customer/orders?payOrderId=" + order.getOrderId();
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Order placement failed: " + e.getMessage());
        }
        return "redirect:/customer/orders";
    }

    @PostMapping("/orders/create/ajax")
    @ResponseBody
    public ResponseEntity<?> createOrderAjax(Authentication authentication,
                                             @RequestParam(value = "productName", required = false) String productName,
                                             @RequestParam(value = "productCategory", required = false) String productCategory,
                                             @RequestParam(value = "productPrice", required = false) BigDecimal productPrice,
                                             @RequestParam(value = "quantity", required = false) Integer quantity,
                                             @RequestParam("pickupAddress") String pickupAddress,
                                             @RequestParam("deliveryAddress") String deliveryAddress,
                                             @RequestParam(value = "packageDescription", required = false) String packageDescription,
                                             @RequestParam(value = "parcelDescription", required = false) String parcelDescription,
                                             @RequestParam(value = "packageWeight", required = false) BigDecimal packageWeight,
                                             @RequestParam(value = "parcelWeight", required = false) BigDecimal parcelWeight,
                                             @RequestParam(value = "packageType", required = false) String packageType,
                                             @RequestParam("paymentMethod") String paymentMethod) {
        try {
            String username = authentication.getName();
            String finalDesc = parcelDescription != null ? parcelDescription : packageDescription;
            BigDecimal finalWeight = parcelWeight != null ? parcelWeight : packageWeight;
            String finalType = packageType != null ? packageType : "Document";

            Order order = paymentService.createOrder(username, productName, productCategory, productPrice, quantity, pickupAddress, deliveryAddress, finalDesc, finalWeight, finalType);
            if ("COD".equalsIgnoreCase(paymentMethod)) {
                paymentService.processPayment(order.getOrderId(), "COD", "COD-" + System.currentTimeMillis());
                return ResponseEntity.ok(Map.of("status", "success", "paymentMethod", "COD", "redirectUrl", "/customer/orders"));
            } else {
                Map<String, Object> orderData = paymentService.createRazorpayOrder(order.getOrderId());
                return ResponseEntity.ok(Map.of("status", "success", "paymentMethod", "RAZORPAY", "orderData", orderData));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @PostMapping("/orders/pay")
    public String payOrder(@RequestParam("orderId") Integer orderId,
                           @RequestParam("paymentMethod") String paymentMethod,
                           @RequestParam(value = "transactionId", required = false) String transactionId,
                           RedirectAttributes redirectAttributes) {
        try {
            String txnId = transactionId;
            if (txnId == null || txnId.trim().isEmpty()) {
                txnId = "TXN-" + System.currentTimeMillis();
            }
            paymentService.processPayment(orderId, paymentMethod, txnId);
            redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment failed: " + e.getMessage());
        }
        return "redirect:/customer/orders";
    }

    @GetMapping("/orders/pay/razorpay")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> payOrderRazorpay(@RequestParam("orderId") Integer orderId) {
        try {
            java.util.Map<String, Object> orderData = paymentService.createRazorpayOrder(orderId);
            return org.springframework.http.ResponseEntity.ok(orderData);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/orders/pay/verify")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> verifyOrderRazorpay(@RequestParam("orderId") Integer orderId,
                                                                          @RequestParam("razorpay_order_id") String razorpayOrderId,
                                                                          @RequestParam("razorpay_payment_id") String razorpayPaymentId,
                                                                          @RequestParam("razorpay_signature") String signature) {
        try {
            paymentService.verifyPaymentSignature(orderId, razorpayOrderId, razorpayPaymentId, signature);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("status", "success"));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("status", "failed", "error", e.getMessage()));
        }
    }


    @GetMapping("/orders/invoice/{id}")
    public String getInvoice(@PathVariable("id") Integer id, Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Order> orderOpt = paymentService.getOrderById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getCustomer().getUser().getUsername().equals(username)) {
                model.addAttribute("order", order);
                return "customer/invoice";
            }
        }
        return "redirect:/customer/orders";
    }

    @GetMapping("/orders/invoice/download/{id}")
    public void downloadInvoice(@PathVariable("id") Integer id, Authentication authentication, 
                                jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String username = authentication.getName();
        Optional<Order> orderOpt = paymentService.getOrderById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getCustomer().getUser().getUsername().equals(username)) {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf");
                pdfGenerationService.generateOrderInvoice(order, response.getOutputStream());
                return;
            }
        }
        response.sendRedirect("/customer/orders");
    }

    @GetMapping("/orders/failed")
    public String paymentFailed(@RequestParam("orderId") Integer orderId, Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Order> orderOpt = paymentService.getOrderById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getCustomer().getUser().getUsername().equals(username)) {
                model.addAttribute("order", order);
                
                // Fetch the payment to display the logged failure reason
                Optional<Payment> paymentOpt = paymentService.getPaymentsByCustomer(username).stream()
                        .filter(p -> p.getOrder().getOrderId().equals(orderId))
                        .findFirst();
                
                String reason = "Payment session was dismissed or signature verification failed.";
                if (paymentOpt.isPresent() && paymentOpt.get().getFailureReason() != null) {
                    reason = paymentOpt.get().getFailureReason();
                }
                model.addAttribute("failureReason", reason);
                return "customer/failed";
            }
        }
        return "redirect:/customer/orders";
    }


    @GetMapping("/payments")
    public String paymentsPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        model.addAttribute("payments", paymentService.getPaymentsByCustomer(username));
        return "customer/payments";
    }

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Customer> customerOpt = userService.getCustomerByUsername(username);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            model.addAttribute("customer", customer);

            // Compute Quick Stats
            List<Delivery> deliveries = deliveryService.getDeliveriesByCustomer(username);
            long totalDeliveries = deliveries.size();
            long completedDeliveries = deliveries.stream()
                    .filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus()))
                    .count();
            long pendingDeliveries = deliveries.stream()
                    .filter(d -> !"DELIVERED".equalsIgnoreCase(d.getStatus()) && !"REJECTED".equalsIgnoreCase(d.getStatus()))
                    .count();

            List<Order> orders = paymentService.getOrdersByCustomer(username);
            long totalOrders = orders.size();

            model.addAttribute("totalDeliveries", totalDeliveries);
            model.addAttribute("completedDeliveries", completedDeliveries);
            model.addAttribute("pendingDeliveries", pendingDeliveries);
            model.addAttribute("totalOrders", totalOrders);
        }
        return "customer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Authentication authentication,
                                @RequestParam("name") String name,
                                @RequestParam("phone") String phone,
                                @RequestParam("email") String email,
                                @RequestParam("address") String address,
                                @RequestParam("city") String city,
                                @RequestParam("state") String state,
                                @RequestParam("pincode") String pincode,
                                @RequestParam(value = "profilePhoto", required = false) MultipartFile file,
                                RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Customer> customerOpt = userService.getCustomerByUsername(username);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            try {
                userService.updateCustomerProfile(customer.getId(), name, phone, email, address, city, state, pincode, file);
                eventPublisher.publishEvent(new NotificationEvent(this, username, "Profile Updated", "Your profile details have been successfully updated.", "CUSTOMER", "LOW"));
                redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer profile not found.");
        }
        return "redirect:/customer/profile";
    }

    @PostMapping("/profile/delete-photo")
    public String deletePhoto(Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Customer> customerOpt = userService.getCustomerByUsername(username);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            try {
                userService.deleteCustomerProfilePhoto(customer.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Profile photo deleted successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete profile photo: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer profile not found.");
        }
        return "redirect:/customer/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        Optional<Customer> customerOpt = userService.getCustomerByUsername(username);

        if (!customerOpt.isPresent()) {
            return "redirect:/login";
        }
        Customer customer = customerOpt.get();

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Confirm Password does not match New Password.");
            return "redirect:/customer/profile?tab=password";
        }

        try {
            userService.updateCustomerPassword(customer.getId(), currentPassword, newPassword);
            eventPublisher.publishEvent(new NotificationEvent(this, username, "Password Changed", "Your password has been changed successfully.", "SYSTEM", "HIGH"));

            // Invalidate session and clear context
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            return "redirect:/login?passwordChanged=true";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customer/profile?tab=password";
        }
    }
}
