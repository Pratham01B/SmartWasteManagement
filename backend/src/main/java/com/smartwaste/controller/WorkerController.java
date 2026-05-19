package com.smartwaste.controller;

import com.smartwaste.dto.worker.WorkerResponse;
import com.smartwaste.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Worker management endpoints — admin only.
 */
@RestController
@RequestMapping("/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    /** GET /api/workers — all workers */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkerResponse>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }

    /** GET /api/workers/active — only active workers */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkerResponse>> getActiveWorkers() {
        return ResponseEntity.ok(workerService.getActiveWorkers());
    }

    /** GET /api/workers/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkerResponse> getWorker(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.getWorkerById(id));
    }

    /** PATCH /api/workers/{id}/toggle-status */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkerResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.toggleWorkerStatus(id));
    }
}
