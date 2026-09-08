package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.request.MatchingGroupUpdateRequest;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.GroupTrip;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.GroupTripStatus;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.GroupTripRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.vendor.Vendor;
import com.sep.treksphere.vendor.VendorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingGroupLifecycleServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private GroupTripRepository groupTripRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private MatchingGroupMapper matchingGroupMapper = Mappers.getMapper(MatchingGroupMapper.class);

    @InjectMocks
    private MatchingGroupService matchingGroupService;

    private User leaderUser;
    private User otherUser;
    private CustomUserDetails leaderUserDetails;
    private CustomUserDetails otherUserDetails;

    private MatchingGroup tourGroup;
    private MatchingGroup customJourneyGroup;
    private Tour sampleTour;
    private Vendor sampleVendor;
    private CustomJourney sampleJourney;
    private GroupTrip sampleTrip;
    private MatchingMember leaderMember;

    @BeforeEach
    void setUp() {
        leaderUser = new User();
        leaderUser.setUserId(UUID.randomUUID());
        leaderUser.setFullName("Leader Nguyen");
        leaderUserDetails = new CustomUserDetails(leaderUser);

        otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setFullName("Member Tran");
        otherUserDetails = new CustomUserDetails(otherUser);

        sampleVendor = new Vendor();
        sampleVendor.setStatus(VendorStatus.ACTIVE);
        sampleVendor.setIsDeleted(false);

        sampleTour = new Tour();
        sampleTour.setTourId(UUID.randomUUID());
        sampleTour.setTourName("Fansipan Summit Tour");
        sampleTour.setStatus(TourStatus.PUBLISHED);
        sampleTour.setVendor(sampleVendor);
        sampleTour.setIsDeleted(false);
        sampleTour.setMinCapacity(2);
        sampleTour.setMaxCapacity(20);
        sampleTour.setDifficulty(DifficultyLevel.HARD);

        tourGroup = new MatchingGroup();
        tourGroup.setMatchingGroupId(UUID.randomUUID());
        tourGroup.setGroupName("Fansipan Weekend Trekkers");
        tourGroup.setDescription("Nhóm leo Fansipan cuối tuần");
        tourGroup.setMaxSize(8);
        tourGroup.setCurrentSize(1);
        tourGroup.setTargetDate(LocalDate.now().plusDays(10));
        tourGroup.setMatchingDeadline(LocalDateTime.now().plusDays(5));
        tourGroup.setStatus(MatchingGroupStatus.OPEN);
        tourGroup.setOwner(leaderUser);
        tourGroup.setTour(sampleTour);

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setUser(leaderUser);
        leaderMember.setMatchingGroup(tourGroup);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);
        leaderMember.setIsDeleted(false);
        tourGroup.setMembers(new HashSet<>(Set.of(leaderMember)));

        sampleTrip = new GroupTrip();
        sampleTrip.setGroupTripId(UUID.randomUUID());
        sampleTrip.setMatchingGroup(tourGroup);
        sampleTrip.setStatus(GroupTripStatus.PLANNED);
        sampleTrip.setScheduledStartAt(LocalDateTime.now().plusDays(10));

        sampleJourney = new CustomJourney();
        sampleJourney.setCustomJourneyId(UUID.randomUUID());
        sampleJourney.setTitle("Lao Than Cloud Hunting");
        sampleJourney.setDescription("Hành trình săn mây Lảo Thẩn 2N1Đ");
        sampleJourney.setDifficulty(JourneyDifficulty.MODERATE);
        sampleJourney.setStartDate(LocalDate.now().plusDays(10));
        sampleJourney.setEndDate(LocalDate.now().plusDays(11));
        sampleJourney.setIsLocked(false);

        customJourneyGroup = new MatchingGroup();
        customJourneyGroup.setMatchingGroupId(UUID.randomUUID());
        customJourneyGroup.setGroupName("Lao Than Trekkers");
        customJourneyGroup.setMaxSize(6);
        customJourneyGroup.setCurrentSize(1);
        customJourneyGroup.setTargetDate(LocalDate.now().plusDays(10));
        customJourneyGroup.setMatchingDeadline(LocalDateTime.now().plusDays(5));
        customJourneyGroup.setStatus(MatchingGroupStatus.OPEN);
        customJourneyGroup.setOwner(leaderUser);
        customJourneyGroup.setCustomJourney(sampleJourney);
        sampleJourney.setMatchingGroup(customJourneyGroup);

        MatchingMember cjLeaderMember = new MatchingMember();
        cjLeaderMember.setUser(leaderUser);
        cjLeaderMember.setMatchingGroup(customJourneyGroup);
        cjLeaderMember.setRole(MatchingRole.LEADER);
        cjLeaderMember.setStatus(JoinStatus.ACCEPTED);
        cjLeaderMember.setIsDeleted(false);
        customJourneyGroup.setMembers(new HashSet<>(Set.of(cjLeaderMember)));

        lenient().when(userRepository.getReferenceById(leaderUser.getUserId())).thenReturn(leaderUser);
        lenient().when(userRepository.getReferenceById(otherUser.getUserId())).thenReturn(otherUser);
        lenient().when(matchingMemberRepository.findByMatchingGroupAndUser(eq(tourGroup), eq(leaderUser)))
                .thenReturn(Optional.of(leaderMember));
        lenient().when(matchingMemberRepository.findByMatchingGroupAndUser(eq(customJourneyGroup), eq(leaderUser)))
                .thenReturn(Optional.of(cjLeaderMember));
        lenient().when(matchingMemberRepository.findByMatchingGroupAndUser(any(), eq(otherUser)))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("TC-P2-S4-01: Leader chỉnh sửa thông tin nhóm Tour thành công")
    void updateMatchingGroup_AsLeader_Success() {
        UUID groupId = tourGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setGroupName("Fansipan Advanced Trekkers");
        request.setDescription("Cập nhật lịch trình chi tiết và yêu cầu thể lực");
        request.setMaxSize(10);
        request.setTargetDate(LocalDate.now().plusDays(15));
        request.setMatchingDeadline(LocalDateTime.now().plusDays(10));

        MatchingGroupDetailResponse response = matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails);

        assertThat(response).isNotNull();
        assertThat(tourGroup.getGroupName()).isEqualTo("Fansipan Advanced Trekkers");
        assertThat(tourGroup.getDescription()).isEqualTo("Cập nhật lịch trình chi tiết và yêu cầu thể lực");
        assertThat(tourGroup.getMaxSize()).isEqualTo(10);
        assertThat(tourGroup.getTargetDate()).isEqualTo(LocalDate.now().plusDays(15));
        verify(matchingGroupRepository).save(tourGroup);
    }

    @Test
    @DisplayName("TC-P2-S4-02: Người không phải Leader chỉnh sửa nhóm bị từ chối 403 UNAUTHORIZED")
    void updateMatchingGroup_AsNonLeader_ThrowsUnauthorized() {
        UUID groupId = tourGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setGroupName("Unauthorized Change");

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request, otherUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);
    }

    @Test
    @DisplayName("TC-P2-S4-03: Giảm capacity nhỏ hơn số active member bị từ chối")
    void updateMatchingGroup_CapacityLessThanActiveMembers_ThrowsError() {
        UUID groupId = tourGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(5L);

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setMaxSize(4); // < active count 5

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_CAPACITY_LESS_THAN_ACTIVE_MEMBERS);
    }

    @Test
    @DisplayName("TC-P2-S4-04: Sức chứa vượt quá giới hạn Tour bị từ chối")
    void updateMatchingGroup_ExceedsTourCapacity_ThrowsError() {
        UUID groupId = tourGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(1L);

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setMaxSize(25); // Tour max is 20

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_SIZE_EXCEEDS_TOUR_CAPACITY);
    }

    @Test
    @DisplayName("TC-P2-S4-05: Ngày đi hoặc hạn chót không hợp lệ bị từ chối")
    void updateMatchingGroup_InvalidDates_ThrowsError() {
        UUID groupId = tourGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));

        MatchingGroupUpdateRequest request1 = new MatchingGroupUpdateRequest();
        request1.setTargetDate(LocalDate.now().minusDays(1)); // Quá khứ

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request1, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TARGET_DATE);

        MatchingGroupUpdateRequest request2 = new MatchingGroupUpdateRequest();
        request2.setMatchingDeadline(LocalDateTime.now().plusDays(20)); // Sau targetDate (plusDays(10))

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request2, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_DEADLINE);
    }

    @Test
    @DisplayName("TC-P2-S4-06: Leader cập nhật Custom Journey thành công")
    void updateCustomJourneyGroup_Success() {
        UUID groupId = customJourneyGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(customJourneyGroup));
        when(groupTripRepository.findByMatchingGroup(customJourneyGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(customJourneyGroup));

        CustomJourneyUpdateRequest cjRequest = new CustomJourneyUpdateRequest();
        cjRequest.setTitle("Lao Than Cloud Hunting - Updated");
        cjRequest.setDescription("Mô tả mới có thêm cắm trại đêm");
        cjRequest.setDifficulty(JourneyDifficulty.HARD);
        cjRequest.setStartDate(LocalDate.now().plusDays(10));
        cjRequest.setEndDate(LocalDate.now().plusDays(12));

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setCustomJourney(cjRequest);

        MatchingGroupDetailResponse response = matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails);

        assertThat(response).isNotNull();
        assertThat(sampleJourney.getTitle()).isEqualTo("Lao Than Cloud Hunting - Updated");
        assertThat(sampleJourney.getDifficulty()).isEqualTo(JourneyDifficulty.HARD);
        assertThat(sampleJourney.getEndDate()).isEqualTo(LocalDate.now().plusDays(12));
    }

    @Test
    @DisplayName("TC-P2-S4-07: Sửa Custom Journey khi đã bị khóa (is_locked=true) bị ném lỗi JOURNEY_LOCKED")
    void updateCustomJourneyGroup_WhenLocked_ThrowsJourneyLocked() {
        UUID groupId = customJourneyGroup.getMatchingGroupId();
        sampleJourney.setIsLocked(true);
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(customJourneyGroup));

        CustomJourneyUpdateRequest cjRequest = new CustomJourneyUpdateRequest();
        cjRequest.setTitle("Locked Change");

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setCustomJourney(cjRequest);

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOURNEY_LOCKED);
    }

    @Test
    @DisplayName("TC-P2-S4-08: Sửa Custom Journey khi chuyến đi đã bắt đầu bị ném lỗi JOURNEY_LOCKED")
    void updateCustomJourneyGroup_WhenTripStarted_ThrowsJourneyLocked() {
        UUID groupId = customJourneyGroup.getMatchingGroupId();
        sampleTrip.setStatus(GroupTripStatus.IN_PROGRESS);
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(customJourneyGroup));
        when(groupTripRepository.findByMatchingGroup(customJourneyGroup)).thenReturn(Optional.of(sampleTrip));

        CustomJourneyUpdateRequest cjRequest = new CustomJourneyUpdateRequest();
        cjRequest.setTitle("Started Trip Change");

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setCustomJourney(cjRequest);

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOURNEY_LOCKED);
    }

    @Test
    @DisplayName("TC-P2-S4-08B: Tự động đồng bộ startDate từ Custom Journey sang targetDate của nhóm")
    void updateCustomJourneyGroup_SyncsStartDateToTargetDate() {
        UUID groupId = customJourneyGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(customJourneyGroup));
        when(groupTripRepository.findByMatchingGroup(customJourneyGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(customJourneyGroup));

        LocalDate newDate = LocalDate.now().plusDays(20);
        CustomJourneyUpdateRequest cjRequest = new CustomJourneyUpdateRequest();
        cjRequest.setStartDate(newDate);
        cjRequest.setEndDate(newDate.plusDays(2));

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setCustomJourney(cjRequest);

        matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails);

        assertThat(customJourneyGroup.getTargetDate()).isEqualTo(newDate);
        assertThat(sampleJourney.getStartDate()).isEqualTo(newDate);
        assertThat(sampleJourney.getEndDate()).isEqualTo(newDate.plusDays(2));
    }

    @Test
    @DisplayName("TC-P2-S4-08C: Cập nhật targetDate ở ngoài tự động đồng bộ sang startDate của Custom Journey")
    void updateCustomJourneyGroup_SyncsTargetDateToStartDate() {
        UUID groupId = customJourneyGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(customJourneyGroup));
        when(groupTripRepository.findByMatchingGroup(customJourneyGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(customJourneyGroup));

        LocalDate newDate = LocalDate.now().plusDays(20);
        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setTargetDate(newDate);

        matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails);

        assertThat(customJourneyGroup.getTargetDate()).isEqualTo(newDate);
        assertThat(sampleJourney.getStartDate()).isEqualTo(newDate);
    }

    @Test
    @DisplayName("TC-P2-S4-08D: Truyền cả 2 ngày nhưng khác nhau bị ném lỗi CUSTOM_JOURNEY_TARGET_DATE_MISMATCH")
    void updateCustomJourneyGroup_TargetDateMismatch_ThrowsError() {
        UUID groupId = customJourneyGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(customJourneyGroup));

        CustomJourneyUpdateRequest cjRequest = new CustomJourneyUpdateRequest();
        cjRequest.setStartDate(LocalDate.now().plusDays(20));

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setTargetDate(LocalDate.now().plusDays(25)); // Khác nhau
        request.setCustomJourney(cjRequest);

        assertThatThrownBy(() -> matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_JOURNEY_TARGET_DATE_MISMATCH);
    }

    @Test
    @DisplayName("TC-P2-S4-09: Tăng sức chứa bằng currentSize tự chuyển trạng thái sang FULL")
    void updateMatchingGroup_AutoTransitionsToFull() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setCurrentSize(4);
        tourGroup.setStatus(MatchingGroupStatus.OPEN);

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(matchingMemberRepository.countActiveMembersByGroupIdAndStatus(groupId, JoinStatus.ACCEPTED)).thenReturn(4L);
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        MatchingGroupUpdateRequest request = new MatchingGroupUpdateRequest();
        request.setMaxSize(4); // equal to currentSize 4

        matchingGroupService.updateMatchingGroup(groupId, request, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.FULL);
    }

    @Test
    @DisplayName("TC-P2-S4-10: Leader ẩn nhóm (hide) thành công sang trạng thái HIDDEN")
    void hideMatchingGroup_AsLeader_Success() {
        UUID groupId = tourGroup.getMatchingGroupId();
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(groupTripRepository.findByMatchingGroup(tourGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        matchingGroupService.hideMatchingGroup(groupId, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.HIDDEN);
        verify(matchingGroupRepository).save(tourGroup);
    }

    @Test
    @DisplayName("TC-P2-S4-11: Ẩn nhóm khi Trip đã bắt đầu bị từ chối")
    void hideMatchingGroup_WhenTripInProgress_ThrowsInvalidState() {
        UUID groupId = tourGroup.getMatchingGroupId();
        sampleTrip.setStatus(GroupTripStatus.IN_PROGRESS);
        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(groupTripRepository.findByMatchingGroup(tourGroup)).thenReturn(Optional.of(sampleTrip));

        assertThatThrownBy(() -> matchingGroupService.hideMatchingGroup(groupId, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_INVALID_STATE);
    }

    @Test
    @DisplayName("TC-P2-S4-12: Hiển thị lại nhóm (show) tự động tính toán trạng thái OPEN")
    void showMatchingGroup_RecalculatesToOpen() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setStatus(MatchingGroupStatus.HIDDEN);
        tourGroup.setCurrentSize(2);
        tourGroup.setMaxSize(8);
        tourGroup.setMatchingDeadline(LocalDateTime.now().plusDays(3));
        tourGroup.setTargetDate(LocalDate.now().plusDays(5));

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(groupTripRepository.findByMatchingGroup(tourGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        matchingGroupService.showMatchingGroup(groupId, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.OPEN);
    }

    @Test
    @DisplayName("TC-P2-S4-13: Hiển thị lại nhóm (show) khi đã đủ người tự động chuyển FULL")
    void showMatchingGroup_RecalculatesToFull() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setStatus(MatchingGroupStatus.HIDDEN);
        tourGroup.setCurrentSize(8);
        tourGroup.setMaxSize(8);

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(groupTripRepository.findByMatchingGroup(tourGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        matchingGroupService.showMatchingGroup(groupId, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.FULL);
    }

    @Test
    @DisplayName("TC-P2-S4-14: Hiển thị lại nhóm (show) khi hết hạn chót tự động chuyển CLOSED")
    void showMatchingGroup_RecalculatesToClosed() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setStatus(MatchingGroupStatus.HIDDEN);
        tourGroup.setMatchingDeadline(LocalDateTime.now().minusHours(1)); // Đã qua hạn chót

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(groupTripRepository.findByMatchingGroup(tourGroup)).thenReturn(Optional.of(sampleTrip));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        matchingGroupService.showMatchingGroup(groupId, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.CLOSED);
    }

    @Test
    @DisplayName("TC-P2-S4-15: Leader chủ động đóng tuyển thành viên (close)")
    void closeMatchingGroup_AsLeader_Success() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setStatus(MatchingGroupStatus.OPEN);

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        matchingGroupService.closeMatchingGroup(groupId, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.CLOSED);
    }

    @Test
    @DisplayName("TC-P2-S4-16: Leader mở lại tuyển thành viên (open) khi điều kiện còn hợp lệ")
    void openMatchingGroup_AsLeader_Success() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setStatus(MatchingGroupStatus.CLOSED);
        tourGroup.setCurrentSize(2);
        tourGroup.setMaxSize(8);
        tourGroup.setMatchingDeadline(LocalDateTime.now().plusDays(3));
        tourGroup.setTargetDate(LocalDate.now().plusDays(5));

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));
        when(matchingGroupRepository.save(any(MatchingGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchingGroupRepository.findDetailById(eq(groupId))).thenReturn(Optional.of(tourGroup));

        matchingGroupService.openMatchingGroup(groupId, leaderUserDetails);

        assertThat(tourGroup.getStatus()).isEqualTo(MatchingGroupStatus.OPEN);
    }

    @Test
    @DisplayName("TC-P2-S4-17: Mở lại nhóm khi đã hết hạn chót bị từ chối")
    void openMatchingGroup_WhenDeadlinePassed_ThrowsError() {
        UUID groupId = tourGroup.getMatchingGroupId();
        tourGroup.setStatus(MatchingGroupStatus.CLOSED);
        tourGroup.setMatchingDeadline(LocalDateTime.now().minusMinutes(5));

        when(matchingGroupRepository.findByIdForUpdate(groupId)).thenReturn(Optional.of(tourGroup));

        assertThatThrownBy(() -> matchingGroupService.openMatchingGroup(groupId, leaderUserDetails))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_DEADLINE_PASSED);
    }
}
