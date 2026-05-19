package com.smartwaste.service;

import com.smartwaste.dto.worker.WorkerResponse;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for worker management (admin operations).
 */
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final UserRepository userRepository;

    /** Get all workers. */
    public List<WorkerResponse> getAllWorkers() {
        return userRepository.findByRole(Role.WORKER)
                .stream()
                .map(WorkerResponse::from)
                .toList();
    }

    /** Get only active workers. */
    public List<WorkerResponse> getActiveWorkers() {
        return userRepository.findByRoleAndIsActive(Role.WORKER, true)
                .stream()
                .map(WorkerResponse::from)
                .toList();
    }

    /** Toggle worker active/inactive status. */
    @Transactional
    public WorkerResponse toggleWorkerStatus(Long workerId) {
        User worker = userRepository.findById(workerId)
                .filter(u -> u.getRole() == Role.WORKER)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));

        worker.setIsActive(!worker.getIsActive());
        return WorkerResponse.from(userRepository.save(worker));
    }

    /** Get a single worker by ID. */
    public WorkerResponse getWorkerById(Long workerId) {
        User worker = userRepository.findById(workerId)
                .filter(u -> u.getRole() == Role.WORKER)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));
        return WorkerResponse.from(worker);
    }
}
