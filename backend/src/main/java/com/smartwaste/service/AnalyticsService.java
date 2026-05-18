package com.smartwaste.service;

import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.CollectionRoute.RouteStatus;
import com.smartwaste.entity.Role;
import com.smartwaste.repository.CollectionRouteRepository;
import com.smartwaste.repository.ComplaintRepository;
import com.smartwaste.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for aggregating analytics data for the admin dashboard.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CollectionRouteRepository routeRepository;

    /**
     * Returns a summary of key metrics for the dashboard.
     */
    public DashboardStats getDashboardStats() {
        long totalComplaints = complaintRepository.count();
        long pendingComplaints = complaintRepository.countByStatus(ComplaintStatus.PENDING);
        long resolvedComplaints = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
        long complaintsToday = complaintRepository.countComplaintsSince(
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        );

        long totalCitizens = userRepository.countByRole(Role.CITIZEN);
        long totalWorkers = userRepository.countByRole(Role.WORKER);
        long activeWorkers = userRepository.findByRoleAndIsActive(Role.WORKER, true).size();

        long activeRoutes = routeRepository.countByStatus(RouteStatus.IN_PROGRESS);
        long completedRoutesToday = routeRepository.countByStatus(RouteStatus.COMPLETED);

        // Waste type breakdown
        Map<String, Long> wasteTypeBreakdown = new HashMap<>();
        List<Object[]> wasteTypeData = complaintRepository.countByWasteType();
        for (Object[] row : wasteTypeData) {
            wasteTypeBreakdown.put(row[0].toString(), (Long) row[1]);
        }

        // Complaint status breakdown
        Map<String, Long> statusBreakdown = new HashMap<>();
        List<Object[]> statusData = complaintRepository.countByStatusGrouped();
        for (Object[] row : statusData) {
            statusBreakdown.put(row[0].toString(), (Long) row[1]);
        }

        double resolutionRate = totalComplaints > 0
                ? (double) resolvedComplaints / totalComplaints * 100
                : 0.0;

        return DashboardStats.builder()
                .totalComplaints(totalComplaints)
                .pendingComplaints(pendingComplaints)
                .resolvedComplaints(resolvedComplaints)
                .complaintsToday(complaintsToday)
                .resolutionRate(Math.round(resolutionRate * 10.0) / 10.0)
                .totalCitizens(totalCitizens)
                .totalWorkers(totalWorkers)
                .activeWorkers(activeWorkers)
                .activeRoutes(activeRoutes)
                .completedRoutesToday(completedRoutesToday)
                .wasteTypeBreakdown(wasteTypeBreakdown)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    // -------------------------
    // Response DTO
    // -------------------------

    @Data
    @Builder
    public static class DashboardStats {
        private long totalComplaints;
        private long pendingComplaints;
        private long resolvedComplaints;
        private long complaintsToday;
        private double resolutionRate;

        private long totalCitizens;
        private long totalWorkers;
        private long activeWorkers;

        private long activeRoutes;
        private long completedRoutesToday;

        private Map<String, Long> wasteTypeBreakdown;
        private Map<String, Long> statusBreakdown;
    }
}
