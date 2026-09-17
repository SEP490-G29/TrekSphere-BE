package com.sep.treksphere.tour.schedule;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourReadinessService;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bao phủ 4 method mà `unit_test_gap_report.md` liệt kê thiếu cho `TourScheduleService`
 * (`getUpcomingSchedules`, `createSchedule`, `updateSchedule`, `deleteSchedule`). Job cron
 * `closePastOpenSchedules` không nằm trong phạm vi (không thao tác qua UI/API).
 */
@ExtendWith(MockitoExtension.class)
class TourScheduleServiceTest {

    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private TourRepository tourRepository;
    @Mock private VendorAccessService vendorAccessService;
    @Mock private TourReadinessService readinessService;
    @Mock private MatchingMemberRepository matchingMemberRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private TourScheduleService scheduleService;

    private static final String VENDOR_EMAIL = "manager@example.com";

    private Vendor vendor;
    private Tour tour;

    @BeforeEach
    void setUp() {
        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());

        tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setTourName("Fansipan Trek");
        tour.setDurationDays(3);
        tour.setStatus(TourStatus.PUBLISHED);
        tour.setVendor(vendor);
    }

    private TourSchedule futureOpenSchedule() {
        TourSchedule schedule = new TourSchedule();
        schedule.setTourScheduleId(UUID.randomUUID());
        schedule.setTour(tour);
        schedule.setDepartureDate(LocalDate.now().plusDays(10));
        schedule.setReturnDate(LocalDate.now().plusDays(12));
        schedule.setPrice(BigDecimal.valueOf(2_000_000));
        schedule.setStatus(ScheduleStatus.OPEN);
        return schedule;
    }

    // ---------------------------------------------------------------
    // getUpcomingSchedules
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getUpcomingSchedules: tour tồn tại -> trả về danh sách lịch OPEN trong tương lai")
    void getUpcomingSchedules_Success() {
        when(tourRepository.findPublishedDetailById(tour.getTourId())).thenReturn(Optional.of(tour));
        when(tourScheduleRepository
                .findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                        eq(tour), eq(ScheduleStatus.OPEN), any(LocalDate.class)))
                .thenReturn(List.of(futureOpenSchedule()));

        List<TourScheduleResponse> result = scheduleService.getUpcomingSchedules(tour.getTourId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ScheduleStatus.OPEN);
    }

    @Test
    @DisplayName("getUpcomingSchedules: tour không tồn tại -> ném AppException TOUR_NOT_FOUND")
    void getUpcomingSchedules_TourNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(tourRepository.findPublishedDetailById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getUpcomingSchedules(randomId))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // createSchedule
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createSchedule: ngày hợp lệ trong thời lượng tour -> tạo lịch OPEN mới")
    void createSchedule_Success() {
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(LocalDate.now().plusDays(10));
        request.setReturnDate(LocalDate.now().plusDays(12));
        request.setPrice(BigDecimal.valueOf(2_000_000));

        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourScheduleRepository.save(any(TourSchedule.class))).thenAnswer(inv -> {
            TourSchedule s = inv.getArgument(0);
            s.setTourScheduleId(UUID.randomUUID());
            return s;
        });

        TourScheduleResponse response = scheduleService.createSchedule(VENDOR_EMAIL, tour.getTourId(), request);

        assertThat(response.getStatus()).isEqualTo(ScheduleStatus.OPEN);
        assertThat(response.getTourId()).isEqualTo(tour.getTourId().toString());
    }

    @Test
    @DisplayName("createSchedule: ngày khởi hành không ở tương lai -> ném AppException SCHEDULE_DEPARTURE_IN_PAST")
    void createSchedule_DepartureInPast_ThrowsException() {
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(LocalDate.now());
        request.setReturnDate(LocalDate.now().plusDays(2));
        request.setPrice(BigDecimal.valueOf(2_000_000));

        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        assertThatThrownBy(() -> scheduleService.createSchedule(VENDOR_EMAIL, tour.getTourId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_DEPARTURE_IN_PAST));
        verify(tourScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSchedule: ngày về trước ngày đi -> ném AppException SCHEDULE_RETURN_BEFORE_DEPARTURE")
    void createSchedule_ReturnBeforeDeparture_ThrowsException() {
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(LocalDate.now().plusDays(10));
        request.setReturnDate(LocalDate.now().plusDays(5));
        request.setPrice(BigDecimal.valueOf(2_000_000));

        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        assertThatThrownBy(() -> scheduleService.createSchedule(VENDOR_EMAIL, tour.getTourId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_RETURN_BEFORE_DEPARTURE));
    }

    @Test
    @DisplayName("createSchedule: khoảng ngày dài hơn thời lượng tour -> ném AppException SCHEDULE_DURATION_EXCEEDS_TOUR")
    void createSchedule_DurationExceedsTour_ThrowsException() {
        // tour.durationDays = 3 -> tối đa returnDate = departure + 2 ngày
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(LocalDate.now().plusDays(10));
        request.setReturnDate(LocalDate.now().plusDays(20));
        request.setPrice(BigDecimal.valueOf(2_000_000));

        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        assertThatThrownBy(() -> scheduleService.createSchedule(VENDOR_EMAIL, tour.getTourId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_DURATION_EXCEEDS_TOUR));
    }

    @Test
    @DisplayName("createSchedule: tour thuộc vendor khác -> ném AppException TOUR_NOT_BELONG_TO_VENDOR")
    void createSchedule_NotOwner_ThrowsException() {
        Vendor otherVendor = new Vendor();
        otherVendor.setVendorId(UUID.randomUUID());
        CreateScheduleRequest request = new CreateScheduleRequest();
        request.setDepartureDate(LocalDate.now().plusDays(10));
        request.setReturnDate(LocalDate.now().plusDays(11));
        request.setPrice(BigDecimal.valueOf(2_000_000));

        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(otherVendor);

        assertThatThrownBy(() -> scheduleService.createSchedule(VENDOR_EMAIL, tour.getTourId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR));
    }

    // ---------------------------------------------------------------
    // updateSchedule
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateSchedule: chỉ đổi giá, giữ nguyên ngày/status OPEN -> cập nhật thành công")
    void updateSchedule_UpdatePriceOnly_Success() {
        TourSchedule schedule = futureOpenSchedule();
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setPrice(BigDecimal.valueOf(2_500_000));

        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourScheduleRepository.save(schedule)).thenReturn(schedule);

        TourScheduleResponse response = scheduleService.updateSchedule(
                VENDOR_EMAIL, schedule.getTourScheduleId(), request);

        assertThat(response.getPrice()).isEqualByComparingTo("2500000");
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.OPEN);
        verify(readinessService).ensureCanRemoveFutureOpenSchedule(tour, schedule.getTourScheduleId(), false);
        verify(notificationService, never()).notify(any(List.class), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateSchedule: huỷ lịch không kèm lý do -> ném AppException SCHEDULE_CHANGE_REASON_REQUIRED")
    void updateSchedule_CancelWithoutReason_ThrowsException() {
        TourSchedule schedule = futureOpenSchedule();
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setStatus(ScheduleStatus.CANCELLED);

        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        assertThatThrownBy(() -> scheduleService.updateSchedule(
                VENDOR_EMAIL, schedule.getTourScheduleId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_CHANGE_REASON_REQUIRED));
        verify(tourScheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSchedule: huỷ lịch kèm lý do -> chuyển CANCELLED và thông báo thành viên nhóm ghép")
    void updateSchedule_CancelWithReason_NotifiesMembers() {
        TourSchedule schedule = futureOpenSchedule();
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setStatus(ScheduleStatus.CANCELLED);
        request.setReason("Thời tiết xấu");

        UUID memberId = UUID.randomUUID();
        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourScheduleRepository.save(schedule)).thenReturn(schedule);
        when(matchingMemberRepository.findAcceptedMemberUserIdsByTourAndTargetDate(any(), any(), any()))
                .thenReturn(List.of(memberId));

        TourScheduleResponse response = scheduleService.updateSchedule(
                VENDOR_EMAIL, schedule.getTourScheduleId(), request);

        assertThat(response.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
        assertThat(schedule.getCancellationReason()).isEqualTo("Thời tiết xấu");
        assertThat(schedule.getCancelledAt()).isNotNull();
        verify(notificationService).notify(eq(List.of(memberId)), any(), any(), eq(tour.getTourId()),
                any(), eq(tour.getTourName()), any());
    }

    @Test
    @DisplayName("updateSchedule: lịch đã CANCELLED từ trước -> ném AppException SCHEDULE_NOT_EDITABLE")
    void updateSchedule_AlreadyCancelled_ThrowsException() {
        TourSchedule schedule = futureOpenSchedule();
        schedule.setStatus(ScheduleStatus.CANCELLED);
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setPrice(BigDecimal.valueOf(1_000_000));

        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        assertThatThrownBy(() -> scheduleService.updateSchedule(
                VENDOR_EMAIL, schedule.getTourScheduleId(), request))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_NOT_EDITABLE));
    }

    // ---------------------------------------------------------------
    // deleteSchedule
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deleteSchedule: hợp lệ -> soft-delete lịch khởi hành")
    void deleteSchedule_Success() {
        TourSchedule schedule = futureOpenSchedule();
        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        scheduleService.deleteSchedule(VENDOR_EMAIL, schedule.getTourScheduleId());

        assertThat(schedule.getIsDeleted()).isTrue();
        assertThat(schedule.getDeletedBy()).isEqualTo(VENDOR_EMAIL);
        verify(readinessService).ensureCanRemoveFutureOpenSchedule(tour, schedule.getTourScheduleId(), true);
    }

    @Test
    @DisplayName("deleteSchedule: readiness chặn (tour publish cần còn lịch OPEN tương lai) -> ném lỗi, không xoá")
    void deleteSchedule_ReadinessBlocks_ThrowsException() {
        TourSchedule schedule = futureOpenSchedule();
        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET))
                .when(readinessService).ensureCanRemoveFutureOpenSchedule(tour, schedule.getTourScheduleId(), true);

        assertThatThrownBy(() -> scheduleService.deleteSchedule(VENDOR_EMAIL, schedule.getTourScheduleId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET));
        assertThat(schedule.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("deleteSchedule: lịch đã CANCELLED -> ném AppException SCHEDULE_NOT_EDITABLE")
    void deleteSchedule_AlreadyCancelled_ThrowsException() {
        TourSchedule schedule = futureOpenSchedule();
        schedule.setStatus(ScheduleStatus.CANCELLED);
        when(tourScheduleRepository.findByIdForUpdate(schedule.getTourScheduleId()))
                .thenReturn(Optional.of(schedule));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        assertThatThrownBy(() -> scheduleService.deleteSchedule(VENDOR_EMAIL, schedule.getTourScheduleId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SCHEDULE_NOT_EDITABLE));
    }
}
