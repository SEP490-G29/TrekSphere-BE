package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CustomJourneyCreateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupCreateRequest;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorStatus;
import com.sep.treksphere.matching.service.impl.MatchingGroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingGroupCreateServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private com.sep.treksphere.matching.repository.GroupJoinApplicationRepository groupJoinApplicationRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private MatchingGroupMapper matchingGroupMapper = Mappers.getMapper(MatchingGroupMapper.class);

    @InjectMocks
    private MatchingGroupServiceImpl matchingGroupService;


    private User owner;
    private Tour tour;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUserId(UUID.randomUUID());
        owner.setFullName("P2-S3 Leader");
        owner.setStatus(UserStatus.ACTIVE);

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());
        vendor.setStatus(VendorStatus.ACTIVE);
        vendor.setIsDeleted(false);

        tour = new Tour();
        tour.setTourId(UUID.randomUUID());
        tour.setTourName("Approved Tour");
        tour.setStatus(TourStatus.PUBLISHED);
        tour.setVendor(vendor);
        tour.setMinCapacity(1);
        tour.setMaxCapacity(10);

        when(userRepository.findByIdForUpdate(owner.getUserId())).thenReturn(Optional.of(owner));
    }

    @Test
    @DisplayName("[P2-S3] Tạo Tour-backed Group kèm đúng một Leader và GroupTrip PLANNED")
    void createTourGroup_CreatesCompleteAggregate() {
        MatchingGroupCreateRequest request = tourRequest();
        stubGroupSave();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner), eq(tour), anyCollection(), any(LocalDateTime.class), any(LocalDate.class)))
                .thenReturn(false);

        matchingGroupService.createMatchingGroup(request, owner.getUserId());

        ArgumentCaptor<MatchingGroup> groupCaptor = ArgumentCaptor.forClass(MatchingGroup.class);
        ArgumentCaptor<GroupTrip> tripCaptor = ArgumentCaptor.forClass(GroupTrip.class);
        verify(matchingGroupRepository).save(groupCaptor.capture());
        verify(groupTripRepository).save(tripCaptor.capture());

        MatchingGroup createdGroup = groupCaptor.getValue();
        assertThat(createdGroup.getTour()).isSameAs(tour);
        assertThat(createdGroup.getCustomJourney()).isNull();
        assertThat(createdGroup.getStatus()).isEqualTo(MatchingGroupStatus.OPEN);
        assertThat(createdGroup.getCurrentSize()).isEqualTo(1);
        assertThat(createdGroup.getMembers()).singleElement().satisfies(this::assertInitialLeader);

        GroupTrip createdTrip = tripCaptor.getValue();
        assertThat(createdTrip.getMatchingGroup()).isSameAs(createdGroup);
        assertThat(createdTrip.getStatus()).isEqualTo(GroupTripStatus.PLANNED);
        assertThat(createdTrip.getScheduledStartAt()).isEqualTo(request.getScheduledStartAt());
    }

    @Test
    @DisplayName("[P2-S3] Tạo Custom Journey Group không cần Tour")
    void createCustomJourneyGroup_CreatesCompleteAggregate() {
        MatchingGroupCreateRequest request = customJourneyRequest();
        stubGroupSave();
        when(matchingGroupRepository
                .existsByOwnerAndTourIsNullAndGroupNameIgnoreCaseAndTargetDateAndIsDeletedFalse(
                        owner, "Ha Giang Explorers", request.getTargetDate()))
                .thenReturn(false);

        matchingGroupService.createMatchingGroup(request, owner.getUserId());

        ArgumentCaptor<MatchingGroup> groupCaptor = ArgumentCaptor.forClass(MatchingGroup.class);
        verify(matchingGroupRepository).save(groupCaptor.capture());
        verify(tourRepository, never()).findByTourIdAndIsDeletedFalse(any());

        MatchingGroup createdGroup = groupCaptor.getValue();
        assertThat(createdGroup.getTour()).isNull();
        assertThat(createdGroup.getCustomJourney()).isNotNull();
        assertThat(createdGroup.getCustomJourney().getMatchingGroup()).isSameAs(createdGroup);
        assertThat(createdGroup.getCustomJourney().getDifficulty()).isEqualTo(JourneyDifficulty.EXTREME);
        assertThat(createdGroup.getCustomJourney().getTitle()).isEqualTo("Ha Giang Loop");
        assertThat(createdGroup.getMembers()).singleElement().satisfies(this::assertInitialLeader);
        verify(groupTripRepository).save(any(GroupTrip.class));
    }

    @Test
    @DisplayName("[P2-S3] Từ chối request có Tour và Custom Journey cùng lúc")
    void createGroup_WithConflictingSources_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        request.setCustomJourney(validCustomJourney(request.getTargetDate()));

        assertErrorCode(request, ErrorCode.MATCHING_GROUP_SOURCE_INVALID);

        verify(matchingGroupRepository, never()).save(any());
        verify(groupTripRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Từ chối request không khai báo sourceType")
    void createGroup_WithoutSourceType_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        request.setSourceType(null);

        assertErrorCode(request, ErrorCode.MATCHING_GROUP_SOURCE_INVALID);

        verify(matchingGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Từ chối ngày Custom Journey mâu thuẫn với target date")
    void createCustomJourneyGroup_WithMismatchedTargetDate_IsRejected() {
        MatchingGroupCreateRequest request = customJourneyRequest();
        request.getCustomJourney().setStartDate(request.getTargetDate().plusDays(1));
        request.getCustomJourney().setEndDate(request.getTargetDate().plusDays(2));

        assertErrorCode(request, ErrorCode.CUSTOM_JOURNEY_TARGET_DATE_MISMATCH);
    }

    @Test
    @DisplayName("[P2-S3] Từ chối Custom Journey có ngày kết thúc trước ngày bắt đầu")
    void createCustomJourneyGroup_WithInvalidDateRange_IsRejected() {
        MatchingGroupCreateRequest request = customJourneyRequest();
        request.getCustomJourney().setEndDate(request.getTargetDate().minusDays(1));

        assertErrorCode(request, ErrorCode.CUSTOM_JOURNEY_DATE_INVALID);
    }

    @Test
    @DisplayName("[P2-S3] Từ chối scheduledStartAt đã qua")
    void createGroup_WithPastScheduledStart_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        request.setScheduledStartAt(LocalDateTime.now().minusMinutes(1));

        assertErrorCode(request, ErrorCode.INVALID_SCHEDULED_START);

        verify(tourRepository, never()).findByTourIdAndIsDeletedFalse(any());
    }

    @Test
    @DisplayName("[P2-S3] Retry Custom Journey trùng trả conflict và không ghi aggregate")
    void createCustomJourneyGroup_DuplicateRetry_IsRejected() {
        MatchingGroupCreateRequest request = customJourneyRequest();
        when(matchingGroupRepository
                .existsByOwnerAndTourIsNullAndGroupNameIgnoreCaseAndTargetDateAndIsDeletedFalse(
                        owner, "Ha Giang Explorers", request.getTargetDate()))
                .thenReturn(true);

        assertErrorCode(request, ErrorCode.DUPLICATE_MATCHING_GROUP);

        verify(matchingGroupRepository, never()).save(any());
        verify(groupTripRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Từ chối Tour không tồn tại")
    void createTourGroup_WithMissingTour_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.empty());

        assertErrorCode(request, ErrorCode.TOUR_NOT_FOUND);

        verify(matchingGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Từ chối Tour chưa được duyệt")
    void createTourGroup_WithUnapprovedTour_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        tour.setStatus(TourStatus.DRAFT);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertErrorCode(request, ErrorCode.MATCHING_TOUR_NOT_APPROVED);

        verify(matchingGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Từ chối kích thước nhóm vượt capacity Tour")
    void createTourGroup_ExceedingTourCapacity_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        request.setMaxSize(tour.getMaxCapacity() + 1);
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));

        assertErrorCode(request, ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);

        verify(matchingGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Retry Tour-backed Group trùng trả conflict")
    void createTourGroup_DuplicateRetry_IsRejected() {
        MatchingGroupCreateRequest request = tourRequest();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner), eq(tour), anyCollection(), any(LocalDateTime.class), any(LocalDate.class)))
                .thenReturn(true);

        assertErrorCode(request, ErrorCode.ALREADY_HAS_ACTIVE_GROUP);

        verify(matchingGroupRepository, never()).save(any());
        verify(groupTripRepository, never()).save(any());
    }

    @Test
    @DisplayName("[P2-S3] Lỗi tạo GroupTrip được phát ra để transaction rollback Group và Leader")
    void createGroup_WhenTripCreationFails_PropagatesFailureInsideTransaction() {
        MatchingGroupCreateRequest request = tourRequest();
        stubGroupSave();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner), eq(tour), anyCollection(), any(LocalDateTime.class), any(LocalDate.class)))
                .thenReturn(false);
        when(groupTripRepository.save(any(GroupTrip.class)))
                .thenThrow(new IllegalStateException("simulated trip persistence failure"));

        assertThatThrownBy(() -> matchingGroupService.createMatchingGroup(request, owner.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated trip persistence failure");

        verify(matchingGroupRepository).save(any(MatchingGroup.class));
        verify(groupTripRepository).save(any(GroupTrip.class));
    }

    @Test
    @DisplayName("[P2-S3] Lỗi lưu Group/Leader dừng transaction trước khi tạo GroupTrip")
    void createGroup_WhenLeaderCascadeFails_DoesNotCreateTrip() {
        MatchingGroupCreateRequest request = tourRequest();
        when(tourRepository.findByTourIdAndIsDeletedFalse(tour.getTourId())).thenReturn(Optional.of(tour));
        when(matchingGroupRepository
                .existsByOwnerAndTourAndStatusInAndMatchingDeadlineAfterAndTargetDateAfterAndIsDeletedFalse(
                        eq(owner), eq(tour), anyCollection(), any(LocalDateTime.class), any(LocalDate.class)))
                .thenReturn(false);
        when(matchingGroupRepository.save(any(MatchingGroup.class)))
                .thenThrow(new IllegalStateException("simulated leader cascade failure"));

        assertThatThrownBy(() -> matchingGroupService.createMatchingGroup(request, owner.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated leader cascade failure");

        verify(groupTripRepository, never()).save(any(GroupTrip.class));
    }

    private MatchingGroupCreateRequest tourRequest() {
        MatchingGroupCreateRequest request = baseRequest();
        request.setSourceType(MatchingGroupSourceType.TOUR);
        request.setTourId(tour.getTourId());
        return request;
    }

    private MatchingGroupCreateRequest customJourneyRequest() {
        MatchingGroupCreateRequest request = baseRequest();
        request.setSourceType(MatchingGroupSourceType.CUSTOM_JOURNEY);
        request.setGroupName(" Ha Giang Explorers ");
        request.setCustomJourney(validCustomJourney(request.getTargetDate()));
        return request;
    }

    private MatchingGroupCreateRequest baseRequest() {
        LocalDate targetDate = LocalDate.now().plusDays(10);
        MatchingGroupCreateRequest request = new MatchingGroupCreateRequest();
        request.setGroupName("Weekend Trekkers");
        request.setDescription(" A complete aggregate ");
        request.setMaxSize(6);
        request.setTargetDate(targetDate);
        request.setMatchingDeadline(LocalDateTime.now().plusDays(5));
        request.setScheduledStartAt(targetDate.atTime(6, 30));
        return request;
    }

    private CustomJourneyCreateRequest validCustomJourney(LocalDate targetDate) {
        CustomJourneyCreateRequest journey = new CustomJourneyCreateRequest();
        journey.setTitle(" Ha Giang Loop ");
        journey.setDescription(" Four-day custom route ");
        journey.setDifficulty(JourneyDifficulty.EXTREME);
        journey.setStartDate(targetDate);
        journey.setEndDate(targetDate.plusDays(3));
        return journey;
    }

    private void assertInitialLeader(MatchingMember member) {
        assertThat(member.getUser()).isSameAs(owner);
        assertThat(member.getRole()).isEqualTo(MatchingRole.LEADER);
        assertThat(member.getStatus()).isEqualTo(JoinStatus.ACCEPTED);
    }

    private void assertErrorCode(MatchingGroupCreateRequest request, ErrorCode expectedErrorCode) {
        assertThatThrownBy(() -> matchingGroupService.createMatchingGroup(request, owner.getUserId()))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private void stubGroupSave() {
        when(matchingGroupRepository.save(any(MatchingGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
