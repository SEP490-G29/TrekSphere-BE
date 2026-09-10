package com.sep.treksphere.tour;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRepository;
import com.sep.treksphere.tour.schedule.ScheduleStatus;
import com.sep.treksphere.tour.schedule.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourReadinessService {

    private final TourCheckpointRepository checkpointRepository;
    private final TourScheduleRepository scheduleRepository;

    public List<String> getPublishReadinessErrors(Tour tour) {
        List<String> errors = getStructuralErrors(tour);
        boolean hasFutureSchedule = scheduleRepository
                .existsByTourAndStatusAndDepartureDateAfterAndIsDeletedFalse(
                        tour, ScheduleStatus.OPEN, LocalDate.now());
        if (!hasFutureSchedule) {
            errors.add("NO_FUTURE_OPEN_SCHEDULE");
        }
        return errors;
    }

    public void validateForPublish(Tour tour) {
        List<String> errors = getPublishReadinessErrors(tour);
        if (!errors.isEmpty()) {
            throw new AppException(
                    ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET,
                    "Tour chưa đủ điều kiện công khai: " + String.join(", ", errors));
        }
    }

    public void validatePublishedStructure(Tour tour) {
        List<String> errors = getStructuralErrors(tour);
        if (!errors.isEmpty()) {
            throw new AppException(
                    ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET,
                    "Tour đang công khai phải giữ dữ liệu hợp lệ: " + String.join(", ", errors));
        }
    }

    public void ensureCanRemoveCheckpoint(Tour tour) {
        if (tour.getStatus() == TourStatus.PUBLISHED
                && checkpointRepository.countByTourAndIsDeletedFalse(tour) <= 2) {
            throw new AppException(
                    ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET,
                    "Không thể xóa: Tour công khai phải có ít nhất 2 checkpoint.");
        }
    }

    public void ensureCanRemoveFutureOpenSchedule(
            Tour tour, java.util.UUID scheduleId, boolean removesQualifyingSchedule) {
        if (tour.getStatus() != TourStatus.PUBLISHED || !removesQualifyingSchedule) {
            return;
        }
        long remaining = scheduleRepository.countFutureOpenSchedulesExcluding(
                tour.getTourId(), LocalDate.now(), scheduleId);
        if (remaining == 0) {
            throw new AppException(
                    ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET,
                    "Không thể thay đổi: Tour công khai phải còn ít nhất 1 lịch OPEN trong tương lai.");
        }
    }

    private List<String> getStructuralErrors(Tour tour) {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(tour.getTourName())
                || !StringUtils.hasText(tour.getDescription())
                || tour.getDifficulty() == null
                || !StringUtils.hasText(tour.getLocation())
                || tour.getDurationDays() == null
                || tour.getDurationDays() < 1) {
            errors.add("MISSING_MAIN_FIELDS");
        }
        if (tour.getMinCapacity() == null
                || tour.getMaxCapacity() == null
                || tour.getMinCapacity() < 1
                || tour.getMaxCapacity() < tour.getMinCapacity()) {
            errors.add("INVALID_CAPACITY");
        }
        if (!StringUtils.hasText(tour.getCoverImageUrl())) {
            errors.add("MISSING_COVER_IMAGE");
        }
        if (checkpointRepository.countByTourAndIsDeletedFalse(tour) < 2) {
            errors.add("INSUFFICIENT_CHECKPOINTS");
        }
        return errors;
    }
}
