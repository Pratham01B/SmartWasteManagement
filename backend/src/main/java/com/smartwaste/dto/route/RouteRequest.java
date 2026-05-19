package com.smartwaste.dto.route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RouteRequest {

    @NotNull(message = "Worker ID is required")
    private Long workerId;

    @NotBlank(message = "Route name is required")
    private String routeName;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private String areaName;
    private String pincode;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMin;
}
