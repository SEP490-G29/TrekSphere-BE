package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.security.CustomUserDetails;
import com.sep.treksphere.matching.dto.request.MatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.request.MyMatchingGroupFilterRequest;
import com.sep.treksphere.matching.dto.response.MatchingGroupDetailResponse;
import com.sep.treksphere.matching.dto.response.MatchingGroupResponse;
import com.sep.treksphere.matching.entity.CustomJourney;
import com.sep.treksphere.matching.entity.CustomJourneyCheckpoint;
import com.sep.treksphere.matching.entity.CustomJourneyCostItem;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.enums.CostItemCategory;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.JourneyDifficulty;
import com.sep.treksphere.matching.enums.MatchingGroupSourceType;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.mapper.MatchingGroupMapper;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.tour.Tour;
import com.sep.treksphere.tour.TourRepository;
import com.sep.treksphere.tour.TourStatus;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingGroupServiceTest {

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private TourRepository tourRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private MatchingGroupMapper matchingGroupMapper = Mappers.getMapper(MatchingGroupMapper.class);

    @InjectMocks
    private MatchingGroupService matchingGroupService;

    private User owner;
    private User memberUser;
    private MatchingGroup tourGroup;
    private MatchingGroup customJourneyGroup;
    private Tour sampleTour;
    private CustomJourney sampleJourney;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUserId(UUID.randomUUID());
        owner.setFullName("Leader John");
        owner.setAvatarUrl("https://example.com/leader.jpg");

        memberUser = new User();
        memberUser.setUserId(UUID.randomUUID());
        memberUser.setFullName("Member Alice");
        memberUser.setAvatarUrl("https://example.com/alice.jpg");

        // 1. Setup Tour-backed group
        sampleTour = new Tour();
        sampleTour.setTourId(UUID.randomUUID());
        sampleTour.setTourName("Fansipan Summit Trek");
        sampleTour.setLocation("Lao Cai, Sa Pa");
        sampleTour.setDifficulty(DifficultyLevel.HARD);
        sampleTour.setStatus(TourStatus.APPROVED);

        tourGroup = new MatchingGroup();
        tourGroup.setMatchingGroupId(UUID.randomUUID());
        tourGroup.setGroupName("Fansipan Weekend Warriors");
        tourGroup.setDescription("Conquering Fansipan peak together");
        tourGroup.setOwner(owner);
        tourGroup.setTour(sampleTour);
        tourGroup.setMaxSize(8);
        tourGroup.setCurrentSize(2);
        tourGroup.setTargetDate(LocalDate.now().plusDays(10));
        tourGroup.setMatchingDeadline(LocalDateTime.now().plusDays(5));
        tourGroup.setStatus(MatchingGroupStatus.OPEN);

        // 2. Setup Custom Journey group
        customJourneyGroup = new MatchingGroup();
        customJourneyGroup.setMatchingGroupId(UUID.randomUUID());
        customJourneyGroup.setGroupName("Ha Giang Off-the-beaten-path");
        customJourneyGroup.setDescription("Custom motor & hiking trip in Ha Giang");
        customJourneyGroup.setOwner(owner);
        customJourneyGroup.setTour(null); // No Tour
        customJourneyGroup.setMaxSize(6);
        customJourneyGroup.setCurrentSize(1);
        customJourneyGroup.setTargetDate(LocalDate.now().plusDays(15));
        customJourneyGroup.setMatchingDeadline(LocalDateTime.now().plusDays(8));
        customJourneyGroup.setStatus(MatchingGroupStatus.OPEN);

        sampleJourney = new CustomJourney();
        sampleJourney.setCustomJourneyId(UUID.randomUUID());
        sampleJourney.setMatchingGroup(customJourneyGroup);
        sampleJourney.setTitle("Ha Giang Loop Explorer");
        sampleJourney.setDescription("3 days 2 nights loop");
        sampleJourney.setDifficulty(JourneyDifficulty.MODERATE);
        sampleJourney.setStartDate(LocalDate.now().plusDays(15));
        sampleJourney.setEndDate(LocalDate.now().plusDays(18));
        sampleJourney.setIsLocked(false);

        CustomJourneyCheckpoint cp = new CustomJourneyCheckpoint();
        cp.setCustomJourneyCheckpointId(UUID.randomUUID());
        cp.setCustomJourney(sampleJourney);
        cp.setCheckpointOrder(1);
        cp.setTitle("Dong Van Karst Plateau");
        cp.setLocationName("Dong Van, Ha Giang");
        sampleJourney.getCheckpoints().add(cp);

        CustomJourneyCostItem ci = new CustomJourneyCostItem();
        ci.setCustomJourneyCostItemId(UUID.randomUUID());
        ci.setCustomJourney(sampleJourney);
        ci.setItemName("Homestay 2 nights");
        ci.setCategory(CostItemCategory.OTHER);
        ci.setEstimatedAmount(new BigDecimal("600000.00"));
        sampleJourney.getCostItems().add(ci);

        customJourneyGroup.setCustomJourney(sampleJourney);

        // Members for tourGroup
        MatchingMember leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setMatchingGroup(tourGroup);
        leaderMember.setUser(owner);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);

        MatchingMember normalMember = new MatchingMember();
        normalMember.setMatchingMemberId(UUID.randomUUID());
        normalMember.setMatchingGroup(tourGroup);
        normalMember.setUser(memberUser);
        normalMember.setRole(MatchingRole.MEMBER);
        normalMember.setStatus(JoinStatus.ACCEPTED);

        tourGroup.getMembers().add(leaderMember);
        tourGroup.getMembers().add(normalMember);
    }

    @Test
    @DisplayName("[P2-S2] Discovery: Tìm kiếm danh sách nhóm trả về Matching Summary thống nhất cho cả Tour và Custom Journey")
    void getMatchingGroups_ReturnsUnifiedSummaryForBothSources() {
        MatchingGroupFilterRequest filter = new MatchingGroupFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        Page<MatchingGroup> page = new PageImpl<>(List.of(tourGroup, customJourneyGroup), PageRequest.of(0, 10), 2);

        when(matchingGroupRepository.findAvailableMatchingGroups(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(page);

        PaginationResponse<MatchingGroupResponse> result = matchingGroupService.getMatchingGroups(filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        // Group 1: Tour-backed
        MatchingGroupResponse res1 = result.getContent().get(0);
        assertThat(res1.getSourceType()).isEqualTo(MatchingGroupSourceType.TOUR);
        assertThat(res1.getTourId()).isEqualTo(sampleTour.getTourId());
        assertThat(res1.getTourName()).isEqualTo("Fansipan Summit Trek");
        assertThat(res1.getDifficulty()).isEqualTo("HARD");
        assertThat(res1.getLocation()).isEqualTo("Lao Cai, Sa Pa");

        // Group 2: Custom Journey
        MatchingGroupResponse res2 = result.getContent().get(1);
        assertThat(res2.getSourceType()).isEqualTo(MatchingGroupSourceType.CUSTOM_JOURNEY);
        assertThat(res2.getTourId()).isNull();
        assertThat(res2.getCustomJourneyId()).isEqualTo(sampleJourney.getCustomJourneyId());
        assertThat(res2.getCustomJourneyTitle()).isEqualTo("Ha Giang Loop Explorer");
        assertThat(res2.getDifficulty()).isEqualTo("MODERATE");
        assertThat(res2.getLocation()).isEqualTo("Dong Van, Ha Giang");
        assertThat(res2.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("600000.00"));
    }

    @Test
    @DisplayName("[P2-S2] Privacy Boundary: Khách vãng lai (Guest chưa đăng nhập) xem chi tiết nhóm KHÔNG thấy danh sách members")
    void getMatchingGroupById_AsGuest_HidesMemberList() {
        when(matchingGroupRepository.findPublicDetailById(eq(tourGroup.getMatchingGroupId()), anyCollection(), any()))
                .thenReturn(Optional.of(tourGroup));

        MatchingGroupDetailResponse detail = matchingGroupService.getMatchingGroupById(tourGroup.getMatchingGroupId(), null);

        assertThat(detail).isNotNull();
        assertThat(detail.getGroupName()).isEqualTo("Fansipan Weekend Warriors");
        assertThat(detail.getCurrentSize()).isEqualTo(2);
        assertThat(detail.getMaxSize()).isEqualTo(8);
        assertThat(detail.getOwnerName()).isEqualTo("Leader John");
        // Bắt buộc members phải là rỗng khi chưa đăng nhập
        assertThat(detail.getMembers()).isEmpty();
        assertThat(detail.getIsOwner()).isFalse();
        assertThat(detail.getCanJoin()).isFalse();
    }

    @Test
    @DisplayName("[P2-S2] Privacy Boundary: Trekker chưa tham gia (Non-member) xem chi tiết nhóm KHÔNG thấy danh sách members")
    void getMatchingGroupById_AsNonMember_HidesMemberList() {
        User outsider = new User();
        outsider.setUserId(UUID.randomUUID());
        CustomUserDetails userDetails = new CustomUserDetails(outsider);

        when(matchingGroupRepository.findPublicDetailById(eq(tourGroup.getMatchingGroupId()), anyCollection(), any()))
                .thenReturn(Optional.of(tourGroup));

        MatchingGroupDetailResponse detail = matchingGroupService.getMatchingGroupById(tourGroup.getMatchingGroupId(), userDetails);

        assertThat(detail).isNotNull();
        // Bắt buộc members phải là rỗng đối với người ngoài
        assertThat(detail.getMembers()).isEmpty();
        assertThat(detail.getIsOwner()).isFalse();
        assertThat(detail.getCanJoin()).isTrue(); // Được phép nộp đơn xin tham gia
    }

    @Test
    @DisplayName("[P2-S2] Privacy Boundary: Thành viên đã duyệt (Accepted Member) xem chi tiết nhóm ĐƯỢC THẤY danh sách members")
    void getMatchingGroupById_AsAcceptedMember_ShowsMemberList() {
        CustomUserDetails userDetails = new CustomUserDetails(memberUser);

        when(matchingGroupRepository.findPublicDetailById(eq(tourGroup.getMatchingGroupId()), anyCollection(), any()))
                .thenReturn(Optional.of(tourGroup));

        MatchingGroupDetailResponse detail = matchingGroupService.getMatchingGroupById(tourGroup.getMatchingGroupId(), userDetails);

        assertThat(detail).isNotNull();
        // Accepted Member được xem danh sách thành viên trong nhóm
        assertThat(detail.getMembers()).hasSize(2);
        assertThat(detail.getMyMembershipStatus()).isEqualTo(JoinStatus.ACCEPTED);
        assertThat(detail.getCanLeave()).isTrue();
    }

    @Test
    @DisplayName("[P2-S2] Privacy Boundary: Leader (Chủ nhóm) xem chi tiết nhóm ĐƯỢC THẤY danh sách members")
    void getMatchingGroupById_AsLeader_ShowsMemberList() {
        CustomUserDetails userDetails = new CustomUserDetails(owner);

        when(matchingGroupRepository.findPublicDetailById(eq(tourGroup.getMatchingGroupId()), anyCollection(), any()))
                .thenReturn(Optional.of(tourGroup));

        MatchingGroupDetailResponse detail = matchingGroupService.getMatchingGroupById(tourGroup.getMatchingGroupId(), userDetails);

        assertThat(detail).isNotNull();
        assertThat(detail.getIsOwner()).isTrue();
        assertThat(detail.getMembers()).hasSize(2);
    }

    @Test
    @DisplayName("[P2-S2] getMyMatchingGroups: Phân biệt rõ ràng nhóm làm Leader (isOwner=true, myRole=LEADER) và nhóm làm Member (isOwner=false, myRole=MEMBER)")
    void getMyMatchingGroups_Success() {
        // tourGroup do owner làm chủ
        // customJourneyGroup đổi chủ thành memberUser để test trường hợp owner tham gia với tư cách member
        customJourneyGroup.setOwner(memberUser);

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        MyMatchingGroupFilterRequest filter = new MyMatchingGroupFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        Page<MatchingGroup> page = new PageImpl<>(List.of(tourGroup, customJourneyGroup), PageRequest.of(0, 10), 2);

        when(matchingGroupRepository.findOwnedOrJoinedGroups(
                eq(owner.getUserId()), eq(MatchingRole.MEMBER), eq(JoinStatus.ACCEPTED), isNull(), isNull(), anyString(), any()
        )).thenReturn(page);

        PaginationResponse<MatchingGroupResponse> response = matchingGroupService.getMyMatchingGroups(filter, userDetails);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);

        // Nhóm 1: Do owner làm chủ -> isOwner = true, myRole = LEADER
        MatchingGroupResponse group1 = response.getContent().get(0);
        assertThat(group1.getMatchingGroupId()).isEqualTo(tourGroup.getMatchingGroupId());
        assertThat(group1.getIsOwner()).isTrue();
        assertThat(group1.getMyRole()).isEqualTo(MatchingRole.LEADER);

        // Nhóm 2: Do memberUser làm chủ -> isOwner = false, myRole = MEMBER
        MatchingGroupResponse group2 = response.getContent().get(1);
        assertThat(group2.getMatchingGroupId()).isEqualTo(customJourneyGroup.getMatchingGroupId());
        assertThat(group2.getIsOwner()).isFalse();
        assertThat(group2.getMyRole()).isEqualTo(MatchingRole.MEMBER);
    }
}
