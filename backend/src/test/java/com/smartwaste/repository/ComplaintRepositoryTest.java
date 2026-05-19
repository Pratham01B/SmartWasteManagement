package com.smartwaste.repository;

import com.smartwaste.entity.Complaint;
import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.Complaint.WasteType;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComplaintRepository — uses Mockito to verify query method contracts
 * without requiring a live database connection.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintRepositoryTest {

    @Mock
    private ComplaintRepository complaintRepository;

    private User citizen;
    private User worker;
    private Complaint pendingComplaint;
    private Complaint resolvedComplaint;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        citizen = User.builder()
                .id(1L)
                .firstName("Rahul")
                .lastName("Sharma")
                .email("rahul@example.com")
                .passwordHash("hashed")
                .role(Role.CITIZEN)
                .rewardPoints(0)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        worker = User.builder()
                .id(2L)
                .firstName("Amit")
                .lastName("Kumar")
                .email("amit@example.com")
                .passwordHash("hashed")
                .role(Role.WORKER)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        pendingComplaint = Complaint.builder()
                .id(1L)
                .citizen(citizen)
                .title("Garbage on street")
                .description("Large pile of garbage near park")
                .address("MG Road, Pune")
                .pincode("411001")
                .wasteType(WasteType.MIXED)
                .status(ComplaintStatus.PENDING)
                .build();

        resolvedComplaint = Complaint.builder()
                .id(2L)
                .citizen(citizen)
                .assignedWorker(worker)
                .title("Plastic waste")
                .description("Plastic bottles dumped near river")
                .address("River Road, Pune")
                .pincode("411001")
                .wasteType(WasteType.PLASTIC)
                .status(ComplaintStatus.RESOLVED)
                .resolvedAt(LocalDateTime.now())
                .rewardPointsAwarded(10)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    // -------------------------
    // findByCitizen
    // -------------------------

    @Test
    @DisplayName("findByCitizen returns complaints filed by the given citizen")
    void findByCitizen_returnsCitizenComplaints() {
        Page<Complaint> expected = new PageImpl<>(List.of(pendingComplaint, resolvedComplaint));
        when(complaintRepository.findByCitizen(citizen, pageable)).thenReturn(expected);

        Page<Complaint> result = complaintRepository.findByCitizen(citizen, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Complaint::getCitizen).containsOnly(citizen);
        verify(complaintRepository).findByCitizen(citizen, pageable);
    }

    @Test
    @DisplayName("findByCitizen returns empty page when citizen has no complaints")
    void findByCitizen_returnsEmptyPage_whenNoneExist() {
        when(complaintRepository.findByCitizen(citizen, pageable)).thenReturn(Page.empty());

        Page<Complaint> result = complaintRepository.findByCitizen(citizen, pageable);

        assertThat(result).isEmpty();
    }

    // -------------------------
    // findByAssignedWorker
    // -------------------------

    @Test
    @DisplayName("findByAssignedWorker returns complaints assigned to the given worker")
    void findByAssignedWorker_returnsWorkerComplaints() {
        Page<Complaint> expected = new PageImpl<>(List.of(resolvedComplaint));
        when(complaintRepository.findByAssignedWorker(worker, pageable)).thenReturn(expected);

        Page<Complaint> result = complaintRepository.findByAssignedWorker(worker, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAssignedWorker()).isEqualTo(worker);
        verify(complaintRepository).findByAssignedWorker(worker, pageable);
    }

    // -------------------------
    // findByStatus
    // -------------------------

    @Test
    @DisplayName("findByStatus returns only complaints with the given status")
    void findByStatus_returnsPendingComplaints() {
        Page<Complaint> expected = new PageImpl<>(List.of(pendingComplaint));
        when(complaintRepository.findByStatus(ComplaintStatus.PENDING, pageable)).thenReturn(expected);

        Page<Complaint> result = complaintRepository.findByStatus(ComplaintStatus.PENDING, pageable);

        assertThat(result.getContent()).allMatch(c -> c.getStatus() == ComplaintStatus.PENDING);
        verify(complaintRepository).findByStatus(ComplaintStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("findByStatus returns empty page when no complaints match the status")
    void findByStatus_returnsEmpty_whenNoMatch() {
        when(complaintRepository.findByStatus(ComplaintStatus.REJECTED, pageable)).thenReturn(Page.empty());

        Page<Complaint> result = complaintRepository.findByStatus(ComplaintStatus.REJECTED, pageable);

        assertThat(result).isEmpty();
    }

    // -------------------------
    // findByPincodeAndStatus
    // -------------------------

    @Test
    @DisplayName("findByPincodeAndStatus returns complaints matching pincode and status")
    void findByPincodeAndStatus_returnsMatchingComplaints() {
        when(complaintRepository.findByPincodeAndStatus("411001", ComplaintStatus.PENDING))
                .thenReturn(List.of(pendingComplaint));

        List<Complaint> result = complaintRepository.findByPincodeAndStatus("411001", ComplaintStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPincode()).isEqualTo("411001");
        assertThat(result.get(0).getStatus()).isEqualTo(ComplaintStatus.PENDING);
    }

    @Test
    @DisplayName("findByPincodeAndStatus returns empty list when no match")
    void findByPincodeAndStatus_returnsEmpty_whenNoMatch() {
        when(complaintRepository.findByPincodeAndStatus("999999", ComplaintStatus.PENDING))
                .thenReturn(List.of());

        List<Complaint> result = complaintRepository.findByPincodeAndStatus("999999", ComplaintStatus.PENDING);

        assertThat(result).isEmpty();
    }

    // -------------------------
    // countByStatus
    // -------------------------

    @Test
    @DisplayName("countByStatus returns correct count for a given status")
    void countByStatus_returnsCorrectCount() {
        when(complaintRepository.countByStatus(ComplaintStatus.PENDING)).thenReturn(5L);

        long count = complaintRepository.countByStatus(ComplaintStatus.PENDING);

        assertThat(count).isEqualTo(5L);
        verify(complaintRepository).countByStatus(ComplaintStatus.PENDING);
    }

    @Test
    @DisplayName("countByStatus returns zero when no complaints have the given status")
    void countByStatus_returnsZero_whenNoneExist() {
        when(complaintRepository.countByStatus(ComplaintStatus.REJECTED)).thenReturn(0L);

        long count = complaintRepository.countByStatus(ComplaintStatus.REJECTED);

        assertThat(count).isZero();
    }

    // -------------------------
    // countComplaintsSince (@Query)
    // -------------------------

    @Test
    @DisplayName("countComplaintsSince returns count of complaints created after the given timestamp")
    void countComplaintsSince_returnsCountAfterTimestamp() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        when(complaintRepository.countComplaintsSince(since)).thenReturn(3L);

        long count = complaintRepository.countComplaintsSince(since);

        assertThat(count).isEqualTo(3L);
        verify(complaintRepository).countComplaintsSince(since);
    }

    @Test
    @DisplayName("countComplaintsSince returns zero when no complaints exist after the timestamp")
    void countComplaintsSince_returnsZero_whenNoneAfterTimestamp() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
        when(complaintRepository.countComplaintsSince(futureDate)).thenReturn(0L);

        long count = complaintRepository.countComplaintsSince(futureDate);

        assertThat(count).isZero();
    }

    // -------------------------
    // countByWasteType (@Query)
    // -------------------------

    @Test
    @DisplayName("countByWasteType returns grouped counts per waste type")
    void countByWasteType_returnsGroupedResults() {
        List<Object[]> mockResult = List.of(
                new Object[]{WasteType.PLASTIC, 4L},
                new Object[]{WasteType.ORGANIC, 2L},
                new Object[]{WasteType.MIXED, 1L}
        );
        when(complaintRepository.countByWasteType()).thenReturn(mockResult);

        List<Object[]> result = complaintRepository.countByWasteType();

        assertThat(result).hasSize(3);
        assertThat(result.get(0)[0]).isEqualTo(WasteType.PLASTIC);
        assertThat(result.get(0)[1]).isEqualTo(4L);
        verify(complaintRepository).countByWasteType();
    }

    @Test
    @DisplayName("countByWasteType returns empty list when no complaints exist")
    void countByWasteType_returnsEmpty_whenNoComplaints() {
        when(complaintRepository.countByWasteType()).thenReturn(List.of());

        List<Object[]> result = complaintRepository.countByWasteType();

        assertThat(result).isEmpty();
    }

    // -------------------------
    // countByStatusGrouped (@Query)
    // -------------------------

    @Test
    @DisplayName("countByStatusGrouped returns grouped counts per status")
    void countByStatusGrouped_returnsGroupedResults() {
        List<Object[]> mockResult = List.of(
                new Object[]{ComplaintStatus.PENDING, 10L},
                new Object[]{ComplaintStatus.RESOLVED, 5L},
                new Object[]{ComplaintStatus.ASSIGNED, 3L}
        );
        when(complaintRepository.countByStatusGrouped()).thenReturn(mockResult);

        List<Object[]> result = complaintRepository.countByStatusGrouped();

        assertThat(result).hasSize(3);
        assertThat(result.get(0)[0]).isEqualTo(ComplaintStatus.PENDING);
        assertThat(result.get(0)[1]).isEqualTo(10L);
        verify(complaintRepository).countByStatusGrouped();
    }

    // -------------------------
    // findByCitizenIdWithUser (@Query with @Param)
    // -------------------------

    @Test
    @DisplayName("findByCitizenIdWithUser returns complaints with citizen and worker eagerly fetched")
    void findByCitizenIdWithUser_returnsComplaintsForCitizenId() {
        Page<Complaint> expected = new PageImpl<>(List.of(pendingComplaint, resolvedComplaint));
        when(complaintRepository.findByCitizenIdWithUser(1L, pageable)).thenReturn(expected);

        Page<Complaint> result = complaintRepository.findByCitizenIdWithUser(1L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allMatch(c -> c.getCitizen().getId().equals(1L));
        verify(complaintRepository).findByCitizenIdWithUser(1L, pageable);
    }

    @Test
    @DisplayName("findByCitizenIdWithUser returns empty page for unknown citizen ID")
    void findByCitizenIdWithUser_returnsEmpty_forUnknownCitizenId() {
        when(complaintRepository.findByCitizenIdWithUser(999L, pageable)).thenReturn(Page.empty());

        Page<Complaint> result = complaintRepository.findByCitizenIdWithUser(999L, pageable);

        assertThat(result).isEmpty();
    }

    // -------------------------
    // JpaRepository inherited — findById / save
    // -------------------------

    @Test
    @DisplayName("findById returns the complaint when it exists")
    void findById_returnsComplaint_whenExists() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(pendingComplaint));

        Optional<Complaint> result = complaintRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById returns empty Optional when complaint does not exist")
    void findById_returnsEmpty_whenNotFound() {
        when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Complaint> result = complaintRepository.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save persists and returns the complaint")
    void save_persistsComplaint() {
        when(complaintRepository.save(pendingComplaint)).thenReturn(pendingComplaint);

        Complaint saved = complaintRepository.save(pendingComplaint);

        assertThat(saved).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Garbage on street");
        verify(complaintRepository).save(pendingComplaint);
    }
}
