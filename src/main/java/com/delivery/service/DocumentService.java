package com.delivery.service;

import com.delivery.model.Agent;
import com.delivery.model.Document;
import com.delivery.model.Vehicle;
import com.delivery.repository.AgentRepository;
import com.delivery.repository.DocumentRepository;
import com.delivery.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;
import com.delivery.event.NotificationEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.delivery.dto.AgentDocumentStatusDTO;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final AgentRepository agentRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${upload.document.dir:uploads/documents}")
    private String docUploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           AgentRepository agentRepository,
                           VehicleRepository vehicleRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.agentRepository = agentRepository;
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Document uploadDocument(String agentId, Integer vehicleId, String documentType,
                                   LocalDate expiryDate, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or not provided.");
        }

        // Ensure target directory exists
        File directory = new File(docUploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate unique filename
        String fileExtension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String prefix = agentId != null ? agentId : (vehicleId != null ? "vehicle_" + vehicleId : "doc");
        String uniqueFilename = prefix + "_" + UUID.randomUUID().toString() + fileExtension;

        // Save to disk
        Path path = Paths.get(docUploadDir, uniqueFilename);
        Files.write(path, file.getBytes());

        // Create entity
        Agent agent = agentId != null ? agentRepository.findById(agentId).orElse(null) : null;
        Vehicle vehicle = vehicleId != null ? vehicleRepository.findById(vehicleId).orElse(null) : null;

        Document document = new Document();
        document.setAgent(agent);
        document.setVehicle(vehicle);
        document.setDocumentType(documentType);
        document.setFileName(originalFilename);
        document.setFilePath("/uploads/documents/" + uniqueFilename);
        document.setExpiryDate(expiryDate);
        document.setVerificationStatus("PENDING");

        Document saved = documentRepository.save(document);

        String recipient = "admin";
        if (agent != null) {
            recipient = agent.getUser().getUsername();
        } else if (vehicle != null && vehicle.getAgent() != null) {
            recipient = vehicle.getAgent().getUser().getUsername();
        }
        eventPublisher.publishEvent(new NotificationEvent(
            this,
            recipient,
            "Document Uploaded",
            "Document of type " + documentType + " (" + originalFilename + ") has been uploaded and is pending verification.",
            "DOCUMENT",
            "MEDIUM"
        ));

        return saved;
    }

    @Transactional
    public Document verifyDocument(Integer documentId, String status, String remarks) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found."));

        doc.setVerificationStatus(status);
        doc.setRemarks(remarks);
        Document saved = documentRepository.save(doc);

        String recipient = "admin";
        if (doc.getAgent() != null) {
            recipient = doc.getAgent().getUser().getUsername();
        } else if (doc.getVehicle() != null && doc.getVehicle().getAgent() != null) {
            recipient = doc.getVehicle().getAgent().getUser().getUsername();
        }

        String priority = "APPROVED".equalsIgnoreCase(status) ? "MEDIUM" : "HIGH";
        eventPublisher.publishEvent(new NotificationEvent(
            this,
            recipient,
            "Document Verification: " + status,
            "Your document of type " + doc.getDocumentType() + " has been " + status.toLowerCase() + ". Remarks: " + (remarks != null ? remarks : "N/A"),
            "DOCUMENT",
            priority
        ));

        return saved;
    }

    public List<Document> getAgentDocuments(String agentId) {
        return documentRepository.findByAgentId(agentId);
    }

    public List<Document> getVehicleDocuments(Integer vehicleId) {
        return documentRepository.findByVehicleId(vehicleId);
    }

    public List<Document> getPendingDocuments() {
        return documentRepository.findByVerificationStatus("PENDING");
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    public AgentDocumentStatusDTO getAgentDocumentStatus(Agent agent) {
        List<Document> docs = new java.util.ArrayList<>(documentRepository.findByAgentId(agent.getId()));
        if (agent.getVehicle() != null) {
            List<Document> vehicleDocs = documentRepository.findByVehicleId(agent.getVehicle().getId());
            for (Document vd : vehicleDocs) {
                if (docs.stream().noneMatch(d -> d.getId().equals(vd.getId()))) {
                    docs.add(vd);
                }
            }
        }

        int total = docs.size();
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int expired = 0;
        int expiringSoon = 0;

        LocalDate today = LocalDate.now();
        for (Document doc : docs) {
            if ("PENDING".equalsIgnoreCase(doc.getVerificationStatus())) {
                pending++;
            } else if ("APPROVED".equalsIgnoreCase(doc.getVerificationStatus())) {
                approved++;
            } else if ("REJECTED".equalsIgnoreCase(doc.getVerificationStatus())) {
                rejected++;
            }

            if (doc.getExpiryDate() != null) {
                if (doc.getExpiryDate().isBefore(today)) {
                    expired++;
                } else if (doc.getExpiryDate().isBefore(today.plusDays(30))) {
                    expiringSoon++;
                }
            }
        }

        String aggregateStatus;
        if (expired > 0) {
            aggregateStatus = "EXPIRED";
        } else if (pending > 0) {
            aggregateStatus = "PENDING";
        } else if (rejected > 0) {
            aggregateStatus = "REJECTED";
        } else if (total == 0) {
            aggregateStatus = "MISSING";
        } else {
            aggregateStatus = "APPROVED";
        }

        String assignedVehicleStr = "None";
        if (agent.getVehicle() != null) {
            assignedVehicleStr = agent.getVehicle().getVehicleNumber() + " (" + agent.getVehicle().getModel() + ")";
        }

        return new AgentDocumentStatusDTO(
                agent.getId(),
                agent.getName(),
                agent.getPhone(),
                assignedVehicleStr,
                aggregateStatus,
                total,
                pending,
                approved,
                rejected,
                expired,
                expiringSoon
        );
    }
}
