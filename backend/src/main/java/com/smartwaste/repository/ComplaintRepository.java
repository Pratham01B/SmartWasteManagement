package com.smartwaste.repository;

import com.smartwaste.entity.Complaint;
import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByCitizen(User citizen, Pageable pageable);

    Page<Complaint> findByAssignedWorker(User worker, Pageable pageable);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    List<Complaint> findByPincodeAndStatus(String pincode, ComplaintStatus status);

    long countByStatus(ComplaintStatus status);

    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :since")
    long countComplaintsSince(LocalDateTime since);

    @Query("SELECT c.wasteType, COUNT(c) FROM Complaint c GROUP BY c.wasteType")
    List<Object[]> countByWasteType();

    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT c FROM Complaint c LEFT JOIN FETCH c.citizen LEFT JOIN FETCH c.assignedWorker WHERE c.citizen.id = :citizenId")
    Page<Complaint> findByCitizenIdWithUser(@Param("citizenId") Long citizenId, Pageable pageable);
}
