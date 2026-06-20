package com.delivery.service;

import com.delivery.model.Order;
import com.delivery.model.Salary;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    public void generateOrderInvoice(Order order, OutputStream os) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, os);
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Title
            Paragraph title = new Paragraph("TAX INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Invoice Details Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);

            infoTable.addCell(createCell("Company Details:\nDelivery Service Provider Ltd\n123 Logistics St, Tech Park\nsupport@deliveryservice.com", normalFont, false));
            
            String formattedDate = order.getCreatedAt() != null 
                    ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
                    : "N/A";
            
            infoTable.addCell(createCell("Invoice No: INV-" + order.getOrderId() + "\nDate: " + formattedDate + "\nCustomer: " + order.getCustomer().getName() + "\nPayment Status: " + order.getPaymentStatus(), normalFont, false));
            document.add(infoTable);

            // Order Details Table
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{3.0f, 1.0f, 1.0f, 1.0f});
            itemsTable.setSpacingAfter(20);

            // Headers
            itemsTable.addCell(new PdfPCell(new Phrase("Product Name", headerFont)));
            itemsTable.addCell(new PdfPCell(new Phrase("Price (INR)", headerFont)));
            itemsTable.addCell(new PdfPCell(new Phrase("Qty", headerFont)));
            itemsTable.addCell(new PdfPCell(new Phrase("Subtotal (INR)", headerFont)));

            BigDecimal subtotal = order.getProductPrice().multiply(BigDecimal.valueOf(order.getQuantity()));

            // Row
            itemsTable.addCell(new PdfPCell(new Phrase(order.getProductName(), normalFont)));
            itemsTable.addCell(new PdfPCell(new Phrase(order.getProductPrice().toString(), normalFont)));
            itemsTable.addCell(new PdfPCell(new Phrase(String.valueOf(order.getQuantity()), normalFont)));
            itemsTable.addCell(new PdfPCell(new Phrase(subtotal.toString(), normalFont)));
            document.add(itemsTable);

            // Breakdown
            PdfPTable breakdownTable = new PdfPTable(2);
            breakdownTable.setWidthPercentage(40);
            breakdownTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            breakdownTable.addCell(new PdfPCell(new Phrase("Product Subtotal:", boldFont)));
            breakdownTable.addCell(new PdfPCell(new Phrase(subtotal.toString() + " INR", normalFont)));

            breakdownTable.addCell(new PdfPCell(new Phrase("Delivery Charge:", boldFont)));
            breakdownTable.addCell(new PdfPCell(new Phrase(order.getDeliveryCharge().toString() + " INR", normalFont)));

            breakdownTable.addCell(new PdfPCell(new Phrase("GST / Tax (5%):", boldFont)));
            breakdownTable.addCell(new PdfPCell(new Phrase(order.getTax().toString() + " INR", normalFont)));

            breakdownTable.addCell(new PdfPCell(new Phrase("Total Amount:", headerFont)));
            breakdownTable.addCell(new PdfPCell(new Phrase(order.getTotalPrice().toString() + " INR", headerFont)));

            document.add(breakdownTable);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF invoice", e);
        }
    }

    public void generateSalarySlip(Salary salary, OutputStream os) {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, os);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Title
            Paragraph title = new Paragraph("SALARY SLIP", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Metadata Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(20);

            infoTable.addCell(createCell("Agent Details:\nName: " + salary.getAgent().getName() + "\nAgent ID: " + salary.getAgent().getId() + "\nPhone: " + salary.getAgent().getPhone(), normalFont, false));
            infoTable.addCell(createCell("Salary Month: " + salary.getMonth() + " " + salary.getYear() + "\nStatus: " + salary.getSalaryStatus() + "\nDate: " + (salary.getPaymentDate() != null ? salary.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "N/A"), normalFont, false));
            document.add(infoTable);

            // Earnings and Deductions Table
            PdfPTable salaryTable = new PdfPTable(2);
            salaryTable.setWidthPercentage(100);
            salaryTable.setSpacingAfter(20);

            // Row 1: Basic Salary
            salaryTable.addCell(new PdfPCell(new Phrase("Basic Salary", boldFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(salary.getBasicSalary().toString() + " INR", normalFont)));

            // Row 2: Delivery Incentives
            salaryTable.addCell(new PdfPCell(new Phrase("Delivery Incentives", boldFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(salary.getIncentive().toString() + " INR", normalFont)));

            // Row 3: Fuel Reimbursement
            salaryTable.addCell(new PdfPCell(new Phrase("Fuel Reimbursement", boldFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(salary.getFuelReimbursement().toString() + " INR", normalFont)));

            // Row 4: Bonus
            salaryTable.addCell(new PdfPCell(new Phrase("Bonus", boldFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(salary.getBonus().toString() + " INR", normalFont)));

            // Row 5: Deductions
            salaryTable.addCell(new PdfPCell(new Phrase("Deductions", boldFont)));
            salaryTable.addCell(new PdfPCell(new Phrase("-" + salary.getDeductions().toString() + " INR", normalFont)));

            // Row 6: Net Salary
            salaryTable.addCell(new PdfPCell(new Phrase("Net Salary Paid", headerFont)));
            salaryTable.addCell(new PdfPCell(new Phrase(salary.getNetSalary().toString() + " INR", headerFont)));

            document.add(salaryTable);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF salary slip", e);
        }
    }

    private PdfPCell createCell(String content, Font font, boolean border) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        if (!border) {
            cell.setBorder(Rectangle.NO_BORDER);
        }
        return cell;
    }
}
