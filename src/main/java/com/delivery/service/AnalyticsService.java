package com.delivery.service;

import com.delivery.model.*;
import com.delivery.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final PaymentRepository paymentRepository;
    private final FuelExpenseRepository fuelExpenseRepository;
    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;
    private final SalaryRepository salaryRepository;
    private final CustomerRepository customerRepository;
    private final DeliveryRepository deliveryRepository;
    private final VehicleRepository vehicleRepository;

    public AnalyticsService(PaymentRepository paymentRepository,
                            FuelExpenseRepository fuelExpenseRepository,
                            VehicleMaintenanceRepository vehicleMaintenanceRepository,
                            SalaryRepository salaryRepository,
                            CustomerRepository customerRepository,
                            DeliveryRepository deliveryRepository,
                            VehicleRepository vehicleRepository) {
        this.paymentRepository = paymentRepository;
        this.fuelExpenseRepository = fuelExpenseRepository;
        this.vehicleMaintenanceRepository = vehicleMaintenanceRepository;
        this.salaryRepository = salaryRepository;
        this.customerRepository = customerRepository;
        this.deliveryRepository = deliveryRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public Map<String, Object> getFinancialSummary() {
        List<Payment> payments = paymentRepository.findAll();
        BigDecimal revenue = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Salary> salaries = salaryRepository.findAll();
        BigDecimal salariesPaid = salaries.stream()
                .filter(s -> "PAID".equalsIgnoreCase(s.getSalaryStatus()))
                .map(Salary::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FuelExpense> fuel = fuelExpenseRepository.findAll();
        BigDecimal fuelExpense = fuel.stream()
                .map(FuelExpense::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VehicleMaintenance> maintenanceList = vehicleMaintenanceRepository.findAll();
        BigDecimal maintenanceCost = maintenanceList.stream()
                .filter(m -> "COMPLETED".equalsIgnoreCase(m.getStatus()))
                .map(VehicleMaintenance::getMaintenanceCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = salariesPaid.add(fuelExpense).add(maintenanceCost);
        BigDecimal netProfit = revenue.subtract(totalExpense);

        Map<String, Object> summary = new HashMap<>();
        summary.put("revenue", revenue);
        summary.put("salariesPaid", salariesPaid);
        summary.put("fuelExpense", fuelExpense);
        summary.put("maintenanceCost", maintenanceCost);
        summary.put("totalExpense", totalExpense);
        summary.put("netProfit", netProfit);
        return summary;
    }

    public Map<String, List<Object>> getMonthlyTrends() {
        // Last 6 months labels
        List<String> months = new ArrayList<>();
        List<Object> monthlyRevenue = new ArrayList<>();
        List<Object> monthlyExpense = new ArrayList<>();
        List<Object> monthlyProfit = new ArrayList<>();

        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate targetDate = now.minusMonths(i);
            String label = targetDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + targetDate.getYear();
            months.add(label);

            int targetMonthVal = targetDate.getMonthValue();
            int targetYear = targetDate.getYear();

            // Revenue in target month
            BigDecimal rev = paymentRepository.findAll().stream()
                    .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) &&
                                 p.getPaymentDate() != null &&
                                 p.getPaymentDate().getMonthValue() == targetMonthVal &&
                                 p.getPaymentDate().getYear() == targetYear)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyRevenue.add(rev);

            // Salary in target month
            BigDecimal sal = salaryRepository.findAll().stream()
                    .filter(s -> "PAID".equalsIgnoreCase(s.getSalaryStatus()) &&
                                 s.getYear() == targetYear &&
                                 getMonthVal(s.getMonth()) == targetMonthVal)
                    .map(Salary::getNetSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Fuel in target month
            BigDecimal fuel = fuelExpenseRepository.findAll().stream()
                    .filter(f -> f.getExpenseDate() != null &&
                                 f.getExpenseDate().getMonthValue() == targetMonthVal &&
                                 f.getExpenseDate().getYear() == targetYear)
                    .map(FuelExpense::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Maintenance in target month
            BigDecimal maint = vehicleMaintenanceRepository.findAll().stream()
                    .filter(m -> "COMPLETED".equalsIgnoreCase(m.getStatus()) &&
                                 m.getCompletedDate() != null &&
                                 m.getCompletedDate().getMonthValue() == targetMonthVal &&
                                 m.getCompletedDate().getYear() == targetYear)
                    .map(VehicleMaintenance::getMaintenanceCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal exp = sal.add(fuel).add(maint);
            monthlyExpense.add(exp);

            monthlyProfit.add(rev.subtract(exp));
        }

        Map<String, List<Object>> trends = new HashMap<>();
        trends.put("labels", months.stream().map(s -> (Object) s).collect(Collectors.toList()));
        trends.put("revenue", monthlyRevenue);
        trends.put("expense", monthlyExpense);
        trends.put("profit", monthlyProfit);
        return trends;
    }

    public Map<String, List<Object>> getDeliveryTrends() {
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        LocalDate now = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            labels.add(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));

            long count = deliveryRepository.findAll().stream()
                    .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().toLocalDate().equals(date))
                    .count();
            values.add(count);
        }

        Map<String, List<Object>> trends = new HashMap<>();
        trends.put("labels", labels.stream().map(s -> (Object) s).collect(Collectors.toList()));
        trends.put("deliveries", values);
        return trends;
    }

    public Map<String, List<Object>> getCustomerGrowth() {
        List<String> labels = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            labels.add(date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));

            long count = customerRepository.findAll().stream()
                    .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().isBefore(date.plusMonths(1).withDayOfMonth(1)))
                    .count();
            values.add(count);
        }

        Map<String, List<Object>> growth = new HashMap<>();
        growth.put("labels", labels.stream().map(s -> (Object) s).collect(Collectors.toList()));
        growth.put("customers", values);
        return growth;
    }

    public Map<String, Object> getVehicleUtilization() {
        long totalVehicles = vehicleRepository.count();
        long availableVehicles = vehicleRepository.findAll().stream()
                .filter(v -> "AVAILABLE".equalsIgnoreCase(v.getStatus()))
                .count();
        long assignedVehicles = vehicleRepository.findAll().stream()
                .filter(v -> "ASSIGNED".equalsIgnoreCase(v.getStatus()))
                .count();
        long maintenanceVehicles = vehicleRepository.findAll().stream()
                .filter(v -> "MAINTENANCE".equalsIgnoreCase(v.getStatus()))
                .count();

        Map<String, Object> util = new HashMap<>();
        util.put("total", totalVehicles);
        util.put("available", availableVehicles);
        util.put("assigned", assignedVehicles);
        util.put("maintenance", maintenanceVehicles);
        return util;
    }

    private int getMonthVal(String name) {
        if (name == null) return 1;
        switch (name.trim().toLowerCase()) {
            case "january": case "jan": return 1;
            case "february": case "feb": return 2;
            case "march": case "mar": return 3;
            case "april": case "apr": return 4;
            case "may": return 5;
            case "june": case "jun": return 6;
            case "july": case "jul": return 7;
            case "august": case "aug": return 8;
            case "september": case "sep": case "sept": return 9;
            case "october": case "oct": return 10;
            case "november": case "nov": return 11;
            case "december": case "dec": return 12;
            default:
                try {
                    return Integer.parseInt(name.trim());
                } catch (Exception e) {
                    return 1;
                }
        }
    }
}
