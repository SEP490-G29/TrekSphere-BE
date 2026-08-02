package com.sep.treksphere.service.impl;

import com.sep.treksphere.dto.request.CoordinatorScheduleFilterRequest;
import com.sep.treksphere.dto.response.CoordinatorScheduleResponse;
import com.sep.treksphere.dto.response.PaginationResponse;
import com.sep.treksphere.entity.CoordinatorSchedule;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.CoordinatorScheduleMapper;
import com.sep.treksphere.repository.CoordinatorScheduleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.service.CoordinatorScheduleService;
import com.sep.treksphere.repository.BookingParticipantRepository;
import com.sep.treksphere.mapper.LogisticsInfoMapper;
import com.sep.treksphere.dto.request.LogisticsInfoFilterRequest;
import com.sep.treksphere.dto.response.LogisticsPassengerResponse;
import com.sep.treksphere.entity.BookingParticipant;
import com.sep.treksphere.enums.booking.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoordinatorScheduleServiceImpl implements CoordinatorScheduleService {

    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final UserRepository userRepository;
    private final CoordinatorScheduleMapper coordinatorScheduleMapper;
    private final BookingParticipantRepository bookingParticipantRepository;
    private final LogisticsInfoMapper logisticsInfoMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<CoordinatorScheduleResponse> getMySchedules(
            String email,
            CoordinatorScheduleFilterRequest request
    ) {
        log.info("Fetching coordinator schedules for user email: {}", email);

        if (request.getDepartureDateFrom() != null && request.getDepartureDateTo() != null) {
            if (request.getDepartureDateFrom().isAfter(request.getDepartureDateTo())) {
                log.warn("Invalid date range for schedule query: from {} to {}", request.getDepartureDateFrom(), request.getDepartureDateTo());
                throw new AppException(ErrorCode.INVALID_DATE_RANGE);
            }
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User with email {} not found", email);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        Page<CoordinatorSchedule> page = coordinatorScheduleRepository.findByCoordinatorIdAndFilters(
                user.getUserId(),
                request.getStatus(),
                request.getIsCancelled(),
                request.getDepartureDateFrom(),
                request.getDepartureDateTo(),
                request.getKeyword(),
                request.getPageable()
        );

        List<CoordinatorScheduleResponse> responses = page.getContent().stream()
                .map(coordinatorScheduleMapper::toResponse)
                .toList();

        log.info("Found {} schedules for coordinator: {}", responses.size(), email);

        return PaginationResponse.<CoordinatorScheduleResponse>builder()
                .content(responses)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<LogisticsPassengerResponse> getLogisticsInfo(
            String email,
            UUID tourSessionId,
            LogisticsInfoFilterRequest request
    ) {
        log.info("Coordinator {} requesting logistics info for tourSessionId {}", email, tourSessionId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate coordinator assignment
        boolean isAssigned = coordinatorScheduleRepository.existsByTourSession_TourSessionIdAndCoordinator_UserIdAndIsDeletedFalse(
                tourSessionId, user.getUserId()
        );
        if (!isAssigned) {
            log.error("Coordinator {} is not assigned to tourSessionId {}", email, tourSessionId);
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        // Only fetch CONFIRMED and COMPLETED participants
        List<BookingStatus> validStatuses = List.of(
                BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED
        );

        Page<BookingParticipant> participantPage = 
                bookingParticipantRepository.findParticipantsByTourSessionIdAndFilters(
                        tourSessionId,
                        validStatuses,
                        request.getKeyword(),
                        request.getIsPresentStart(),
                        request.getIsPresentEnd(),
                        request.getPageable()
                );

        List<LogisticsPassengerResponse> responses = participantPage.getContent().stream()
                .map(logisticsInfoMapper::toLogisticsPassengerResponse)
                .toList();

        return PaginationResponse.<LogisticsPassengerResponse>builder()
                .content(responses)
                .pageNumber(participantPage.getNumber())
                .pageSize(participantPage.getSize())
                .totalElements(participantPage.getTotalElements())
                .totalPages(participantPage.getTotalPages())
                .last(participantPage.isLast())
                .build();
    }
}
