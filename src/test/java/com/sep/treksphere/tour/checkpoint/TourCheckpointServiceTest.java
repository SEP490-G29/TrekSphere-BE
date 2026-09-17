package com.sep.treksphere.tour.checkpoint;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.tour.DifficultyLevel;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Bao phủ 4 method mà `unit_test_gap_report.md` liệt kê thiếu cho `TourCheckpointService`. */
@ExtendWith(MockitoExtension.class)
class TourCheckpointServiceTest {

    @Mock private TourCheckpointRepository tourCheckpointRepository;
    @Mock private TourRepository tourRepository;
    @Mock private VendorAccessService vendorAccessService;
    @Mock private FileService fileService;
    @Mock private TourReadinessService readinessService;

    @InjectMocks
    private TourCheckpointService checkpointService;

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
        tour.setDifficulty(DifficultyLevel.MODERATE);
        tour.setStatus(TourStatus.PUBLISHED);
        tour.setVendor(vendor);
        tour.setIsDeleted(false);
    }

    private TourCheckpointRequest validRequest() {
        TourCheckpointRequest request = new TourCheckpointRequest();
        request.setCheckpointName("  Trạm dừng 1  ");
        request.setCheckpointOrder(1);
        return request;
    }

    // ---------------------------------------------------------------
    // getCheckpointsByTourId
    // ---------------------------------------------------------------

    @Test
    @DisplayName("getCheckpointsByTourId: tour tồn tại -> trả về danh sách checkpoint theo thứ tự")
    void getCheckpointsByTourId_Success() {
        TourCheckpoint checkpoint = new TourCheckpoint();
        checkpoint.setTourCheckpointId(UUID.randomUUID());
        checkpoint.setTour(tour);
        checkpoint.setCheckpointName("Trạm 1");
        checkpoint.setCheckpointOrder(1);

        when(tourRepository.findPublishedDetailById(tour.getTourId())).thenReturn(Optional.of(tour));
        when(tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour))
                .thenReturn(List.of(checkpoint));

        List<TourCheckpointResponse> result = checkpointService.getCheckpointsByTourId(tour.getTourId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCheckpointName()).isEqualTo("Trạm 1");
    }

    @Test
    @DisplayName("getCheckpointsByTourId: tour không tồn tại -> ném AppException TOUR_NOT_FOUND")
    void getCheckpointsByTourId_TourNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(tourRepository.findPublishedDetailById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkpointService.getCheckpointsByTourId(randomId))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // createCheckpoint
    // ---------------------------------------------------------------

    @Test
    @DisplayName("createCheckpoint: hợp lệ -> tạo checkpoint mới, trim tên")
    void createCheckpoint_Success() {
        when(tourRepository.findById(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourCheckpointRepository.existsByTourAndCheckpointOrderAndIsDeletedFalse(tour, 1)).thenReturn(false);
        when(tourCheckpointRepository.existsByTourAndCheckpointNameIgnoreCaseAndIsDeletedFalse(tour, "Trạm dừng 1"))
                .thenReturn(false);
        when(tourCheckpointRepository.save(any(TourCheckpoint.class))).thenAnswer(inv -> {
            TourCheckpoint cp = inv.getArgument(0);
            cp.setTourCheckpointId(UUID.randomUUID());
            return cp;
        });

        TourCheckpointResponse response = checkpointService.createCheckpoint(
                tour.getTourId(), validRequest(), null, VENDOR_EMAIL);

        assertThat(response.getCheckpointName()).isEqualTo("Trạm dừng 1");
        assertThat(response.getTourId()).isEqualTo(tour.getTourId().toString());
    }

    @Test
    @DisplayName("createCheckpoint: trùng thứ tự -> ném AppException CHECKPOINT_DUPLICATE_ORDER")
    void createCheckpoint_DuplicateOrder_ThrowsException() {
        when(tourRepository.findById(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourCheckpointRepository.existsByTourAndCheckpointOrderAndIsDeletedFalse(tour, 1)).thenReturn(true);

        assertThatThrownBy(() -> checkpointService.createCheckpoint(
                tour.getTourId(), validRequest(), null, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHECKPOINT_DUPLICATE_ORDER));
        verify(tourCheckpointRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckpoint: tour thuộc vendor khác -> ném AppException TOUR_NOT_BELONG_TO_VENDOR")
    void createCheckpoint_NotOwner_ThrowsException() {
        Vendor otherVendor = new Vendor();
        otherVendor.setVendorId(UUID.randomUUID());
        when(tourRepository.findById(tour.getTourId())).thenReturn(Optional.of(tour));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(otherVendor);

        assertThatThrownBy(() -> checkpointService.createCheckpoint(
                tour.getTourId(), validRequest(), null, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_BELONG_TO_VENDOR));
    }

    @Test
    @DisplayName("createCheckpoint: tour không tồn tại/đã xoá -> ném AppException TOUR_NOT_FOUND")
    void createCheckpoint_TourNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(tourRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkpointService.createCheckpoint(
                randomId, validRequest(), null, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // updateCheckpoint
    // ---------------------------------------------------------------

    @Test
    @DisplayName("updateCheckpoint: hợp lệ -> cập nhật thông tin checkpoint")
    void updateCheckpoint_Success() {
        UUID checkpointId = UUID.randomUUID();
        TourCheckpoint existing = new TourCheckpoint();
        existing.setTourCheckpointId(checkpointId);
        existing.setTour(tour);
        existing.setIsDeleted(false);

        when(tourCheckpointRepository.findById(checkpointId)).thenReturn(Optional.of(existing));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourCheckpointRepository.existsByTourAndCheckpointOrderAndTourCheckpointIdNotAndIsDeletedFalse(
                tour, 1, checkpointId)).thenReturn(false);
        when(tourCheckpointRepository.existsByTourAndCheckpointNameIgnoreCaseAndTourCheckpointIdNotAndIsDeletedFalse(
                tour, "Trạm dừng 1", checkpointId)).thenReturn(false);
        when(tourCheckpointRepository.save(existing)).thenReturn(existing);

        TourCheckpointResponse response = checkpointService.updateCheckpoint(
                checkpointId, validRequest(), null, VENDOR_EMAIL);

        assertThat(response.getCheckpointName()).isEqualTo("Trạm dừng 1");
        assertThat(existing.getCheckpointOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateCheckpoint: trùng tên với checkpoint khác -> ném AppException CHECKPOINT_DUPLICATE_NAME")
    void updateCheckpoint_DuplicateName_ThrowsException() {
        UUID checkpointId = UUID.randomUUID();
        TourCheckpoint existing = new TourCheckpoint();
        existing.setTourCheckpointId(checkpointId);
        existing.setTour(tour);
        existing.setIsDeleted(false);

        when(tourCheckpointRepository.findById(checkpointId)).thenReturn(Optional.of(existing));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        when(tourCheckpointRepository.existsByTourAndCheckpointOrderAndTourCheckpointIdNotAndIsDeletedFalse(
                tour, 1, checkpointId)).thenReturn(false);
        when(tourCheckpointRepository.existsByTourAndCheckpointNameIgnoreCaseAndTourCheckpointIdNotAndIsDeletedFalse(
                tour, "Trạm dừng 1", checkpointId)).thenReturn(true);

        assertThatThrownBy(() -> checkpointService.updateCheckpoint(
                checkpointId, validRequest(), null, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHECKPOINT_DUPLICATE_NAME));
    }

    @Test
    @DisplayName("updateCheckpoint: checkpoint không tồn tại -> ném AppException CHECKPOINT_NOT_FOUND")
    void updateCheckpoint_NotFound_ThrowsException() {
        UUID checkpointId = UUID.randomUUID();
        when(tourCheckpointRepository.findById(checkpointId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkpointService.updateCheckpoint(
                checkpointId, validRequest(), null, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHECKPOINT_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // deleteCheckpoint
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deleteCheckpoint: hợp lệ -> soft-delete checkpoint")
    void deleteCheckpoint_Success() {
        UUID checkpointId = UUID.randomUUID();
        TourCheckpoint existing = new TourCheckpoint();
        existing.setTourCheckpointId(checkpointId);
        existing.setTour(tour);
        existing.setIsDeleted(false);

        when(tourCheckpointRepository.findById(checkpointId)).thenReturn(Optional.of(existing));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);

        checkpointService.deleteCheckpoint(checkpointId, VENDOR_EMAIL);

        assertThat(existing.getIsDeleted()).isTrue();
        assertThat(existing.getDeletedAt()).isNotNull();
        assertThat(existing.getDeletedBy()).isEqualTo(VENDOR_EMAIL);
        verify(readinessService).ensureCanRemoveCheckpoint(tour);
    }

    @Test
    @DisplayName("deleteCheckpoint: readiness chặn (tour publish cần >=2 checkpoint) -> ném lỗi, không xoá")
    void deleteCheckpoint_ReadinessBlocks_ThrowsException() {
        UUID checkpointId = UUID.randomUUID();
        TourCheckpoint existing = new TourCheckpoint();
        existing.setTourCheckpointId(checkpointId);
        existing.setTour(tour);
        existing.setIsDeleted(false);

        when(tourCheckpointRepository.findById(checkpointId)).thenReturn(Optional.of(existing));
        when(vendorAccessService.resolveActiveByManagerEmail(VENDOR_EMAIL)).thenReturn(vendor);
        doThrow(new AppException(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET))
                .when(readinessService).ensureCanRemoveCheckpoint(tour);

        assertThatThrownBy(() -> checkpointService.deleteCheckpoint(checkpointId, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOUR_PUBLISH_REQUIREMENTS_NOT_MET));
        assertThat(existing.getIsDeleted()).isFalse();
        verify(tourCheckpointRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCheckpoint: checkpoint không tồn tại -> ném AppException CHECKPOINT_NOT_FOUND")
    void deleteCheckpoint_NotFound_ThrowsException() {
        UUID checkpointId = UUID.randomUUID();
        when(tourCheckpointRepository.findById(checkpointId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkpointService.deleteCheckpoint(checkpointId, VENDOR_EMAIL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CHECKPOINT_NOT_FOUND));
    }
}
