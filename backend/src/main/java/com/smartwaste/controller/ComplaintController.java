package com.smartwaste.controller;

import com.smartwaste.dto.complaint.ComplaintRequest;
import com.smartwaste.dto.complaint.ComplaintResponse;
import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.User;
import com.smartwaste.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for complaint management.
 */
@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * POST /api/complaints
     * Citizen files a new complaint.
     */
    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<ComplaintResponse> createComplaint(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintService.createComplaint(request, currentUser));
    }

    /**
     * GET /api/complaints
     * Admin gets all complaints (paginated).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ComplaintResponse>> getAllComplaints(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(complaintService.getAllComplaints(pageable));
    }

    /**
     * GET /api/complaints/my
     * Citizen views their own complaints.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<Page<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(complaintService.getComplaintsByCitizen(currentUser, pageable));
    }

    /**
     * GET /api/complaints/worker
     * Worker views complaints assigned to them.
     */
    @GetMapping("/worker")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Page<ComplaintResponse>> getWorkerComplaints(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(complaintService.getComplaintsByWorker(currentUser, pageable));
    }

    /**
     * GET /api/complaints/{id}
     * Get a single complaint by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CITIZEN', 'WORKER')")
    public ResponseEntity<ComplaintResponse> getComplaint(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintById(id));
    }

    /**
     * PATCH /api/complaints/{id}/assign/{workerId}
     * Admin assigns a complaint to a worker.
     */
    @PatchMapping("/{id}/assign/{workerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintResponse> assignComplaint(
            @PathVariable Long id,
            @PathVariable Long workerId
    ) {
        return ResponseEntity.ok(complaintService.assignComplaint(id, workerId));
    }

    /**
     * PATCH /api/complaints/{id}/status
     * Worker updates complaint status.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKER')")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(complaintService.updateStatus(id, status, currentUser));
    }
}
