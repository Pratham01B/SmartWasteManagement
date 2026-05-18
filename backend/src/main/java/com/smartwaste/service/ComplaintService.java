package com.smartwaste.service;

import com.smartwaste.dto.complaint.ComplaintRequest;
import com.smartwaste.dto.complaint.ComplaintResponse;
import com.smartwaste.entity.Complaint;
import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.ComplaintRepository;
import com.smartwaste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for complaint management.
 */
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    private static final int REWARD_POINTS_ON_RESOLVE = 10;

    /**
     * Citizen files a new complaint.
     */
    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest request, User citizen) {
        Complaint complaint = Complaint.builder()
                .citizen(citizen)
                .title(request.getTitle())
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .pincode(request.getPincode())
                .wasteType(request.getWasteType())
                .priority(request.getPriority() != null ? request.getPriority() : Complaint.Priority.MEDIUM)
                .imageUrl(request.getImageUrl())
                .build();

        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    /**
     * Get all complaints — admin view, paginated.
     */
    public Page<ComplaintResponse> getAllComplaints(Pageable pageable) {
        return complaintRepository.findAll(pageable).map(ComplaintResponse::from);
    }

    /**
     * Get complaints filed by a specific citizen.
     */
    public Page<ComplaintResponse> getComplaintsByCitizen(User citizen, Pageable pageable) {
        return complaintRepository.findByCitizen(citizen, pageable).map(ComplaintResponse::from);
    }

    /**
     * Get complaints assigned to a specific worker.
     */
    public Page<ComplaintResponse> getComplaintsByWorker(User worker, Pageable pageable) {
        return complaintRepository.findByAssignedWorker(worker, pageable).map(ComplaintResponse::from);
    }

    /**
     * Get a single complaint by ID.
     */
    public ComplaintResponse getComplaintById(Long id) {
        return ComplaintResponse.from(findComplaintOrThrow(id));
    }

    /**
     * Admin assigns a complaint to a worker.
     */
    @Transactional
    public ComplaintResponse assignComplaint(Long complaintId, Long workerId) {
        Complaint complaint = findComplaintOrThrow(complaintId);

        User worker = userRepository.findById(workerId)
                .filter(u -> u.getRole() == Role.WORKER)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));

        complaint.setAssignedWorker(worker);
        complaint.setStatus(ComplaintStatus.ASSIGNED);

        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    /**
     * Worker updates complaint status (IN_PROGRESS or RESOLVED).
     */
    @Transactional
    public ComplaintResponse updateStatus(Long complaintId, ComplaintStatus newStatus, User worker) {
        Complaint complaint = findComplaintOrThrow(complaintId);

        complaint.setStatus(newStatus);

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
            // Award reward points to the citizen
            User citizen = complaint.getCitizen();
            citizen.addRewardPoints(REWARD_POINTS_ON_RESOLVE);
            complaint.setRewardPointsAwarded(REWARD_POINTS_ON_RESOLVE);
            userRepository.save(citizen);
        }

        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    // -------------------------
    // Private helpers
    // -------------------------

    private Complaint findComplaintOrThrow(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found: " + id));
    }
}
