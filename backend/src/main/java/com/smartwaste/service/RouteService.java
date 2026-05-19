package com.smartwaste.service;

import com.smartwaste.dto.route.RouteRequest;
import com.smartwaste.dto.route.RouteResponse;
import com.smartwaste.entity.CollectionRoute;
import com.smartwaste.entity.CollectionRoute.RouteStatus;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.CollectionRouteRepository;
import com.smartwaste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for collection route management.
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    private final CollectionRouteRepository routeRepository;
    private final UserRepository userRepository;

    /** Get all routes. */
    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll()
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    /** Get routes for a specific worker. */
    public List<RouteResponse> getRoutesByWorker(Long workerId) {
        User worker = findWorkerOrThrow(workerId);
        return routeRepository.findByWorker(worker)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    /** Create a new route. */
    @Transactional
    public RouteResponse createRoute(RouteRequest request) {
        User worker = findWorkerOrThrow(request.getWorkerId());

        CollectionRoute route = CollectionRoute.builder()
                .worker(worker)
                .routeName(request.getRouteName())
                .scheduledDate(request.getScheduledDate())
                .areaName(request.getAreaName())
                .pincode(request.getPincode())
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .estimatedDurationMin(request.getEstimatedDurationMin())
                .build();

        return RouteResponse.from(routeRepository.save(route));
    }

    /** Update route status. */
    @Transactional
    public RouteResponse updateRouteStatus(Long routeId, RouteStatus newStatus) {
        CollectionRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routeId));

        route.setStatus(newStatus);

        if (newStatus == RouteStatus.IN_PROGRESS && route.getStartedAt() == null) {
            route.setStartedAt(java.time.LocalDateTime.now());
        }
        if (newStatus == RouteStatus.COMPLETED && route.getCompletedAt() == null) {
            route.setCompletedAt(java.time.LocalDateTime.now());
        }

        return RouteResponse.from(routeRepository.save(route));
    }

    /** Delete a route. */
    @Transactional
    public void deleteRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route not found: " + routeId);
        }
        routeRepository.deleteById(routeId);
    }

    // ── helpers ──────────────────────────────────────────

    private User findWorkerOrThrow(Long workerId) {
        return userRepository.findById(workerId)
                .filter(u -> u.getRole() == Role.WORKER)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));
    }
}
