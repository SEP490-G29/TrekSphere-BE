package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.AssignCoordinatorRequest;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.PorterSchedule;
import com.sep.treksphere.entity.PorterProfile;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.entity.VendorEquipment;
import com.sep.treksphere.entity.SessionEquipment;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.SessionEquipmentMapper;
import com.sep.treksphere.mapper.TourSessionMapper;
import com.sep.treksphere.repository.CoordinatorScheduleRepository;
import com.sep.treksphere.repository.PorterScheduleRepository;
import com.sep.treksphere.repository.PorterProfileRepository;
import com.sep.treksphere.repository.TourSessionRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.repository.VendorEquipmentRepository;
import com.sep.treksphere.repository.SessionEquipmentRepository;
import com.sep.treksphere.service.LogisticsAllocationService;
import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.StaffScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.stream.Collectors;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import com.sep.treksphere.dto.request.AssignPorterRequest;
import com.sep.treksphere.dto.request.AssignEquipmentRequest;
import com.sep.treksphere.dto.request.CancelScheduleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LogisticsAllocationServiceImpl implements LogisticsAllocationService {

    private final TourSessionRepository tourSessionRepository;
    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final PorterScheduleRepository porterScheduleRepository;
    private final PorterProfileRepository porterProfileRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final VendorEquipmentRepository vendorEquipmentRepository;
    private final SessionEquipmentRepository sessionEquipmentRepository;
    private final TourSessionMapper tourSessionMapper;
    private final SessionEquipmentMapper sessionEquipmentMapper;

    @Override
    @Transactional
    public void assignEquipment(UUID sessionId, AssignEquipmentRequest request, UUID vendorUserId) {
        log.info("Assigning equipment {} to session {}", request.getEquipmentId(), sessionId);

        TourSession session = tourSessionRepository.findByIdWithVendor(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_SESSION_NOT_FOUND));

        UUID vendorId = resolveVendorId(vendorUserId);

        if (!session.getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (session.getStatus() != TourSessionStatus.PENDING) {
            throw new AppException(ErrorCode.TOUR_SESSION_ALREADY_STARTED);
        }

        VendorEquipment equipment = vendorEquipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new AppException(ErrorCode.EQUIPMENT_NOT_FOUND));

        if (equipment.getIsDeleted() || !equipment.getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.EQUIPMENT_NOT_FOUND);
        }

        if (equipment.getTotalQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.NOT_ENOUGH_EQUIPMENT_IN_STOCK);
        }

        // Deduct quantity from inventory
        equipment.setTotalQuantity(equipment.getTotalQuantity() - request.getQuantity());
        vendorEquipmentRepository.save(equipment);

        // Create session equipment allocation
        SessionEquipment sessionEquipment = sessionEquipmentMapper.toEntity(request);
        sessionEquipment.setTourSession(session);
        sessionEquipment.setEquipment(equipment);

        sessionEquipmentRepository.save(sessionEquipment);
        log.info("Equipment assigned successfully");
    }

    @Override
    @Transactional
    public void assignPorter(UUID sessionId, AssignPorterRequest request, UUID vendorUserId) {
        log.info("Assigning porter {} to session {}", request.getPorterId(), sessionId);

        TourSession session = tourSessionRepository.findByIdWithVendor(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_SESSION_NOT_FOUND));

        UUID vendorId = resolveVendorId(vendorUserId);

        if (!session.getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (session.getStatus() != TourSessionStatus.PENDING) {
            throw new AppException(ErrorCode.TOUR_SESSION_ALREADY_STARTED);
        }

        PorterProfile porter = porterProfileRepository.findById(request.getPorterId())
                .orElseThrow(() -> new AppException(ErrorCode.PORTER_NOT_FOUND));

        if (porter.getIsDeleted() || !porter.getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.PORTER_NOT_FOUND);
        }

        boolean alreadyAssigned = porterScheduleRepository
                .existsByTourSession_TourSessionIdAndPorter_PorterIdAndIsDeletedFalse(sessionId, request.getPorterId());
        if (alreadyAssigned) {
            throw new AppException(ErrorCode.PORTER_ALREADY_ASSIGNED);
        }

        int overlappingCount = porterScheduleRepository.countOverlappingSchedules(
                request.getPorterId(),
                session.getTourSchedule().getDepartureDate(),
                session.getTourSchedule().getReturnDate()
        );
        if (overlappingCount > 0) {
            throw new AppException(ErrorCode.PORTER_SCHEDULE_CONFLICT);
        }

        PorterSchedule newSchedule = new PorterSchedule();
        newSchedule.setTourSession(session);
        newSchedule.setPorter(porter);
        newSchedule.setNote(request.getNote());

        porterScheduleRepository.save(newSchedule);
        log.info("Porter assigned successfully");
    }

    @Override
    @Transactional
    public void removePorter(UUID porterScheduleId, UUID vendorUserId) {
        log.info("Removing porter schedule {}", porterScheduleId);

        PorterSchedule schedule = porterScheduleRepository.findById(porterScheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.PORTER_SCHEDULE_NOT_FOUND));

        if (schedule.getIsDeleted()) {
            throw new AppException(ErrorCode.PORTER_SCHEDULE_NOT_FOUND);
        }

        UUID vendorId = resolveVendorId(vendorUserId);

        if (!schedule.getTourSession().getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_BELONG_TO_VENDOR);
        }

        if (schedule.getTourSession().getStatus() != TourSessionStatus.PENDING) {
            throw new AppException(ErrorCode.TOUR_SESSION_ALREADY_STARTED);
        }

        LocalDateTime departureDateTime = schedule.getTourSession().getTourSchedule().getDepartureDate().atStartOfDay();
        if (LocalDateTime.now().plusDays(1).isAfter(departureDateTime)) {
            throw new AppException(ErrorCode.CANCEL_TOO_CLOSE_TO_DEPARTURE);
        }

        schedule.setIsDeleted(true);
        schedule.setDeletedAt(LocalDateTime.now());
        schedule.setDeletedBy(vendorUserId.toString());
        porterScheduleRepository.save(schedule);
        log.info("Porter schedule removed successfully");
    }

    @Override
    @Transactional
    public void assignCoordinator(UUID sessionId, AssignCoordinatorRequest request, UUID userId) {
        log.info("Assigning coordinator {} to session {}", request.getCoordinatorId(), sessionId);

        TourSession session = tourSessionRepository.findByIdWithVendor(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_SESSION_NOT_FOUND));

        UUID vendorId = resolveVendorId(userId);

        if (!session.getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }

        if (session.getStatus() != TourSessionStatus.PENDING) {
            throw new AppException(ErrorCode.TOUR_SESSION_ALREADY_STARTED);
        }

        VendorStaff coordinatorStaff = vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(request.getCoordinatorId())
                .orElseThrow(() -> new AppException(ErrorCode.COORDINATOR_NOT_FOUND));

        if (!coordinatorStaff.getVendor().getVendorId().equals(vendorId)) { 
            throw new AppException(ErrorCode.COORDINATOR_NOT_FOUND);
        }

        // Check if already assigned
        boolean alreadyAssigned = coordinatorScheduleRepository
                .existsByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(sessionId, request.getCoordinatorId());
        if (alreadyAssigned) {
            throw new AppException(ErrorCode.COORDINATOR_ALREADY_ASSIGNED);
        }

        // Check overlapping schedule
        long overlappingCount = coordinatorScheduleRepository.countOverlappingSchedules(
                request.getCoordinatorId(),
                session.getTourSchedule().getDepartureDate(),
                session.getTourSchedule().getReturnDate()
        );
        if (overlappingCount > 0) {
            throw new AppException(ErrorCode.COORDINATOR_SCHEDULE_CONFLICT);
        }

        // // Check IN_PROGRESS tours
        // long inProgressCount = coordinatorScheduleRepository.countSchedulesByStatus(
        //         request.getCoordinatorId(),
        //         TourSessionStatus.IN_PROGRESS
        // );
        // if (inProgressCount > 0) {
        //     throw new AppException(ErrorCode.COORDINATOR_IN_PROGRESS_TOUR);
        // }

        CoordinatorSchedule newSchedule = new CoordinatorSchedule();
        newSchedule.setTourSession(session);
        newSchedule.setCoordinator(coordinatorStaff.getUser());
        newSchedule.setIsLead(request.getIsLead());

        coordinatorScheduleRepository.save(newSchedule);
        log.info("Coordinator assigned successfully");
    }

    @Override
    @Transactional
    public void removeCoordinator(UUID scheduleId, UUID userId) {
        log.info("Removing coordinator schedule {}", scheduleId);

        CoordinatorSchedule schedule = coordinatorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (schedule.getIsDeleted()) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_FOUND);
        }

        UUID vendorId = resolveVendorId(userId);

        if (!schedule.getTourSession().getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_BELONG_TO_VENDOR);
        }

        if (schedule.getTourSession().getStatus() != TourSessionStatus.PENDING) {
            throw new AppException(ErrorCode.TOUR_SESSION_ALREADY_STARTED);
        }

        schedule.setIsDeleted(true);
        schedule.setDeletedAt(LocalDateTime.now());
        schedule.setDeletedBy(userId.toString());
        coordinatorScheduleRepository.save(schedule);
        log.info("Coordinator schedule removed successfully");
    }

    @Override
    @Transactional
    public void emergencyCancelSchedule(UUID scheduleId, CancelScheduleRequest request, UUID vendorUserId, boolean isManager) {
        log.info("Emergency cancelling schedule {}", scheduleId);

        CoordinatorSchedule schedule = coordinatorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (schedule.getIsDeleted()) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_FOUND);
        }

        UUID vendorId = resolveVendorId(vendorUserId);

        if (!schedule.getTourSession().getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.SCHEDULE_NOT_BELONG_TO_VENDOR);
        }

        if (!isManager && !schedule.getCoordinator().getUserId().equals(vendorUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_STAFF_ACCESS);
        }

        LocalDateTime departureDateTime = schedule.getTourSession().getTourSchedule().getDepartureDate().atStartOfDay();
        if (LocalDateTime.now().plusDays(1).isAfter(departureDateTime)) {
            throw new AppException(ErrorCode.CANCEL_TOO_CLOSE_TO_DEPARTURE);
        }

        if (schedule.getTourSession().getStatus() != TourSessionStatus.PENDING) {
            throw new AppException(ErrorCode.TOUR_SESSION_ALREADY_STARTED);
        }

        schedule.setIsCancelled(true);
        schedule.setCancelReason(request.getReason());
        coordinatorScheduleRepository.save(schedule);
        
        log.info("Schedule cancelled successfully with reason: {}", request.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<TourSessionSummaryResponse> getVendorSessions(UUID vendorUserId, UUID tourId, TourSessionStatus status, int page, int size) {
        UUID vendorId = resolveVendorId(vendorUserId);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("startedAt").descending());
        
        Page<TourSession> sessionPage = tourSessionRepository.findByVendorAndFilters(vendorId, tourId, status, pageable);
        
        return PaginationResponse.<TourSessionSummaryResponse>builder()
                .pageNumber(page)
                .pageSize(size)
                .totalPages(sessionPage.getTotalPages())
                .totalElements(sessionPage.getTotalElements())
                .last(sessionPage.isLast())
                .content(sessionPage.getContent().stream()
                        .map(tourSessionMapper::toSummaryResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TourSessionAllocationResponse getAllocations(UUID sessionId, UUID vendorUserId) {
        UUID vendorId = resolveVendorId(vendorUserId);
        
        TourSession session = tourSessionRepository.findByIdWithVendor(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.TOUR_SESSION_NOT_FOUND));
                
        if (!session.getTourSchedule().getTour().getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);
        }
        
        List<CoordinatorSchedule> coordinatorSchedules = coordinatorScheduleRepository.findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId);
        List<PorterSchedule> porterSchedules = porterScheduleRepository.findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId);
        List<SessionEquipment> sessionEquipments = sessionEquipmentRepository.findByTourSession_TourSessionIdAndIsDeletedFalse(sessionId);

        TourSessionAllocationResponse response = tourSessionMapper.toAllocationResponse(session);
        response.setCoordinators(tourSessionMapper.toCoordinatorAllocationDtoList(coordinatorSchedules));
        response.setPorters(tourSessionMapper.toPorterAllocationDtoList(porterSchedules));
        response.setEquipments(tourSessionMapper.toEquipmentAllocationDtoList(sessionEquipments));
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<StaffScheduleResponse> getCoordinatorSchedules(UUID coordinatorId, UUID vendorUserId, TourSessionStatus status, int page, int size) {
        UUID vendorId = resolveVendorId(vendorUserId);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        org.springframework.data.domain.Page<CoordinatorSchedule> schedulePage = coordinatorScheduleRepository.findSchedulesByVendor(vendorId, coordinatorId, status, pageable);
        
        List<StaffScheduleResponse> content = tourSessionMapper.toStaffScheduleResponseList(schedulePage.getContent());
        
        return PaginationResponse.<StaffScheduleResponse>builder()
                .content(content)
                .pageNumber(schedulePage.getNumber() + 1)
                .pageSize(schedulePage.getSize())
                .totalElements(schedulePage.getTotalElements())
                .totalPages(schedulePage.getTotalPages())
                .last(schedulePage.isLast())
                .build();
    }

    private UUID resolveVendorId(UUID userId) {
        Optional<Vendor> vendor = vendorRepository.findByManager_UserId(userId);
        if (vendor.isPresent()) {
            return vendor.get().getVendorId();
        }

        VendorStaff staff = vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_VENDOR_ACCESS));
        return staff.getVendor().getVendorId();
    }
}
