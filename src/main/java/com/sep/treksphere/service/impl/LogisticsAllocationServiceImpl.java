package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.logistics.AssignCoordinatorRequest;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.TourSession;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.entity.VendorStaff;
import com.sep.treksphere.enums.tour.TourSessionStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.TourSessionMapper;
import com.sep.treksphere.repository.CoordinatorScheduleRepository;
import com.sep.treksphere.repository.TourSessionRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.LogisticsAllocationService;
import com.sep.treksphere.dto.response.TourSessionAllocationResponse;
import com.sep.treksphere.dto.response.TourSessionSummaryResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.dto.response.CoordinatorAllocationDto;
import com.sep.treksphere.dto.StaffScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.stream.Collectors;
import java.util.List;
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
public class LogisticsAllocationServiceImpl implements LogisticsAllocationService {

    private final TourSessionRepository tourSessionRepository;
    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final VendorRepository vendorRepository;
    private final VendorStaffRepository vendorStaffRepository;
    private final TourSessionMapper tourSessionMapper;

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

        // Check IN_PROGRESS tours
        long inProgressCount = coordinatorScheduleRepository.countSchedulesByStatus(
                request.getCoordinatorId(),
                TourSessionStatus.IN_PROGRESS
        );
        if (inProgressCount > 0) {
            throw new AppException(ErrorCode.COORDINATOR_IN_PROGRESS_TOUR);
        }

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

        TourSessionAllocationResponse response = tourSessionMapper.toAllocationResponse(session);
        response.setCoordinators(tourSessionMapper.toCoordinatorAllocationDtoList(coordinatorSchedules));
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffScheduleResponse> getCoordinatorSchedules(UUID coordinatorId, UUID vendorUserId) {
        UUID vendorId = resolveVendorId(vendorUserId);

        VendorStaff coordinatorStaff = vendorStaffRepository.findByUser_UserIdAndIsActiveTrueAndIsDeletedFalse(coordinatorId)
                .orElseThrow(() -> new AppException(ErrorCode.COORDINATOR_NOT_FOUND));

        if (!coordinatorStaff.getVendor().getVendorId().equals(vendorId)) {
            throw new AppException(ErrorCode.COORDINATOR_NOT_FOUND);
        }

        List<CoordinatorSchedule> schedules = coordinatorScheduleRepository.findByCoordinator_UserIdAndIsDeletedFalse(coordinatorId);
        
        return tourSessionMapper.toStaffScheduleResponseList(schedules);
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
