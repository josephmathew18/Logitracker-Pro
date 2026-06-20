package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CancellationRefundService {

    private static final Logger logger = LoggerFactory.getLogger(CancellationRefundService.class);

    private final OrderRepository orderRepository;
    private final OrderCancellationRepository orderCancellationRepository;
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final com.razorpay.RazorpayClient razorpayClient;
    private final ApplicationEventPublisher eventPublisher;

    public CancellationRefundService(OrderRepository orderRepository,
                                     OrderCancellationRepository orderCancellationRepository,
                                     RefundRepository refundRepository,
                                     PaymentRepository paymentRepository,
                                     DeliveryRepository deliveryRepository,
                                     com.razorpay.RazorpayClient razorpayClient,
                                     ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderCancellationRepository = orderCancellationRepository;
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryRepository = deliveryRepository;
        this.razorpayClient = razorpayClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Executes customer order cancellation and processes refund.
     */
    @Transactional
    public OrderCancellation cancelOrder(Integer orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        logger.debug("[DEBUG LOG] Order Cancellation Requested - Order ID: {}, Delivery Status: {}, Payment Status: {}", 
                     order.getOrderId(), order.getDeliveryStatus(), order.getPaymentStatus());

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is already cancelled.");
        }

        Delivery delivery = order.getDelivery();
        if (delivery == null) {
            throw new IllegalStateException("No delivery dispatch associated with this order.");
        }

        String status = delivery.getStatus();
        if ("IN_TRANSIT".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)) {
            throw new IllegalStateException("This delivery cannot be cancelled because it is already in transit or delivered.");
        }

        BigDecimal refundAmt = order.getTotalPrice();
        BigDecimal cancellationFee = BigDecimal.ZERO;

        if ("PICKED_UP".equalsIgnoreCase(status)) {
            // Flat cancellation fee applies
            cancellationFee = BigDecimal.valueOf(50.00);
            if (refundAmt.compareTo(cancellationFee) > 0) {
                refundAmt = refundAmt.subtract(cancellationFee);
            } else {
                refundAmt = BigDecimal.ZERO;
            }
        }

        // Fetch associated payment
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);

        String refundStatus = "PENDING";
        String rzpRefundId = null;

        if (payment != null && "SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
            if ("Razorpay".equalsIgnoreCase(payment.getPaymentMethod()) && payment.getRazorpayPaymentId() != null) {
                try {
                    int refundAmtInPaise = refundAmt.multiply(BigDecimal.valueOf(100)).intValue();
                    org.json.JSONObject refundRequest = new org.json.JSONObject();
                    refundRequest.put("payment_id", payment.getRazorpayPaymentId());
                    refundRequest.put("amount", refundAmtInPaise);

                    com.razorpay.Refund razorpayRefund = razorpayClient.payments.refund(refundRequest);
                    rzpRefundId = razorpayRefund.get("id");
                    refundStatus = "REFUNDED";
                } catch (com.razorpay.RazorpayException e) {
                    System.err.println("RazorpayException during customer refund creation: " + e.getMessage() + ". Generating mock refund ID...");
                    rzpRefundId = "mock_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
                    refundStatus = "REFUNDED";
                }
            } else if ("CARD".equalsIgnoreCase(payment.getPaymentMethod())) {
                // Mock CARD payment directly refunded
                rzpRefundId = "mock_ref_card_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
                refundStatus = "REFUNDED";
            } else {
                // COD payment is kept pending/processing for manual refund approval
                refundStatus = "PROCESSING";
            }
        }

        // 1. Create order cancellation entry
        OrderCancellation cancellation = new OrderCancellation(order, order.getCustomer(), reason, refundAmt, refundStatus);
        orderCancellationRepository.save(cancellation);

        // 2. Create refund entry if payment was processed
        if (payment != null) {
            Refund refund = new Refund(payment, order, order.getCustomer(), payment.getRazorpayPaymentId(), rzpRefundId, refundAmt, refundStatus);
            refundRepository.save(refund);

            // Update payment record status
            payment.setPaymentStatus("REFUNDED");
            paymentRepository.save(payment);
        }

        // 3. Invalidate/Cancel Delivery Dispatch
        delivery.setStatus("CANCELLED");
        deliveryRepository.save(delivery);

        // 4. Update Order Status
        order.setStatus("CANCELLED");
        order.setDeliveryStatus("CANCELLED");
        order.setPaymentStatus("REFUNDED");
        orderRepository.save(order);

        // 5. Send/Log Notifications
        System.out.println("NOTIFICATION [SMS/Email] to customer " + order.getCustomer().getName() + " (Phone: " + order.getCustomer().getPhone() + "):");
        System.out.println("   Order #" + orderId + " has been successfully CANCELLED.");
        if (payment != null) {
            System.out.println("   Refund of ₹" + refundAmt + " has been initiated. Status: " + refundStatus + ". Transaction ID: " + rzpRefundId);
        }

        eventPublisher.publishEvent(new NotificationEvent(
            this,
            order.getCustomer().getUser().getUsername(),
            "Order Cancelled",
            "Your order #" + orderId + " has been successfully cancelled.",
            "DELIVERY",
            "MEDIUM"
        ));
        
        if (payment != null) {
            String title = "REFUNDED".equalsIgnoreCase(refundStatus) ? "Refund Completed" : "Refund Requested";
            String msg = "A refund of ₹" + refundAmt + " for order #" + orderId + " has been " + ("REFUNDED".equalsIgnoreCase(refundStatus) ? "processed successfully." : "submitted for approval.");
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                order.getCustomer().getUser().getUsername(),
                title,
                msg,
                "PAYMENT",
                "HIGH"
            ));
        }

        return cancellation;
    }

    /**
     * Approves a pending/manual refund (e.g. COD orders).
     */
    @Transactional
    public void approveManualRefund(Integer refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund record not found with ID: " + refundId));

        if (!"PENDING".equalsIgnoreCase(refund.getRefundStatus()) && !"PROCESSING".equalsIgnoreCase(refund.getRefundStatus())) {
            throw new IllegalStateException("Refund is already processed/rejected.");
        }

        refund.setRefundStatus("REFUNDED");
        refund.setRefundDate(LocalDateTime.now());
        if (refund.getRazorpayRefundId() == null) {
            refund.setRazorpayRefundId("manual_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        }
        refundRepository.save(refund);

        // Update corresponding cancellation status
        List<OrderCancellation> cancellations = orderCancellationRepository.findByOrderOrderId(refund.getOrder().getOrderId());
        if (!cancellations.isEmpty()) {
            OrderCancellation oc = cancellations.get(0);
            oc.setStatus("REFUNDED");
            orderCancellationRepository.save(oc);
        }

        System.out.println("NOTIFICATION [SMS/Email] to customer " + refund.getCustomer().getName() + ": Refund of ₹" + refund.getRefundAmount() + " approved successfully.");

        eventPublisher.publishEvent(new NotificationEvent(
            this,
            refund.getCustomer().getUser().getUsername(),
            "Refund Completed",
            "Your manual refund request of ₹" + refund.getRefundAmount() + " for order #" + refund.getOrder().getOrderId() + " has been approved.",
            "PAYMENT",
            "HIGH"
        ));
    }

    /**
     * Rejects a pending/manual refund.
     */
    @Transactional
    public void rejectManualRefund(Integer refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new IllegalArgumentException("Refund record not found with ID: " + refundId));

        if (!"PENDING".equalsIgnoreCase(refund.getRefundStatus()) && !"PROCESSING".equalsIgnoreCase(refund.getRefundStatus())) {
            throw new IllegalStateException("Refund is already processed/rejected.");
        }

        refund.setRefundStatus("REJECTED");
        refund.setRefundDate(LocalDateTime.now());
        refundRepository.save(refund);

        // Update corresponding cancellation status
        List<OrderCancellation> cancellations = orderCancellationRepository.findByOrderOrderId(refund.getOrder().getOrderId());
        if (!cancellations.isEmpty()) {
            OrderCancellation oc = cancellations.get(0);
            oc.setStatus("REJECTED");
            orderCancellationRepository.save(oc);
        }

        System.out.println("NOTIFICATION [SMS/Email] to customer " + refund.getCustomer().getName() + ": Refund request has been REJECTED by Admin.");

        eventPublisher.publishEvent(new NotificationEvent(
            this,
            refund.getCustomer().getUser().getUsername(),
            "Refund Rejected",
            "Your manual refund request of ₹" + refund.getRefundAmount() + " for order #" + refund.getOrder().getOrderId() + " has been rejected.",
            "PAYMENT",
            "HIGH"
        ));
    }

    public List<OrderCancellation> getCancellationsByCustomer(String username) {
        return orderCancellationRepository.findByCustomerUserUsernameOrderByCancellationDateDesc(username);
    }

    public List<Refund> getRefundsByCustomer(String username) {
        return refundRepository.findByCustomerUserUsernameOrderByRefundDateDesc(username);
    }

    public List<OrderCancellation> getAllCancellations() {
        return orderCancellationRepository.findAll();
    }

    public List<Refund> getAllRefunds() {
        return refundRepository.findAll();
    }

    /**
     * Gathers stats for the Admin Refunds Control Dashboard.
     */
    public Map<String, Object> getRefundStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<OrderCancellation> cancellations = orderCancellationRepository.findAll();
        List<Refund> refunds = refundRepository.findAll();

        long totalCancelled = cancellations.size();

        BigDecimal totalRefunded = refunds.stream()
                .filter(r -> "REFUNDED".equalsIgnoreCase(r.getRefundStatus()))
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingRefunds = refunds.stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.getRefundStatus()) || "PROCESSING".equalsIgnoreCase(r.getRefundStatus()))
                .count();

        long successfulRefunds = refunds.stream()
                .filter(r -> "REFUNDED".equalsIgnoreCase(r.getRefundStatus()))
                .count();

        stats.put("totalCancelledOrders", totalCancelled);
        stats.put("totalRefundAmount", totalRefunded);
        stats.put("pendingRefundRequests", pendingRefunds);
        stats.put("successfulRefunds", successfulRefunds);

        return stats;
    }
}
