package com.smartwaste.dto.complaint;

import com.smartwaste.entity.Complaint;
import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.Complaint.Priority;
import com.smartwaste.entity.Complaint.WasteType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ComplaintResponse {

    private Long id;
    private String title;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String pincode;
    private WasteType wasteType;
    private ComplaintStatus status;
    private Priority priority;
    private String imageUrl;
    private Integer rewardPointsAwarded;

    // Citizen info
    private Long citizenId;
    private String citizenName;

    // Worker info (nullable)
    private Long assignedWorkerId;
    private String assignedWorkerName;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    /**
     * Maps a Complaint entity to a response DTO.
     */
    public static ComplaintResponse from(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .address(complaint.getAddress())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .pincode(complaint.getPincode())
                .wasteType(complaint.getWasteType())
                .status(complaint.getStatus())
                .priority(complaint.getPriority())
                .imageUrl(complaint.getImageUrl())
                .rewardPointsAwarded(complaint.getRewardPointsAwarded())
                .citizenId(complaint.getCitizen().getId())
                .citizenName(complaint.getCitizen().getFullName())
                .assignedWorkerId(complaint.getAssignedWorker() != null
                        ? complaint.getAssignedWorker().getId() : null)
                .assignedWorkerName(complaint.getAssignedWorker() != null
                        ? complaint.getAssignedWorker().getFullName() : null)
                .createdAt(complaint.getCreatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .build();
    }
}
