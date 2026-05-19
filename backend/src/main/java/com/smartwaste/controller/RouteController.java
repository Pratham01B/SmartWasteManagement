package com.smartwaste.controller;

import com.smartwaste.dto.route.RouteRequest;
import com.smartwaste.dto.route.RouteResponse;
import com.smartwaste.entity.CollectionRoute.RouteStatus;
import com.smartwaste.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Collection route endpoints.
 */
@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    /** GET /api/routes */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllRoutes());
    }

    /** GET /api/routes/worker/{workerId} */
    @GetMapping("/worker/{workerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<List<RouteResponse>> getRoutesByWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(routeService.getRoutesByWorker(workerId));
    }

    /** POST /api/routes */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(request));
    }

    /** PATCH /api/routes/{id}/status */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<RouteResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam RouteStatus status) {
        return ResponseEntity.ok(routeService.updateRouteStatus(id, status));
    }

    /** DELETE /api/routes/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
