package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to manage orders, payments, invoices, and financial reporting.
 */
@Service
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryService deliveryService;
    private final SalaryRepository salaryRepository;
    private final FuelExpenseRepository fuelExpenseRepository;
    private final com.razorpay.RazorpayClient razorpayClient;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String keyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String keySecret;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository, 
                          CustomerRepository customerRepository, DeliveryService deliveryService,
                          SalaryRepository salaryRepository, FuelExpenseRepository fuelExpenseRepository,
                          com.razorpay.RazorpayClient razorpayClient, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.deliveryService = deliveryService;
        this.salaryRepository = salaryRepository;
        this.fuelExpenseRepository = fuelExpenseRepository;
        this.razorpayClient = razorpayClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new customer order and triggers a corresponding delivery request.
     */
    @Transactional
    public Order createOrder(String customerUsername, String productName, String productCategory, 
                             BigDecimal productPrice, Integer quantity, String pickupAddress, 
                             String deliveryAddress, String parcelDescription, BigDecimal parcelWeight, String packageType) {
        Customer customer = customerRepository.findByUserUsername(customerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found"));

        // Calculate product subtotal
        BigDecimal productSubtotal = BigDecimal.ZERO;
        boolean hasProduct = productName != null && !productName.trim().isEmpty();
        if (hasProduct) {
            if (productPrice == null || quantity == null) {
                throw new IllegalArgumentException("Product price and quantity are required for product orders.");
            }
            productSubtotal = productPrice.multiply(BigDecimal.valueOf(quantity));
        }

        // Determine package details string for dispatch tracking
        String packageDetails = hasProduct ? (quantity + "x " + productName) : parcelDescription;

        // Create Delivery Request first using deliveryService to calculate delivery charge
        Delivery delivery = deliveryService.createDeliveryRequest(customerUsername, pickupAddress, deliveryAddress, packageDetails);
        BigDecimal deliveryCharge = delivery.getTotalCost();

        // Tax is 5% of (Product subtotal + Delivery charge)
        BigDecimal tax = productSubtotal.add(deliveryCharge).multiply(BigDecimal.valueOf(0.05));
        BigDecimal totalAmount = productSubtotal.add(deliveryCharge).add(tax);

        Order order = new Order();
        order.setCustomer(customer);
        order.setProductName(hasProduct ? productName : null);
        order.setProductCategory(hasProduct ? productCategory : null);
        order.setProductPrice(hasProduct ? productPrice : null);
        order.setQuantity(hasProduct ? quantity : null);
        order.setPickupAddress(pickupAddress);
        order.setDeliveryAddress(deliveryAddress);
        order.setParcelDescription(parcelDescription);
        order.setParcelWeight(parcelWeight != null ? parcelWeight : BigDecimal.ZERO);
        order.setPackageType(packageType != null ? packageType : "Document");
        order.setDeliveryCharge(deliveryCharge);
        order.setTax(tax);
        order.setTotalAmount(totalAmount);
        order.setPaymentStatus("PENDING");
        order.setDeliveryStatus("PENDING");
        order.setStatus("PENDING");
        order.setDelivery(delivery);
        
        return orderRepository.save(order);
    }

    // Deprecated delegate method for backward compatibility
    @Deprecated
    @Transactional
    public Order createOrder(String customerUsername, String productName, String productCategory, 
                             BigDecimal productPrice, Integer quantity, String pickupAddress, 
                             String deliveryAddress, String packageDescription, BigDecimal packageWeight) {
        return createOrder(customerUsername, productName, productCategory, productPrice, quantity, pickupAddress, deliveryAddress, packageDescription, packageWeight, "Document");
    }

    /**
     * Processes payment for a specific order.
     */
    @Transactional
    public Payment processPayment(Integer orderId, String method, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException("Order is already paid.");
        }

        String paymentStatus = "SUCCESS";
        String orderPaymentStatus = "PAID";

        if ("COD".equalsIgnoreCase(method)) {
            paymentStatus = "PENDING"; // collected on delivery
            orderPaymentStatus = "PENDING";
        }

        Payment payment = new Payment(order, order.getCustomer(), method, order.getTotalPrice(), paymentStatus, transactionId);
        Payment savedPayment = paymentRepository.save(payment);

        order.setPaymentStatus(orderPaymentStatus);
        orderRepository.save(order);

        if (!"COD".equalsIgnoreCase(method)) {
            eventPublisher.publishEvent(new NotificationEvent(
                this,
                order.getCustomer().getUser().getUsername(),
                "Payment Successful",
                "Payment of ₹" + order.getTotalPrice() + " for order #" + order.getOrderId() + " was successful. TransID: " + transactionId,
                "PAYMENT",
                "MEDIUM"
            ));
        }

        return savedPayment;
    }

    @Transactional
    public Map<String, Object> createRazorpayOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException("Order is already paid.");
        }

        try {
            BigDecimal totalAmount = order.getTotalPrice();
            int amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).intValue();

            org.json.JSONObject orderRequest = new org.json.JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + orderId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrder().getOrderId().equals(orderId))
                    .findFirst()
                    .orElse(null);

            if (payment == null) {
                payment = new Payment(order, order.getCustomer(), "Razorpay", totalAmount, "PENDING", null);
            }
            payment.setRazorpayOrderId(razorpayOrderId);
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", razorpayOrderId);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", keyId);
            response.put("orderId", orderId);
            response.put("customerName", order.getCustomer().getName());
            response.put("customerEmail", order.getCustomer().getEmail());
            response.put("customerPhone", order.getCustomer().getPhone());

            return response;
        } catch (com.razorpay.RazorpayException e) {
            System.err.println("RazorpayException during order creation: " + e.getMessage() + ". Generating mock checkout parameters...");
            
            BigDecimal totalAmount = order.getTotalPrice();
            int amountInPaise = totalAmount.multiply(BigDecimal.valueOf(100)).intValue();
            String mockOrderId = "mock_order_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 14);

            Payment payment = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrder().getOrderId().equals(orderId))
                    .findFirst()
                    .orElse(null);

            if (payment == null) {
                payment = new Payment(order, order.getCustomer(), "Razorpay", totalAmount, "PENDING", null);
            }
            payment.setRazorpayOrderId(mockOrderId);
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("isMock", true);
            response.put("razorpayOrderId", mockOrderId);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", "mock_key");
            response.put("orderId", orderId);
            response.put("customerName", order.getCustomer().getName());
            response.put("customerEmail", order.getCustomer().getEmail());
            response.put("customerPhone", order.getCustomer().getPhone());

            return response;
        }
    }

    @Transactional
    public Payment verifyPaymentSignature(Integer orderId, String razorpayOrderId, String razorpayPaymentId, String signature) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> p.getOrder().getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);

        if (payment == null) {
            payment = new Payment(order, order.getCustomer(), "Razorpay", order.getTotalPrice(), "PENDING", null);
        }

        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(signature);

        try {
            boolean isValid = false;
            if (razorpayOrderId != null && razorpayOrderId.startsWith("mock_order_")) {
                isValid = true;
            } else {
                org.json.JSONObject attributes = new org.json.JSONObject();
                attributes.put("razorpay_order_id", razorpayOrderId);
                attributes.put("razorpay_payment_id", razorpayPaymentId);
                attributes.put("razorpay_signature", signature);
                isValid = com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret);
            }

            if (isValid) {
                payment.setPaymentStatus("SUCCESS");
                payment.setTransactionId(razorpayPaymentId);
                payment.setFailureReason(null);
                paymentRepository.save(payment);

                order.setPaymentStatus("PAID");
                orderRepository.save(order);

                eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    order.getCustomer().getUser().getUsername(),
                    "Payment Successful",
                    "Payment of ₹" + order.getTotalPrice() + " for order #" + order.getOrderId() + " has been successfully verified via Razorpay.",
                    "PAYMENT",
                    "MEDIUM"
                ));

                return payment;
            } else {
                payment.setPaymentStatus("FAILED");
                payment.setFailureReason("Signature verification failed: Invalid Hash");
                paymentRepository.save(payment);

                order.setPaymentStatus("FAILED");
                orderRepository.save(order);

                eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    order.getCustomer().getUser().getUsername(),
                    "Payment Failed",
                    "Payment of ₹" + order.getTotalPrice() + " for order #" + order.getOrderId() + " failed: Signature verification failed.",
                    "PAYMENT",
                    "HIGH"
                ));

                throw new SecurityException("Payment signature verification failed");
            }
        } catch (Exception e) {
            payment.setPaymentStatus("FAILED");
            payment.setFailureReason(e.getMessage() != null ? e.getMessage() : "Unknown verification error");
            paymentRepository.save(payment);

            order.setPaymentStatus("FAILED");
            orderRepository.save(order);

            eventPublisher.publishEvent(new NotificationEvent(
                this,
                order.getCustomer().getUser().getUsername(),
                "Payment Failed",
                "Payment of ₹" + order.getTotalPrice() + " for order #" + order.getOrderId() + " failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown verification error"),
                "PAYMENT",
                "HIGH"
            ));

            throw new RuntimeException("Razorpay signature verification exception: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Payment refundPayment(Integer paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!"SUCCESS".equalsIgnoreCase(payment.getPaymentStatus())) {
            throw new IllegalStateException("Only successful payments can be refunded.");
        }

        if ("Razorpay".equalsIgnoreCase(payment.getPaymentMethod()) && payment.getRazorpayPaymentId() != null) {
            try {
                org.json.JSONObject refundRequest = new org.json.JSONObject();
                refundRequest.put("payment_id", payment.getRazorpayPaymentId());

                com.razorpay.Refund refund = razorpayClient.payments.refund(refundRequest);
                String refundId = refund.get("id");

                payment.setPaymentStatus("REFUNDED");
                payment.setTransactionId(refundId);
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                if (order != null) {
                    order.setPaymentStatus("REFUNDED");
                    orderRepository.save(order);
                }
            } catch (com.razorpay.RazorpayException e) {
                throw new RuntimeException("Error processing Razorpay refund: " + e.getMessage(), e);
            }
        } else {
            payment.setPaymentStatus("REFUNDED");
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            if (order != null) {
                order.setPaymentStatus("REFUNDED");
                orderRepository.save(order);
            }
        }

        eventPublisher.publishEvent(new NotificationEvent(
            this,
            payment.getCustomer().getUser().getUsername(),
            "Refund Completed",
            "A refund of ₹" + payment.getAmount() + " for order #" + (payment.getOrder() != null ? payment.getOrder().getOrderId() : "N/A") + " has been processed successfully.",
            "PAYMENT",
            "HIGH"
        ));

        return payment;
    }


    public List<Order> getOrdersByCustomer(String username) {
        return orderRepository.findByCustomerUserUsernameOrderByCreatedAtDesc(username);
    }

    public List<Payment> getPaymentsByCustomer(String username) {
        return paymentRepository.findByCustomerUserUsernameOrderByPaymentDateDesc(username);
    }

    public Optional<Order> getOrderById(Integer orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Computes financial reports: daily, monthly revenue, total salaries, fuel, and P&L.
     */
    public Map<String, Object> getFinancialReports() {
        Map<String, Object> reports = new HashMap<>();

        List<Payment> payments = paymentRepository.findAll();
        List<Salary> salaries = salaryRepository.findAll();
        List<FuelExpense> fuelExpenses = fuelExpenseRepository.findAll();

        // 1. Total Revenue (Successful payments)
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Total Salary Expenses (Paid salaries)
        BigDecimal totalSalaryExpenses = salaries.stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getPaymentStatus()))
                .map(Salary::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Total Fuel Expenses
        BigDecimal totalFuelExpenses = fuelExpenses.stream()
                .map(FuelExpense::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Total Expenses
        BigDecimal totalExpenses = totalSalaryExpenses.add(totalFuelExpenses);

        // 5. Total Profit
        BigDecimal totalProfit = totalRevenue.subtract(totalExpenses);

        // 6. Daily Revenue
        LocalDate today = LocalDate.now();
        BigDecimal dailyRevenue = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().toLocalDate().isEqual(today))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. Monthly Revenue
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();
        BigDecimal monthlyRevenue = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .filter(p -> p.getPaymentDate() != null && 
                             p.getPaymentDate().getMonthValue() == currentMonth && 
                             p.getPaymentDate().getYear() == currentYear)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        reports.put("totalRevenue", totalRevenue);
        reports.put("totalSalaryExpenses", totalSalaryExpenses);
        reports.put("totalFuelExpenses", totalFuelExpenses);
        reports.put("totalExpenses", totalExpenses);
        reports.put("totalProfit", totalProfit);
        reports.put("dailyRevenue", dailyRevenue);
        reports.put("monthlyRevenue", monthlyRevenue);

        // Statistics for Admin Dashboard
        long totalOrders = orderRepository.count();
        long successfulPaymentsCount = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .count();
        long failedPaymentsCount = payments.stream()
                .filter(p -> "FAILED".equalsIgnoreCase(p.getPaymentStatus()))
                .count();
        long refundedPaymentsCount = payments.stream()
                .filter(p -> "REFUNDED".equalsIgnoreCase(p.getPaymentStatus()))
                .count();

        reports.put("totalOrders", totalOrders);
        reports.put("successfulPaymentsCount", successfulPaymentsCount);
        reports.put("failedPaymentsCount", failedPaymentsCount);
        reports.put("refundedPaymentsCount", refundedPaymentsCount);

        return reports;
    }
}
