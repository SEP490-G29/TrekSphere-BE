package com.sep.treksphere.tour;

import com.sep.treksphere.common.dto.BaseFilterRequest;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRepository;
import com.sep.treksphere.tour.dto.request.CreateTourRequest;
import com.sep.treksphere.tour.dto.request.UpdateTourRequest;
import com.sep.treksphere.tour.dto.response.TourDetailResponse;
import com.sep.treksphere.tour.dto.response.TourSummaryResponse;
import com.sep.treksphere.tour.image.TourImageRepository;
import com.sep.treksphere.tour.schedule.TourScheduleRepository;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import com.sep.treksphere.vendor.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bao phủ 10 method mà `unit_test_gap_report.md` liệt kê thiếu cho module Tour Management:
 * getTours, getTourById, getVendorTours, getVendorTourById, createTour, updateTour, publishTour,
 * unpublishTour, deleteTour, hideTourForViolation. `restoreTour`/`unhideTour` bị loại vì đánh dấu
 * 🚫 Không cần test (mồ côi, chưa ghép FE).
 */
@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock private TourRepository tourRepository;
    @Mock private TourImageRepository tourImageRepository;
    @Mock private TourCheckpointRepository tourCheckpointRepository;
    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private NotificationService notificationService;
    @Mock private MatchingGroupRepository matchingGroupRepository;
    @Mock private UserRepository userRepository;
    @Mock private TourMapper tourMapper;
    @Mock private FileService fileService;
    @Mock private VendorAccessService vendorAccessService;
    @Mock private TourReadinessService readinessService;

    @InjectMocks
    private TourService tourService;

    private static final String VENDOR_EMAIL = "manager@example.com";

    private Vendor vendor;
    private User manager;
    private User creator;
    private Tour tour;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setUserId(UUID.randomUUID());
        manager.setFullName("Vendor Manager");
        manager.setEmail(VENDOR_EMAIL);

        creator = new User();
        creator.setUserId(UUID.randomUUID());
        creator.setFullName("Creator Name");
        creator.setEmail("creator@example.com");

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setManager(manager);
        vendor.setCompanyName("Trek Co");
        vendor.setStatus(VendorStatus.ACTIVE);

        tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setTourName("Fansipan Trek");
        tour.setDescription("Mo ta chuyen di");
        tour.setDifficulty(DifficultyLevel.MODERATE);
        tour.setLocation("Sa Pa");
        tour.setDurationDays(2);
        tour.setMinCapacity(4);
        tour.setMaxCapacity(12);
        tour.setCoverImageUrl("https://example.com/cover.jpg");
        tour.setStatus(TourStatus.DRAFT);
        tour.setVendor(vendor);
        tour.setCreator(creator);
    }

    // ---------------------------------------------------------------
    // getTours
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getTours: trả về tour PUBLISHED kèm giá thấp nhất (fromPrice)")
    void getTours_ReturnsPublishedToursMappedWithFromPrice() {
        tour.setStatus(TourStatus.PUBLISHED);
        Page<Tour> page = new PageImpl<>(List.of(tour));
        when(tourRepository.searchTours(eq(TourStatus.PUBLISHED), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(page);
        when(tourScheduleRepository.findMinOpenPriceByTourIds(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{tour.getTourId(), BigDecimal.valueOf(1_500_000)}));

        PaginationResponse<TourSummaryResponse> result = tourService.getTours(
                null, null, null, null, null, null, 0, 10, null, null);

        assertThat(result.getContent()).hasSize(1);
        TourSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.getTourId()).isEqualTo(tour.getTourId().toString());
        assertThat(summary.getFromPrice()).isEqualByComparingTo("1500000");
        assertThat(summary.getStatus()).isEqualTo(TourStatus.PUBLISHED);
    }

    @Test
    @DisplayName("getTours: sortBy không hợp lệ -> tự động dùng publishedAt")
    void getTours_InvalidSortField_FallsBackToPublishedAt() {
        when(tourRepository.searchTours(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        tourService.getTours(null, null, null, null, null, null, 0, 10, "not-a-real-field", "asc");

        verify(tourRepository).searchTours(any(), any(), any(), any(), any(), any(), any(),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("publishedAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("not-a-real-field")).isNull();
    }

    // ---------------------------------------------------------------
    // getTourById
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getTourById: tour PUBLISHED tồn tại -> trả về chi tiết công khai")
    void getTourById_Success() {
        when(tourRepository.findPublishedDetailById(tour.getTourId())).thenReturn(Optional.of(tour));

        var response = tourService.getTourById(tour.getTourId());

        assertThat(response.getTourId()).isEqualTo(tour.getTourId().toString());
        assertThat(response.getTourName()).isEqualTo("Fansipan Trek");
    }

    @Test
    @DisplayName("getTourById: không tìm thấy -> ném AppException TOUR_NOT_FOUND")
    void getTourById_NotFound_ThrowsTourNotFound() {
        UUID randomId = UUID.randomUUID();
        when(tourRepository.findPublishedDetailById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.getTourById(randomId))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // getVendorTours
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getVendorTours: trả về toàn bộ tour (mọi trạng thái) của đúng vendor đang đăng nhập")
    void getVendorTours_Success() {
        when(vendorAccessService.resolveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByVendorIdForOwner(eq(vendor.getVendorId()), any(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tour)));

        PaginationResponse<TourSummaryResponse> result =
                tourService.getVendorTours(VENDOR_EMAIL, new BaseFilterRequest());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTourId()).isEqualTo(tour.getTourId().toString());
    }

    // ---------------------------------------------------------------
    // getVendorTourById
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getVendorTourById: đúng chủ sở hữu -> trả về chi tiết đầy đủ cho vendor")
    void getVendorTourById_Success() {
        when(vendorAccessService.resolveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        TourDetailResponse response = tourService.getVendorTourById(VENDOR_EMAIL, tour.getTourId());

        assertThat(response.getTourId()).isEqualTo(tour.getTourId().toString());
        assertThat(response.getVendorId()).isEqualTo(vendor.getVendorId().toString());
    }

    @Test
    @DisplayName("getVendorTourById: tour thuộc vendor khác -> ném AppException TOUR_NOT_BELONG_TO_VENDOR")
    void getVendorTourById_NotOwner_ThrowsTourNotBelongToVendor() {
        Vendor otherVendor = new Vendor();
        otherVendor.setVendorId(UUID.randomUUID());
        tour.setVendor(otherVendor);

        when(vendorAccessService.resolveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> tourService.getVendorTourById(VENDOR_EMAIL, tour.getTourId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR));
    }

    // ---------------------------------------------------------------
    // createTour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createTour: hợp lệ -> tạo tour DRAFT gắn đúng vendor + người tạo")
    void createTour_Success() {
        Tour mapped = new Tour();
        mapped.setTourId(UUID.randomUUID());
        mapped.setMinCapacity(4);
        mapped.setMaxCapacity(10);

        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(userRepository.findByEmail(VENDOR_EMAIL)).thenReturn(Optional.of(creator));
        when(tourMapper.toTour(any(CreateTourRequest.class))).thenReturn(mapped);
        when(tourRepository.save(any(Tour.class))).thenAnswer(inv -> inv.getArgument(0));

        TourDetailResponse response = tourService.createTour(
                VENDOR_EMAIL, new CreateTourRequest(), null, null);

        assertThat(response.getTourId()).isEqualTo(mapped.getTourId().toString());
        assertThat(mapped.getStatus()).isEqualTo(TourStatus.DRAFT);
        assertThat(mapped.getVendor()).isEqualTo(vendor);
        assertThat(mapped.getCreator()).isEqualTo(creator);
    }

    @Test
    @DisplayName("createTour: sức chứa không hợp lệ -> ném AppException INVALID_TOUR_CAPACITY, không lưu")
    void createTour_InvalidCapacity_ThrowsException() {
        Tour mapped = new Tour();
        mapped.setMinCapacity(10);
        mapped.setMaxCapacity(5);

        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(userRepository.findByEmail(VENDOR_EMAIL)).thenReturn(Optional.of(creator));
        when(tourMapper.toTour(any(CreateTourRequest.class))).thenReturn(mapped);

        assertThatThrownBy(() -> tourService.createTour(VENDOR_EMAIL, new CreateTourRequest(), null, null))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOUR_CAPACITY));
        verify(tourRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // updateTour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateTour: tour DRAFT hợp lệ -> cập nhật và lưu thành công")
    void updateTour_Success() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(tourRepository.save(tour)).thenReturn(tour);

        TourDetailResponse response = tourService.updateTour(
                VENDOR_EMAIL, tour.getTourId(), new UpdateTourRequest(), null, null);

        assertThat(response.getTourId()).isEqualTo(tour.getTourId().toString());
        verify(tourMapper).updateTourFromRequest(any(UpdateTourRequest.class), eq(tour));
    }

    @Test
    @DisplayName("updateTour: tour đang PUBLISHED nhưng cấu trúc không còn hợp lệ -> ném lỗi, không lưu")
    void updateTour_PublishedInvalidStructure_ThrowsException() {
        tour.setStatus(TourStatus.PUBLISHED);
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        doThrow(new AppException(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET))
                .when(readinessService).validatePublishedStructure(tour);

        assertThatThrownBy(() -> tourService.updateTour(
                VENDOR_EMAIL, tour.getTourId(), new UpdateTourRequest(), null, null))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET));
        verify(tourRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTour: request khiến sức chứa không hợp lệ -> ném AppException INVALID_TOUR_CAPACITY")
    void updateTour_ResultsInInvalidCapacity_ThrowsException() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        doAnswer(inv -> {
            tour.setMinCapacity(10);
            tour.setMaxCapacity(5);
            return null;
        }).when(tourMapper).updateTourFromRequest(any(UpdateTourRequest.class), eq(tour));

        assertThatThrownBy(() -> tourService.updateTour(
                VENDOR_EMAIL, tour.getTourId(), new UpdateTourRequest(), null, null))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOUR_CAPACITY));
        verify(tourRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // publishTour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("publishTour: tour DRAFT đủ điều kiện -> chuyển PUBLISHED, gán publishedAt")
    void publishTour_Success() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(tourRepository.save(tour)).thenReturn(tour);

        tourService.publishTour(VENDOR_EMAIL, tour.getTourId());

        assertThat(tour.getStatus()).isEqualTo(TourStatus.PUBLISHED);
        assertThat(tour.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("publishTour: tour không ở trạng thái DRAFT -> ném AppException TOUR_NOT_DRAFT")
    void publishTour_NotDraft_ThrowsTourNotDraft() {
        tour.setStatus(TourStatus.PUBLISHED);
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> tourService.publishTour(VENDOR_EMAIL, tour.getTourId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_DRAFT));
        verify(readinessService, never()).validateForPublish(any());
    }

    @Test
    @DisplayName("publishTour: chưa đủ điều kiện readiness -> lan truyền lỗi, không lưu")
    void publishTour_NotReady_ThrowsException() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        doThrow(new AppException(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET))
                .when(readinessService).validateForPublish(tour);

        assertThatThrownBy(() -> tourService.publishTour(VENDOR_EMAIL, tour.getTourId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET));
        verify(tourRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // unpublishTour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("unpublishTour: tour PUBLISHED không có nhóm ghép hoạt động -> chuyển về DRAFT")
    void unpublishTour_Success() {
        tour.setStatus(TourStatus.PUBLISHED);
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(eq(tour.getTourId()), any()))
                .thenReturn(false);
        when(tourRepository.save(tour)).thenReturn(tour);

        tourService.unpublishTour(VENDOR_EMAIL, tour.getTourId());

        assertThat(tour.getStatus()).isEqualTo(TourStatus.DRAFT);
    }

    @Test
    @DisplayName("unpublishTour: còn nhóm ghép đang hoạt động -> ném AppException TOUR_HAS_ACTIVE_GROUPS")
    void unpublishTour_HasActiveGroups_ThrowsException() {
        tour.setStatus(TourStatus.PUBLISHED);
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(eq(tour.getTourId()), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> tourService.unpublishTour(VENDOR_EMAIL, tour.getTourId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_HAS_ACTIVE_GROUPS));
        assertThat(tour.getStatus()).isEqualTo(TourStatus.PUBLISHED);
    }

    @Test
    @DisplayName("unpublishTour: tour không ở trạng thái PUBLISHED -> ném AppException TOUR_NOT_PUBLISHED")
    void unpublishTour_NotPublished_ThrowsException() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> tourService.unpublishTour(VENDOR_EMAIL, tour.getTourId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_PUBLISHED));
    }

    // ---------------------------------------------------------------
    // deleteTour
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deleteTour: không có nhóm ghép hoạt động -> soft-delete tour và cascade")
    void deleteTour_Success() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(eq(tour.getTourId()), any()))
                .thenReturn(false);

        tourService.deleteTour(VENDOR_EMAIL, tour.getTourId());

        assertThat(tour.getIsDeleted()).isTrue();
        assertThat(tour.getDeletedAt()).isNotNull();
        assertThat(tour.getDeletedBy()).isEqualTo(VENDOR_EMAIL);
        verify(tourCheckpointRepository).softDeleteByTourId(eq(tour.getTourId()), any(), eq(VENDOR_EMAIL));
        verify(tourScheduleRepository).softDeleteByTourId(eq(tour.getTourId()), any(), eq(VENDOR_EMAIL));
        verify(tourImageRepository).softDeleteByTourId(eq(tour.getTourId()), any(), eq(VENDOR_EMAIL));
    }

    @Test
    @DisplayName("deleteTour: còn nhóm ghép đang hoạt động -> ném lỗi, không xoá gì cả")
    void deleteTour_HasActiveGroups_ThrowsException() {
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository.existsByTour_TourIdAndStatusInAndIsDeletedFalse(eq(tour.getTourId()), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> tourService.deleteTour(VENDOR_EMAIL, tour.getTourId()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_HAS_ACTIVE_GROUPS));
        verify(tourRepository, never()).save(any());
        assertThat(tour.getIsDeleted()).isFalse();
    }

    // ---------------------------------------------------------------
    // hideTourForViolation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("hideTourForViolation: tour PUBLISHED, có lý do -> ẩn tour và thông báo cho vendor")
    void hideTourForViolation_Success() {
        tour.setStatus(TourStatus.PUBLISHED);
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setUserId(adminId);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(tourRepository.save(tour)).thenReturn(tour);

        tourService.hideTourForViolation(adminId, tour.getTourId(), "  Vi phạm chính sách  ");

        assertThat(tour.getStatus()).isEqualTo(TourStatus.HIDDEN);
        assertThat(tour.getHiddenReason()).isEqualTo("Vi phạm chính sách");
        assertThat(tour.getHiddenBy()).isEqualTo(admin);
        verify(notificationService).notify(
                eq(manager.getUserId()), any(), any(), eq(tour.getTourId()), anyString(),
                eq(tour.getTourName()), eq("Vi phạm chính sách"));
    }

    @Test
    @DisplayName("hideTourForViolation: lý do rỗng -> ném AppException VALIDATION_ERROR, không tra DB")
    void hideTourForViolation_BlankReason_ThrowsValidationError() {
        assertThatThrownBy(() -> tourService.hideTourForViolation(UUID.randomUUID(), tour.getTourId(), "   "))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("hideTourForViolation: tour không ở trạng thái PUBLISHED -> ném AppException TOUR_NOT_PUBLISHED")
    void hideTourForViolation_NotPublished_ThrowsException() {
        UUID adminId = UUID.randomUUID();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(new User()));
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> tourService.hideTourForViolation(adminId, tour.getTourId(), "Vi phạm"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_PUBLISHED));
    }

    @Test
    @DisplayName("hideTourForViolation: admin không tồn tại -> ném AppException USER_NOT_FOUND")
    void hideTourForViolation_AdminNotFound_ThrowsUserNotFound() {
        UUID adminId = UUID.randomUUID();
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tourService.hideTourForViolation(adminId, tour.getTourId(), "Vi phạm"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }
}
