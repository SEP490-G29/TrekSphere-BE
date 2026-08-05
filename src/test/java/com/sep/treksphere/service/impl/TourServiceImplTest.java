package com.sep.treksphere.service.impl;

import com.sep.treksphere.entity.Notification;
import com.sep.treksphere.entity.Tour;
import com.sep.treksphere.entity.User;
import com.sep.treksphere.entity.Vendor;
import com.sep.treksphere.enums.system.NotificationEventType;
import com.sep.treksphere.enums.tour.TourStatus;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;
import com.sep.treksphere.mapper.TourMapper;
import com.sep.treksphere.repository.BookingRepository;
import com.sep.treksphere.repository.NotificationRepository;
import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.repository.TourCheckpointRepository;
import com.sep.treksphere.repository.TourImageRepository;
import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceImplTest {

    @Mock private TourRepository tourRepository;
    @Mock private TourImageRepository tourImageRepository;
    @Mock private TourCheckpointRepository tourCheckpointRepository;
    @Mock private TourScheduleRepository tourScheduleRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private VendorStaffRepository vendorStaffRepository;
    @Mock private UserRepository userRepository;
    @Mock private TourMapper tourMapper;
    @Mock private FileService fileService;

    @InjectMocks
    private TourServiceImpl service;

    private static final String MANAGER_EMAIL = "manager@treksphere.test";

    private UUID tourId;
    private Tour tour;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        tourId = UUID.randomUUID();

        User creator = new User();
        creator.setUserId(UUID.randomUUID());
        creator.setFullName("Tour creator");
        creator.setEmail("creator@treksphere.test");

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setManager(creator);
        vendor.setCompanyName("TrekSphere Vendor");
        vendor.setContactEmail("vendor@treksphere.test");
        vendor.setContactPhone("0900000000");

        tour = new Tour();
        tour.setTourId(tourId);
        tour.setTourName("Ta Nang - Phan Dung");
        tour.setStatus(TourStatus.PENDING_APPROVAL);
        tour.setVendor(vendor);
        tour.setCreator(creator);
    }

    @Test
    void approveTour_ApprovesPendingTourAndClearsOldRejectionReason() {
        tour.setRejectionReason("Old reason");
        stubTourDetailDependencies();

        var response = service.approveTour(MANAGER_EMAIL, tourId);

        assertThat(tour.getStatus()).isEqualTo(TourStatus.APPROVED);
        assertThat(tour.getRejectionReason()).isNull();
        assertThat(response.getStatus()).isEqualTo(TourStatus.APPROVED);
        assertThat(response.getRejectionReason()).isNull();
        verify(tourRepository).save(tour);
        verify(vendorRepository).findByManager_Email(MANAGER_EMAIL);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getEventType()).isEqualTo(NotificationEventType.TOUR_APPROVED);
        assertThat(notificationCaptor.getValue().getRecipient()).isSameAs(tour.getCreator());
    }

    @Test
    void rejectTour_RejectsPendingTourAndPersistsTrimmedReason() {
        stubTourDetailDependencies();

        var response = service.rejectTour(MANAGER_EMAIL, tourId, "  Thiếu thông tin an toàn  ");

        assertThat(tour.getStatus()).isEqualTo(TourStatus.REJECTED);
        assertThat(tour.getRejectionReason()).isEqualTo("Thiếu thông tin an toàn");
        assertThat(response.getRejectionReason()).isEqualTo("Thiếu thông tin an toàn");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getEventType()).isEqualTo(NotificationEventType.TOUR_REJECTED);
        assertThat(notificationCaptor.getValue().getContent()).contains("Thiếu thông tin an toàn");
    }

    @Test
    void rejectTour_RejectsBlankReasonBeforeQueryingTour() {
        assertThatThrownBy(() -> service.rejectTour(MANAGER_EMAIL, tourId, "   "))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REJECTION_REASON_REQUIRED);

        verifyNoInteractions(tourRepository);
    }

    @Test
    void approveTour_RejectsTourOutsidePendingApprovalState() {
        tour.setStatus(TourStatus.DRAFT);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tourId)).thenReturn(Optional.of(tour));

        when(vendorRepository.findByManager_Email(MANAGER_EMAIL)).thenReturn(Optional.of(vendor));

        assertThatThrownBy(() -> service.approveTour(MANAGER_EMAIL, tourId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOUR_NOT_PENDING_APPROVAL);

        verify(tourRepository, never()).save(any());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void approveTour_RejectsTourOwnedByAnotherVendor() {
        Vendor anotherVendor = new Vendor();
        anotherVendor.setVendorId(UUID.randomUUID());
        when(vendorRepository.findByManager_Email(MANAGER_EMAIL)).thenReturn(Optional.of(anotherVendor));
        when(tourRepository.findByTourIdAndIsDeletedFalse(tourId)).thenReturn(Optional.of(tour));

        assertThatThrownBy(() -> service.approveTour(MANAGER_EMAIL, tourId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOUR_NOT_BELONG_TO_VENDOR);

        verify(tourRepository, never()).save(any());
        verifyNoInteractions(notificationRepository);
    }

    private void stubTourDetailDependencies() {
        when(vendorRepository.findByManager_Email(MANAGER_EMAIL)).thenReturn(Optional.of(vendor));
        when(tourRepository.findByTourIdAndIsDeletedFalse(tourId)).thenReturn(Optional.of(tour));
        when(tourRepository.save(tour)).thenReturn(tour);
        when(tourImageRepository.findByTourOrderBySortOrderAsc(tour)).thenReturn(List.of());
        when(tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour)).thenReturn(List.of());
        when(tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(tour)).thenReturn(List.of());
        when(reviewRepository.findAverageRatingByTourAndStatus(any(), any())).thenReturn(null);
        when(reviewRepository.countByTourAndStatusAndIsDeletedFalse(any(), any())).thenReturn(0);
    }
}
