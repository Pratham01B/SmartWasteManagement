package com.smartwaste.dto.route;

import com.smartwaste.entity.CollectionRoute;
import com.smartwaste.entity.CollectionRoute.RouteStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RouteResponse {
    private Long id;
    private Long workerId;
    private String workerName;
    private String routeName;
    private LocalDate scheduledDate;
    private String areaName;
    private String pincode;
    private Double estimatedDistanceKm;
    private Integer estimatedDurationMin;
    private RouteStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static RouteResponse from(CollectionRoute route) {
        return RouteResponse.builder()
                .id(route.getId())
                .workerId(route.getWorker().getId())
                .workerName(route.getWorker().getFullName())
                .routeName(route.getRouteName())
                .scheduledDate(route.getScheduledDate())
                .areaName(route.getAreaName())
                .pincode(route.getPincode())
                .estimatedDistanceKm(route.getEstimatedDistanceKm())
                .estimatedDurationMin(route.getEstimatedDurationMin())
                .status(route.getStatus())
                .startedAt(route.getStartedAt())
                .completedAt(route.getCompletedAt())
                .createdAt(route.getCreatedAt())
                .build();
    }
}
