package com.smartwaste.dto.complaint;

import com.smartwaste.entity.Complaint.Priority;
import com.smartwaste.entity.Complaint.WasteType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    private Double latitude;
    private Double longitude;
    private String pincode;
    private WasteType wasteType;
    private Priority priority;
    private String imageUrl;
}
