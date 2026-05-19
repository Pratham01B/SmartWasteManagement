package com.smartwaste.service;

import com.smartwaste.dto.complaint.ComplaintRequest;
import com.smartwaste.dto.complaint.ComplaintResponse;
import com.smartwaste.entity.Complaint;
import com.smartwaste.entity.Complaint.ComplaintStatus;
import com.smartwaste.entity.Complaint.Priority;
import com.smartwaste.entity.Complaint.WasteType;
import com.smartwaste.entity.Role;
import com.smartwaste.entity.User;
import com.smartwaste.exception.ResourceNotFoundException;
import com.smartwaste.repository.ComplaintRepository;
import com.smartwaste.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ComplaintService complaintService;

    // -------------------------
    // Shared test fixtures
    // -------------------------

    private User citizen;
    private User worker;
    private Complaint savedComplaint;

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
                .build();

        worker = User.builder()
                .id(2L)
                .firstName("Amit")
                .lastName("Kumar")
                .email("amit@example.com")
                .passwordHash("hashed")
                .role(Role.WORKER)
                .isActive(true)
                .build();

        savedComplaint = Complaint.builder()
                .id(10L)
                .citizen(citizen)
                .title("Garbage on street")
                .description("Large pile of garbage near park")
                .address("MG Road, Pune")
                .latitude(18.5204)
                .longitude(73.8567)
                .pincode("411001")
                .wasteType(WasteType.MIXED)
                .priority(Priority.HIGH)
                .status(ComplaintStatus.PENDING)
                .rewardPointsAwarded(0)
                .build();
    }

    // -------------------------
    // createComplaint
    // -------------------------

    @Nested
    @DisplayName("createComplaint")
    class CreateComplaint {

        @Test
        @DisplayName("saves complaint and returns response with citizen info")
        void createComplaint_savesAndReturnsResponse() {
            ComplaintRequest request = new ComplaintRequest();
            request.setTitle("Garbage on street");
            request.setDescription("Large pile of garbage near park");
            request.setAddress("MG Road, Pune");
            request.setLatitude(18.5204);
            request.setLongitude(73.8567);
            request.setPincode("411001");
            request.setWasteType(WasteType.MIXED);
            request.setPriority(Priority.HIGH);

            when(complaintRepository.save(any(Complaint.class))).thenReturn(savedComplaint);

            ComplaintResponse response = complaintService.createComplaint(request, citizen);

            assertThat(response).isNotNull();
            assertThat(response.getTitle()).isEqualTo("Garbage on street");
            assertThat(response.getCitizenId()).isEqualTo(citizen.getId());
            assertThat(response.getStatus()).isEqualTo(ComplaintStatus.PENDING);
            verify(complaintRepository).save(any(Complaint.class));
        }

        @Test
        @DisplayName("defaults priority to MEDIUM when request priority is null")
        void createComplaint_defaultsPriorityToMedium_whenPriorityIsNull() {
            ComplaintRequest request = new ComplaintRequest();
            request.setTitle("Overflowing bin");
            request.setAddress("Shivaji Nagar");
            request.setPriority(null); // explicitly null

            Complaint mediumPriorityComplaint = Complaint.builder()
                    .id(11L)
                    .citizen(citizen)
                    .title("Overflowing bin")
                    .address("Shivaji Nagar")
                    .priority(Priority.MEDIUM)
                    .status(ComplaintStatus.PENDING)
                    .rewardPointsAwarded(0)
                    .build();

            when(complaintRepository.save(any(Complaint.class))).thenReturn(mediumPriorityComplaint);

            ComplaintResponse response = complaintService.createComplaint(request, citizen);

            // Capture the complaint passed to save and verify priority
            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            assertThat(captor.getValue().getPriority()).isEqualTo(Priority.MEDIUM);
        }

        @Test
        @DisplayName("uses provided priority when not null")
        void createComplaint_usesProvidedPriority() {
            ComplaintRequest request = new ComplaintRequest();
            request.setTitle("Hazardous waste dump");
            request.setAddress("Industrial Area");
            request.setPriority(Priority.URGENT);

            Complaint urgentComplaint = Complaint.builder()
                    .id(12L)
                    .citizen(citizen)
                    .title("Hazardous waste dump")
                    .address("Industrial Area")
                    .priority(Priority.URGENT)
                    .status(ComplaintStatus.PENDING)
                    .rewardPointsAwarded(0)
                    .build();

            when(complaintRepository.save(any(Complaint.class))).thenReturn(urgentComplaint);

            complaintService.createComplaint(request, citizen);

            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            assertThat(captor.getValue().getPriority()).isEqualTo(Priority.URGENT);
        }
    }

    // -------------------------
    // getAllComplaints
    // -------------------------

    @Nested
    @DisplayName("getAllComplaints")
    class GetAllComplaints {

        @Test
        @DisplayName("returns paginated complaints mapped to responses")
        void getAllComplaints_returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Complaint> complaintPage = new PageImpl<>(List.of(savedComplaint));
            when(complaintRepository.findAll(pageable)).thenReturn(complaintPage);

            Page<ComplaintResponse> result = complaintService.getAllComplaints(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
            verify(complaintRepository).findAll(pageable);
        }

        @Test
        @DisplayName("returns empty page when no complaints exist")
        void getAllComplaints_returnsEmptyPage_whenNoneExist() {
            Pageable pageable = PageRequest.of(0, 10);
            when(complaintRepository.findAll(pageable)).thenReturn(Page.empty());

            Page<ComplaintResponse> result = complaintService.getAllComplaints(pageable);

            assertThat(result.isEmpty()).isTrue();
        }
    }

    // -------------------------
    // getComplaintsByCitizen
    // -------------------------

    @Nested
    @DisplayName("getComplaintsByCitizen")
    class GetComplaintsByCitizen {

        @Test
        @DisplayName("returns complaints for the given citizen")
        void getComplaintsByCitizen_returnsCorrectPage() {
            Pageable pageable = PageRequest.of(0, 5);
            Page<Complaint> page = new PageImpl<>(List.of(savedComplaint));
            when(complaintRepository.findByCitizenIdWithUser(citizen.getId(), pageable)).thenReturn(page);

            Page<ComplaintResponse> result = complaintService.getComplaintsByCitizen(citizen, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCitizenId()).isEqualTo(citizen.getId());
            verify(complaintRepository).findByCitizenIdWithUser(citizen.getId(), pageable);
        }

        @Test
        @DisplayName("returns empty page when citizen has no complaints")
        void getComplaintsByCitizen_returnsEmptyPage_whenNoneExist() {
            Pageable pageable = PageRequest.of(0, 5);
            when(complaintRepository.findByCitizenIdWithUser(citizen.getId(), pageable)).thenReturn(Page.empty());

            Page<ComplaintResponse> result = complaintService.getComplaintsByCitizen(citizen, pageable);

            assertThat(result.isEmpty()).isTrue();
            verify(complaintRepository).findByCitizenIdWithUser(citizen.getId(), pageable);
        }
    }

    // -------------------------
    // getComplaintsByWorker
    // -------------------------

    @Nested
    @DisplayName("getComplaintsByWorker")
    class GetComplaintsByWorker {

        @Test
        @DisplayName("returns complaints assigned to the given worker")
        void getComplaintsByWorker_returnsCorrectPage() {
            Complaint assignedComplaint = Complaint.builder()
                    .id(10L)
                    .citizen(citizen)
                    .assignedWorker(worker)
                    .title("Garbage on street")
                    .address("MG Road, Pune")
                    .status(ComplaintStatus.ASSIGNED)
                    .priority(Priority.HIGH)
                    .rewardPointsAwarded(0)
                    .build();

            Pageable pageable = PageRequest.of(0, 5);
            Page<Complaint> page = new PageImpl<>(List.of(assignedComplaint));
            when(complaintRepository.findByAssignedWorker(worker, pageable)).thenReturn(page);

            Page<ComplaintResponse> result = complaintService.getComplaintsByWorker(worker, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAssignedWorkerId()).isEqualTo(worker.getId());
            verify(complaintRepository).findByAssignedWorker(worker, pageable);
        }
    }

    // -------------------------
    // getComplaintById
    // -------------------------

    @Nested
    @DisplayName("getComplaintById")
    class GetComplaintById {

        @Test
        @DisplayName("returns response when complaint exists")
        void getComplaintById_returnsResponse_whenFound() {
            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));

            ComplaintResponse response = complaintService.getComplaintById(10L);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getTitle()).isEqualTo("Garbage on street");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when complaint does not exist")
        void getComplaintById_throwsException_whenNotFound() {
            when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> complaintService.getComplaintById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -------------------------
    // assignComplaint
    // -------------------------

    @Nested
    @DisplayName("assignComplaint")
    class AssignComplaint {

        @Test
        @DisplayName("assigns worker and sets status to ASSIGNED")
        void assignComplaint_setsWorkerAndStatus() {
            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));

            Complaint assignedComplaint = Complaint.builder()
                    .id(10L)
                    .citizen(citizen)
                    .assignedWorker(worker)
                    .title("Garbage on street")
                    .address("MG Road, Pune")
                    .status(ComplaintStatus.ASSIGNED)
                    .priority(Priority.HIGH)
                    .rewardPointsAwarded(0)
                    .build();
            when(complaintRepository.save(any(Complaint.class))).thenReturn(assignedComplaint);

            ComplaintResponse response = complaintService.assignComplaint(10L, 2L);

            assertThat(response.getStatus()).isEqualTo(ComplaintStatus.ASSIGNED);
            assertThat(response.getAssignedWorkerId()).isEqualTo(worker.getId());

            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedWorker()).isEqualTo(worker);
            assertThat(captor.getValue().getStatus()).isEqualTo(ComplaintStatus.ASSIGNED);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when complaint does not exist")
        void assignComplaint_throwsException_whenComplaintNotFound() {
            when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> complaintService.assignComplaint(99L, 2L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user is not a WORKER")
        void assignComplaint_throwsException_whenUserIsNotWorker() {
            User nonWorker = User.builder()
                    .id(3L)
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@example.com")
                    .passwordHash("hashed")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();

            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));
            when(userRepository.findById(3L)).thenReturn(Optional.of(nonWorker));

            assertThatThrownBy(() -> complaintService.assignComplaint(10L, 3L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("3");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when worker ID does not exist")
        void assignComplaint_throwsException_whenWorkerIdNotFound() {
            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> complaintService.assignComplaint(10L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -------------------------
    // updateStatus
    // -------------------------

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("updates status to IN_PROGRESS without awarding points")
        void updateStatus_toInProgress_doesNotAwardPoints() {
            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));

            Complaint inProgressComplaint = Complaint.builder()
                    .id(10L)
                    .citizen(citizen)
                    .title("Garbage on street")
                    .address("MG Road, Pune")
                    .status(ComplaintStatus.IN_PROGRESS)
                    .priority(Priority.HIGH)
                    .rewardPointsAwarded(0)
                    .build();
            when(complaintRepository.save(any(Complaint.class))).thenReturn(inProgressComplaint);

            ComplaintResponse response = complaintService.updateStatus(10L, ComplaintStatus.IN_PROGRESS, worker);

            assertThat(response.getStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
            // No reward points should be saved to user
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("awards 10 reward points to citizen when status is RESOLVED")
        void updateStatus_toResolved_awardsRewardPointsToCitizen() {
            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));

            Complaint resolvedComplaint = Complaint.builder()
                    .id(10L)
                    .citizen(citizen)
                    .title("Garbage on street")
                    .address("MG Road, Pune")
                    .status(ComplaintStatus.RESOLVED)
                    .priority(Priority.HIGH)
                    .rewardPointsAwarded(10)
                    .build();
            when(complaintRepository.save(any(Complaint.class))).thenReturn(resolvedComplaint);

            complaintService.updateStatus(10L, ComplaintStatus.RESOLVED, worker);

            // Citizen's reward points should be updated
            verify(userRepository).save(citizen);
            assertThat(citizen.getRewardPoints()).isEqualTo(10);

            // resolvedAt should be set on the complaint
            ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
            verify(complaintRepository).save(captor.capture());
            assertThat(captor.getValue().getResolvedAt()).isNotNull();
            assertThat(captor.getValue().getRewardPointsAwarded()).isEqualTo(10);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when complaint does not exist")
        void updateStatus_throwsException_whenComplaintNotFound() {
            when(complaintRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> complaintService.updateStatus(99L, ComplaintStatus.IN_PROGRESS, worker))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("does not award points when status is REJECTED")
        void updateStatus_toRejected_doesNotAwardPoints() {
            when(complaintRepository.findById(10L)).thenReturn(Optional.of(savedComplaint));

            Complaint rejectedComplaint = Complaint.builder()
                    .id(10L)
                    .citizen(citizen)
                    .title("Garbage on street")
                    .address("MG Road, Pune")
                    .status(ComplaintStatus.REJECTED)
                    .priority(Priority.HIGH)
                    .rewardPointsAwarded(0)
                    .build();
            when(complaintRepository.save(any(Complaint.class))).thenReturn(rejectedComplaint);

            complaintService.updateStatus(10L, ComplaintStatus.REJECTED, worker);

            verifyNoInteractions(userRepository);
            assertThat(citizen.getRewardPoints()).isZero();
        }
    }
}
