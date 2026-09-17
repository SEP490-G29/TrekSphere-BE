package com.sep.treksphere.tour;

import com.sep.treksphere.file.FileService;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.tour.checkpoint.TourCheckpointRepository;
import com.sep.treksphere.tour.dto.request.CreateTourRequest;
import com.sep.treksphere.tour.dto.request.UpdateTourRequest;
import com.sep.treksphere.tour.dto.response.PublicTourDetailResponse;
import com.sep.treksphere.tour.dto.response.TourDetailResponse;
import com.sep.treksphere.tour.image.TourImageRepository;
import com.sep.treksphere.tour.policy.TourParticipationPolicy;
import com.sep.treksphere.tour.policy.TourParticipationPolicyRepository;
import com.sep.treksphere.tour.policy.TourParticipationPolicyRequest;
import com.sep.treksphere.tour.policy.TourParticipationPolicyResponse;
import com.sep.treksphere.tour.schedule.TourScheduleRepository;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {

    @Mock
    private TourRepository tourRepository;

    @Mock
    private TourImageRepository tourImageRepository;

    @Mock
    private TourCheckpointRepository tourCheckpointRepository;

    @Mock
    private TourScheduleRepository tourScheduleRepository;

    @Mock
    private TourParticipationPolicyRepository tourParticipationPolicyRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TourMapper tourMapper = new TourMapper();

    @Mock
    private FileService fileService;

    @Mock
    private VendorAccessService vendorAccessService;

    @Mock
    private TourReadinessService readinessService;

    @InjectMocks
    private TourService tourService;

    private User managerUser;
    private Vendor vendor;
    private UUID tourId;
    private Tour tour;

    @BeforeEach
    void setUp() {
        tourId = UUID.randomUUID();
        managerUser = new User();
        managerUser.setUserId(UUID.randomUUID());
        managerUser.setEmail("manager@vendor.com");
        managerUser.setFullName("Vendor Manager");

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setCompanyName("TrekSphere Travel Co.");
        vendor.setManager(managerUser);

        tour = new Tour();
        tour.setTourId(tourId);
        tour.setTourName("Fansipan Trek");
        tour.setDescription("Chinh phục nóc nhà Đông Dương");
        tour.setDifficulty(DifficultyLevel.MODERATE);
        tour.setLocation("Sa Pa, Lào Cai");
        tour.setDurationDays(2);
        tour.setMinCapacity(2);
        tour.setMaxCapacity(15);
        tour.setTotalDistanceKm(new BigDecimal("25.50"));
        tour.setHighlights("- Ngắm biển mây");
        tour.setIncludes("- HDV địa phương");
        tour.setExcludes("- Chi phí cá nhân");
        tour.setCoverImageUrl("https://cloudinary.com/cover.jpg");
        tour.setStatus(TourStatus.DRAFT);
        tour.setVendor(vendor);
        tour.setCreator(managerUser);
    }

    @Test
    @DisplayName("Tạo tour mới lưu đầy đủ thông tin cơ bản, coverImageUrl và participationPolicy")
    void createTour_ShouldSaveAllFieldsAndPolicy() {
        CreateTourRequest request = new CreateTourRequest();
        request.setTourName("Fansipan Trek");
        request.setDescription("Chinh phục nóc nhà Đông Dương");
        request.setDifficulty(DifficultyLevel.MODERATE);
        request.setLocation("Sa Pa, Lào Cai");
        request.setDurationDays(2);
        request.setMinCapacity(2);
        request.setMaxCapacity(15);
        request.setTotalDistanceKm(new BigDecimal("25.50"));
        request.setHighlights("- Ngắm biển mây");
        request.setIncludes("- HDV địa phương");
        request.setExcludes("- Chi phí cá nhân");
        request.setCoverImageUrl("https://cloudinary.com/cover.jpg");

        TourParticipationPolicyRequest policyRequest = TourParticipationPolicyRequest.builder()
                .minAge(16)
                .maxAge(55)
                .fitnessLevel("MODERATE")
                .healthRequirements("Chạy bộ 3km/ngày")
                .restrictedMedicalConditions("Không bệnh tim")
                .requiredExperience("Đã leo núi 1 lần")
                .requiredSkills("Dùng gậy leo núi")
                .requiredEquipment("Giày trekking, balo")
                .requiredDocuments("CCCD")
                .requiresHealthDeclaration(true)
                .requiresMedicalCertificate(false)
                .guardianRequiredUnderAge(18)
                .additionalRequirements("Không uống rượu")
                .build();
        request.setParticipationPolicy(policyRequest);

        when(vendorAccessService.resolveActiveByManagerEmail("manager@vendor.com")).thenReturn(vendor);
        when(userRepository.findByEmail("manager@vendor.com")).thenReturn(Optional.of(managerUser));
        when(tourRepository.save(any(Tour.class))).thenReturn(tour);
        when(tourImageRepository.findByTourAndIsDeletedFalseOrderBySortOrderAsc(any())).thenReturn(Collections.emptyList());
        when(tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(any())).thenReturn(Collections.emptyList());
        TourParticipationPolicy policy = tourMapper.toParticipationPolicy(policyRequest, tour);
        when(tourParticipationPolicyRepository.findByTour_TourIdAndIsDeletedFalse(tourId)).thenReturn(Optional.of(policy));

        TourDetailResponse response = tourService.createTour("manager@vendor.com", request, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getTourName()).isEqualTo("Fansipan Trek");
        assertThat(response.getTotalDistanceKm()).isEqualTo(new BigDecimal("25.50"));
        assertThat(response.getHighlights()).isEqualTo("- Ngắm biển mây");
        assertThat(response.getIncludes()).isEqualTo("- HDV địa phương");
        assertThat(response.getExcludes()).isEqualTo("- Chi phí cá nhân");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cloudinary.com/cover.jpg");
        assertThat(response.getParticipationPolicy()).isNotNull();
        assertThat(response.getParticipationPolicy().getMinAge()).isEqualTo(16);
        assertThat(response.getParticipationPolicy().getFitnessLevel()).isEqualTo("MODERATE");

        verify(tourParticipationPolicyRepository).save(any(TourParticipationPolicy.class));
    }

    @Test
    @DisplayName("Cập nhật tour cập nhật đầy đủ các trường và participationPolicy")
    void updateTour_ShouldUpdateAllFieldsAndPolicy() {
        UpdateTourRequest request = new UpdateTourRequest();
        request.setTourName("Fansipan Trek 3N2D");
        request.setDescription("Lịch trình mới 3 ngày 2 đêm");
        request.setDifficulty(DifficultyLevel.HARD);
        request.setLocation("Sa Pa, Lào Cai");
        request.setDurationDays(3);
        request.setMinCapacity(3);
        request.setMaxCapacity(20);
        request.setTotalDistanceKm(new BigDecimal("35.00"));
        request.setHighlights("- Bình minh nóc nhà Đông Dương");
        request.setIncludes("- Đầy đủ ăn uống");
        request.setExcludes("- Chi phí mua sắm");
        request.setCoverImageUrl("https://cloudinary.com/new-cover.jpg");

        TourParticipationPolicyRequest policyRequest = TourParticipationPolicyRequest.builder()
                .minAge(18)
                .maxAge(60)
                .fitnessLevel("HIGH")
                .healthRequirements("Thể lực tốt")
                .requiresHealthDeclaration(true)
                .build();
        request.setParticipationPolicy(policyRequest);

        TourParticipationPolicy existingPolicy = tourMapper.toParticipationPolicy(policyRequest, tour);

        when(vendorAccessService.resolveActiveByManagerEmail("manager@vendor.com")).thenReturn(vendor);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tourId))
                .thenReturn(Optional.of(tour));
        when(tourRepository.save(any(Tour.class))).thenReturn(tour);
        when(tourParticipationPolicyRepository.findByTour_TourIdAndIsDeletedFalse(tourId))
                .thenReturn(Optional.of(existingPolicy));
        when(tourImageRepository.findByTourAndIsDeletedFalseOrderBySortOrderAsc(any())).thenReturn(Collections.emptyList());
        when(tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(any())).thenReturn(Collections.emptyList());
        when(tourScheduleRepository.findByTourAndIsDeletedFalseOrderByDepartureDateAsc(any())).thenReturn(Collections.emptyList());

        TourDetailResponse response = tourService.updateTour("manager@vendor.com", tourId, request, null, null);

        assertThat(response).isNotNull();
        verify(tourParticipationPolicyRepository).save(existingPolicy);
        assertThat(existingPolicy.getMinAge()).isEqualTo(18);
        assertThat(existingPolicy.getFitnessLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Lấy chi tiết tour công khai trả về đầy đủ participationPolicy và highlights")
    void getTourById_ShouldReturnParticipationPolicy() {
        tour.setStatus(TourStatus.PUBLISHED);
        TourParticipationPolicy policy = TourParticipationPolicy.builder()
                .tour(tour)
                .minAge(18)
                .fitnessLevel("MODERATE")
                .requiresHealthDeclaration(true)
                .build();

        when(tourRepository.findPublishedDetailById(tourId)).thenReturn(Optional.of(tour));
        when(tourImageRepository.findByTourAndIsDeletedFalseOrderBySortOrderAsc(tour)).thenReturn(Collections.emptyList());
        when(tourCheckpointRepository.findByTourAndIsDeletedFalseOrderByCheckpointOrderAsc(tour)).thenReturn(Collections.emptyList());
        when(tourScheduleRepository.findByTourAndStatusAndDepartureDateGreaterThanEqualAndIsDeletedFalseOrderByDepartureDateAsc(
                eq(tour), any(), any())).thenReturn(Collections.emptyList());
        when(tourParticipationPolicyRepository.findByTour_TourIdAndIsDeletedFalse(tourId)).thenReturn(Optional.of(policy));

        PublicTourDetailResponse response = tourService.getTourById(tourId);

        assertThat(response).isNotNull();
        assertThat(response.getTourName()).isEqualTo("Fansipan Trek");
        assertThat(response.getHighlights()).isEqualTo("- Ngắm biển mây");
        assertThat(response.getParticipationPolicy()).isNotNull();
        assertThat(response.getParticipationPolicy().getMinAge()).isEqualTo(18);
        assertThat(response.getParticipationPolicy().getFitnessLevel()).isEqualTo("MODERATE");
    }
}
