package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointCreateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyCheckpointUpdateRequest;
import com.sep.treksphere.matching.dto.request.CustomJourneyUpdateRequest;
import com.sep.treksphere.matching.dto.response.CustomJourneyCheckpointResponse;
import com.sep.treksphere.matching.dto.response.CustomJourneyDetailResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.CustomJourneyMapper;
import com.sep.treksphere.matching.repository.CustomJourneyCheckpointRepository;
import com.sep.treksphere.matching.repository.CustomJourneyRepository;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.impl.CustomJourneyServiceImpl;
import com.sep.treksphere.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomJourneyServiceTest {

    @Mock
    private CustomJourneyRepository customJourneyRepository;

    @Mock
    private CustomJourneyCheckpointRepository checkpointRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Spy
    private CustomJourneyMapper customJourneyMapper = Mappers.getMapper(CustomJourneyMapper.class);

    @InjectMocks
    private CustomJourneyServiceImpl customJourneyService;

    private UUID groupId;
    private UUID leaderId;
    private UUID memberId;
    private UUID outsiderId;
    private MatchingGroup group;
    private CustomJourney journey;
    private CustomJourneyCheckpoint checkpoint;
    private MatchingMember leaderMember;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        leaderId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();

        User leaderUser = new User();
        leaderUser.setUserId(leaderId);
        leaderUser.setFullName("Leader User");

        group = new MatchingGroup();
        group.setMatchingGroupId(groupId);
        group.setStatus(MatchingGroupStatus.OPEN);
        group.setIsDeleted(false);
        group.setOwner(leaderUser);

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(group);
        leaderMember.setUser(leaderUser);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);
        leaderMember.setIsDeleted(false);

        journey = new CustomJourney();
        journey.setCustomJourneyId(UUID.randomUUID());
        journey.setMatchingGroup(group);
        journey.setTitle("Lao Than Trek");
        journey.setDescription("2D1N trek");
        journey.setDifficulty(JourneyDifficulty.MODERATE);
        journey.setStartDate(LocalDate.now().plusDays(5));
        journey.setEndDate(LocalDate.now().plusDays(6));
        journey.setIsLocked(false);
        journey.setCheckpoints(new HashSet<>());
        journey.setCostItems(new HashSet<>());
        journey.setIsDeleted(false);

        checkpoint = new CustomJourneyCheckpoint();
        checkpoint.setCustomJourneyCheckpointId(UUID.randomUUID());
        checkpoint.setCustomJourney(journey);
        checkpoint.setDayNo(1);
        checkpoint.setCheckpointOrder(1);
        checkpoint.setTitle("Checkpoint 1");
        checkpoint.setLocationName("Base Camp");
        checkpoint.setLatitude(new BigDecimal("22.1234567"));
        checkpoint.setLongitude(new BigDecimal("103.1234567"));
        checkpoint.setIsDeleted(false);
    }

    @Test
    @DisplayName("getJourneyByGroupId - thành công khi group OPEN")
    void getJourneyByGroupId_Success() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(customJourneyRepository.findDetailByGroupId(groupId)).thenReturn(Optional.of(journey));

        CustomJourneyDetailResponse response = customJourneyService.getJourneyByGroupId(groupId, outsiderId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Lao Than Trek");
        assertThat(response.getDifficulty()).isEqualTo(JourneyDifficulty.MODERATE);
    }

    @Test
    @DisplayName("getJourneyByGroupId - ném lỗi khi group không tồn tại")
    void getJourneyByGroupId_GroupNotFound() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customJourneyService.getJourneyByGroupId(groupId, outsiderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("getJourneyByGroupId - ném lỗi khi group HIDDEN và outsider truy cập")
    void getJourneyByGroupId_HiddenGroupForbiddenForOutsider() {
        group.setStatus(MatchingGroupStatus.HIDDEN);
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.existsByMatchingGroup_MatchingGroupIdAndUser_UserIdAndStatusAndIsDeletedFalse(
                groupId, outsiderId, JoinStatus.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> customJourneyService.getJourneyByGroupId(groupId, outsiderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_WORKSPACE_ACCESS);
    }

    @Test
    @DisplayName("updateJourney - Leader cập nhật thành công khi chưa khóa")
    void updateJourney_Success() {
        CustomJourneyUpdateRequest request = new CustomJourneyUpdateRequest();
        request.setTitle("Updated Title");
        request.setDifficulty(JourneyDifficulty.HARD);

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findDetailByGroupId(groupId)).thenReturn(Optional.of(journey));
        when(customJourneyRepository.save(any(CustomJourney.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomJourneyDetailResponse response = customJourneyService.updateJourney(groupId, request, leaderId);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getDifficulty()).isEqualTo(JourneyDifficulty.HARD);
        verify(customJourneyRepository).save(any(CustomJourney.class));
    }

    @Test
    @DisplayName("updateJourney - ném lỗi khi người cập nhật không phải Leader")
    void updateJourney_ForbiddenWhenNotLeader() {
        CustomJourneyUpdateRequest request = new CustomJourneyUpdateRequest();
        request.setTitle("Updated Title");

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));

        assertThatThrownBy(() -> customJourneyService.updateJourney(groupId, request, memberId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MATCHING_GROUP_UNAUTHORIZED_MANAGE);
    }

    @Test
    @DisplayName("updateJourney - ném lỗi khi Journey đã bị khóa (isLocked = true)")
    void updateJourney_ConflictWhenLocked() {
        journey.setIsLocked(true);
        CustomJourneyUpdateRequest request = new CustomJourneyUpdateRequest();
        request.setTitle("Updated Title");

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findDetailByGroupId(groupId)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> customJourneyService.updateJourney(groupId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.JOURNEY_LOCKED);
    }

    @Test
    @DisplayName("updateJourney - ném lỗi khi endDate trước startDate")
    void updateJourney_InvalidDateRange() {
        CustomJourneyUpdateRequest request = new CustomJourneyUpdateRequest();
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now().plusDays(5));

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findDetailByGroupId(groupId)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> customJourneyService.updateJourney(groupId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_JOURNEY_DATE_INVALID);
    }

    @Test
    @DisplayName("updateJourney - ném lỗi khi startDate không khớp với targetDate của group")
    void updateJourney_StartDateMismatchGroupTargetDate() {
        group.setTargetDate(LocalDate.now().plusDays(5));
        CustomJourneyUpdateRequest request = new CustomJourneyUpdateRequest();
        request.setStartDate(LocalDate.now().plusDays(10));

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findDetailByGroupId(groupId)).thenReturn(Optional.of(journey));

        assertThatThrownBy(() -> customJourneyService.updateJourney(groupId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_JOURNEY_TARGET_DATE_MISMATCH);
    }

    @Test
    @DisplayName("getCheckpoints - lấy danh sách checkpoints thành công")
    void getCheckpoints_Success() {
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)).thenReturn(Optional.of(journey));
        when(checkpointRepository.findByCustomJourney_CustomJourneyIdAndIsDeletedFalseOrderByDayNoAscCheckpointOrderAsc(journey.getCustomJourneyId()))
                .thenReturn(List.of(checkpoint));

        List<CustomJourneyCheckpointResponse> response = customJourneyService.getCheckpoints(groupId, outsiderId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Checkpoint 1");
    }

    @Test
    @DisplayName("createCheckpoint - Leader tạo checkpoint thành công")
    void createCheckpoint_Success() {
        CustomJourneyCheckpointCreateRequest request = CustomJourneyCheckpointCreateRequest.builder()
                .dayNo(1)
                .checkpointOrder(1)
                .title("Checkpoint 1")
                .locationName("Base Camp")
                .plannedStartAt(LocalDateTime.now().plusDays(5).withHour(8))
                .plannedEndAt(LocalDateTime.now().plusDays(5).withHour(11))
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)).thenReturn(Optional.of(journey));
        when(checkpointRepository.existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndIsDeletedFalse(
                journey.getCustomJourneyId(), 1)).thenReturn(false);
        when(checkpointRepository.save(any(CustomJourneyCheckpoint.class))).thenAnswer(inv -> {
            CustomJourneyCheckpoint c = inv.getArgument(0);
            c.setCustomJourneyCheckpointId(UUID.randomUUID());
            return c;
        });

        CustomJourneyCheckpointResponse response = customJourneyService.createCheckpoint(groupId, request, leaderId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Checkpoint 1");
        verify(checkpointRepository).save(any(CustomJourneyCheckpoint.class));
    }

    @Test
    @DisplayName("createCheckpoint - ném lỗi khi checkpointOrder bị trùng lặp")
    void createCheckpoint_OrderDuplicated() {
        CustomJourneyCheckpointCreateRequest request = CustomJourneyCheckpointCreateRequest.builder()
                .checkpointOrder(1)
                .title("Checkpoint 1")
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)).thenReturn(Optional.of(journey));
        when(checkpointRepository.existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndIsDeletedFalse(
                journey.getCustomJourneyId(), 1)).thenReturn(true);

        assertThatThrownBy(() -> customJourneyService.createCheckpoint(groupId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHECKPOINT_ORDER_DUPLICATED);
    }

    @Test
    @DisplayName("createCheckpoint - ném lỗi khi plannedEndAt trước plannedStartAt")
    void createCheckpoint_InvalidTimeRange() {
        CustomJourneyCheckpointCreateRequest request = CustomJourneyCheckpointCreateRequest.builder()
                .checkpointOrder(1)
                .title("Checkpoint 1")
                .plannedStartAt(LocalDateTime.now().plusDays(5).withHour(12))
                .plannedEndAt(LocalDateTime.now().plusDays(5).withHour(8))
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)).thenReturn(Optional.of(journey));
        when(checkpointRepository.existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndIsDeletedFalse(
                journey.getCustomJourneyId(), 1)).thenReturn(false);

        assertThatThrownBy(() -> customJourneyService.createCheckpoint(groupId, request, leaderId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHECKPOINT_TIME_INVALID);
    }

    @Test
    @DisplayName("updateCheckpoint - Leader cập nhật checkpoint thành công")
    void updateCheckpoint_Success() {
        UUID cpId = checkpoint.getCustomJourneyCheckpointId();
        CustomJourneyCheckpointUpdateRequest request = CustomJourneyCheckpointUpdateRequest.builder()
                .title("Updated Checkpoint")
                .checkpointOrder(2)
                .build();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)).thenReturn(Optional.of(journey));
        when(checkpointRepository.findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(cpId, journey.getCustomJourneyId()))
                .thenReturn(Optional.of(checkpoint));
        when(checkpointRepository.existsByCustomJourney_CustomJourneyIdAndCheckpointOrderAndCustomJourneyCheckpointIdNotAndIsDeletedFalse(
                journey.getCustomJourneyId(), 2, cpId)).thenReturn(false);
        when(checkpointRepository.save(any(CustomJourneyCheckpoint.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomJourneyCheckpointResponse response = customJourneyService.updateCheckpoint(groupId, cpId, request, leaderId);

        assertThat(response.getTitle()).isEqualTo("Updated Checkpoint");
        verify(checkpointRepository).save(any(CustomJourneyCheckpoint.class));
    }

    @Test
    @DisplayName("deleteCheckpoint - Leader xoá mềm checkpoint thành công")
    void deleteCheckpoint_Success() {
        UUID cpId = checkpoint.getCustomJourneyCheckpointId();

        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(matchingMemberRepository.findActiveMembers(groupId, JoinStatus.ACCEPTED)).thenReturn(List.of(leaderMember));
        when(customJourneyRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId)).thenReturn(Optional.of(journey));
        when(checkpointRepository.findByCustomJourneyCheckpointIdAndCustomJourney_CustomJourneyIdAndIsDeletedFalse(cpId, journey.getCustomJourneyId()))
                .thenReturn(Optional.of(checkpoint));

        customJourneyService.deleteCheckpoint(groupId, cpId, leaderId);

        assertThat(checkpoint.getIsDeleted()).isTrue();
        verify(checkpointRepository).save(checkpoint);
    }
}
