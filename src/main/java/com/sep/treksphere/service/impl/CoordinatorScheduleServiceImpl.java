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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoordinatorScheduleServiceImpl implements CoordinatorScheduleService {

    private final CoordinatorScheduleRepository coordinatorScheduleRepository;
    private final UserRepository userRepository;
    private final CoordinatorScheduleMapper coordinatorScheduleMapper;

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
}
