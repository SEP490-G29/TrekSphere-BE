package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.matching.dto.request.MomentCreateRequest;
import com.sep.treksphere.matching.dto.request.MomentFilterRequest;
import com.sep.treksphere.matching.dto.request.MomentHideRequest;
import com.sep.treksphere.matching.dto.request.MomentUpdateRequest;
import com.sep.treksphere.matching.dto.request.MomentVisibilityUpdateRequest;
import com.sep.treksphere.matching.dto.response.MomentMapResponse;
import com.sep.treksphere.matching.dto.response.MomentMediaResponse;
import com.sep.treksphere.matching.dto.response.MomentResponse;
import com.sep.treksphere.matching.entity.MatchingGroup;
import com.sep.treksphere.matching.entity.MatchingMember;
import com.sep.treksphere.matching.entity.Moment;
import com.sep.treksphere.matching.entity.MomentMedia;
import com.sep.treksphere.matching.enums.JoinStatus;
import com.sep.treksphere.matching.enums.MatchingGroupStatus;
import com.sep.treksphere.matching.enums.MatchingRole;
import com.sep.treksphere.matching.enums.MomentStatus;
import com.sep.treksphere.matching.enums.MomentVisibility;
import com.sep.treksphere.matching.mapper.MomentMapper;
import com.sep.treksphere.matching.repository.MatchingGroupRepository;
import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.repository.MomentMediaRepository;
import com.sep.treksphere.matching.repository.MomentRepository;
import com.sep.treksphere.matching.service.impl.MomentServiceImpl;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.user.User;
import com.sep.treksphere.user.UserRepository;
import com.sep.treksphere.user.UserStatus;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
class MomentServiceTest {

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private MomentMediaRepository momentMediaRepository;

    @Mock
    private MatchingGroupRepository matchingGroupRepository;

    @Mock
    private MatchingMemberRepository matchingMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Spy
    private MomentMapper momentMapper = Mappers.getMapper(MomentMapper.class);

    @InjectMocks
    private MomentServiceImpl momentService;

    private User authorUser;
    private User leaderUser;
    private User otherUser;
    private MatchingGroup matchingGroup;
    private MatchingMember authorMember;
    private MatchingMember leaderMember;
    private Moment groupMoment;
    private Moment personalMoment;

    private UUID groupId;
    private UUID authorUserId;
    private UUID leaderUserId;
    private UUID otherUserId;
    private UUID momentId;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        authorUserId = UUID.randomUUID();
        leaderUserId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        momentId = UUID.randomUUID();

        authorUser = new User();
        authorUser.setUserId(authorUserId);
        authorUser.setEmail("author@example.com");
        authorUser.setFullName("Nguyen Van Author");
        authorUser.setRoles(new HashSet<>());

        leaderUser = new User();
        leaderUser.setUserId(leaderUserId);
        leaderUser.setEmail("leader@example.com");
        leaderUser.setFullName("Tran Van Leader");
        leaderUser.setRoles(new HashSet<>());

        otherUser = new User();
        otherUser.setUserId(otherUserId);
        otherUser.setEmail("other@example.com");
        otherUser.setFullName("Le Van Other");
        otherUser.setRoles(new HashSet<>());

        matchingGroup = new MatchingGroup();
        matchingGroup.setMatchingGroupId(groupId);
        matchingGroup.setGroupName("Trek Fanxipan 2026");
        matchingGroup.setStatus(MatchingGroupStatus.COMPLETED);

        authorMember = new MatchingMember();
        authorMember.setMatchingMemberId(UUID.randomUUID());
        authorMember.setUser(authorUser);
        authorMember.setMatchingGroup(matchingGroup);
        authorMember.setRole(MatchingRole.MEMBER);
        authorMember.setStatus(JoinStatus.ACCEPTED);

        leaderMember = new MatchingMember();
        leaderMember.setMatchingMemberId(UUID.randomUUID());
        leaderMember.setUser(leaderUser);
        leaderMember.setMatchingGroup(matchingGroup);
        leaderMember.setRole(MatchingRole.LEADER);
        leaderMember.setStatus(JoinStatus.ACCEPTED);

        groupMoment = Moment.builder()
                .momentId(momentId)
                .authorUser(authorUser)
                .matchingGroup(matchingGroup)
                .authorMatchingMember(authorMember)
                .caption("Hoàng hôn trên đỉnh Fansipan")
                .capturedAt(LocalDateTime.now().minusDays(1))
                .placeName("Đỉnh Fansipan")
                .latitude(new BigDecimal("22.3033"))
                .longitude(new BigDecimal("103.7753"))
                .visibility(MomentVisibility.GROUP_ONLY)
                .status(MomentStatus.VISIBLE)
                .mediaList(new ArrayList<>())
                .build();

        MomentMedia media1 = MomentMedia.builder()
                .momentMediaId(UUID.randomUUID())
                .moment(groupMoment)
                .imageUrl("https://cloudinary.com/img1.jpg")
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .build();
        groupMoment.addMedia(media1);

        personalMoment = Moment.builder()
                .momentId(UUID.randomUUID())
                .authorUser(authorUser)
                .matchingGroup(null)
                .authorMatchingMember(null)
                .caption("Solo trekking Tà Xùa")
                .capturedAt(LocalDateTime.now().minusDays(2))
                .placeName("Sống lưng khủng long")
                .latitude(new BigDecimal("21.4333"))
                .longitude(new BigDecimal("104.3167"))
                .visibility(MomentVisibility.PUBLIC_PROFILE)
                .status(MomentStatus.VISIBLE)
                .mediaList(new ArrayList<>())
                .build();
    }

    // ==========================================
    // CREATE GROUP MOMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Tạo khoảnh khắc trong nhóm thành công")
    void createGroupMoment_Success() {
        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Khoảnh khắc tuyệt vời")
                .placeName("Trạm Tôn")
                .latitude(new BigDecimal("22.3500"))
                .longitude(new BigDecimal("103.7800"))
                .visibility(MomentVisibility.GROUP_ONLY)
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg", "https://cloudinary.com/photo2.jpg"))
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(matchingGroup));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, authorUserId)).thenReturn(Optional.of(authorMember));
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
            Moment m = invocation.getArgument(0);
            m.setMomentId(UUID.randomUUID());
            return m;
        });

        MomentResponse response = momentService.createGroupMoment(groupId, authorUserId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCaption()).isEqualTo("Khoảnh khắc tuyệt vời");
        assertThat(response.getPlaceName()).isEqualTo("Trạm Tôn");
        assertThat(response.getAuthorName()).isEqualTo("Nguyen Van Author");
        assertThat(response.getMediaList()).hasSize(2);
        verify(momentRepository).save(any(Moment.class));
        verify(notificationService).notify(
                org.mockito.ArgumentMatchers.<List<UUID>>any(),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.NotificationEventType.GROUP_MOMENT_CREATED),
                org.mockito.ArgumentMatchers.eq(com.sep.treksphere.notification.ReferenceType.MATCHING_GROUP),
                org.mockito.ArgumentMatchers.eq(groupId),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thất bại khi người dùng không phải thành viên ACCEPTED")
    void createGroupMoment_ThrowsException_WhenUserNotAcceptedMember() {
        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Cố tình đăng khi chưa là thành viên")
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg"))
                .build();

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(matchingGroup));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> momentService.createGroupMoment(groupId, otherUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCEPTED_MATCHING_MEMBER);
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thất bại khi nhóm chưa khởi hành (trạng thái OPEN)")
    void createGroupMoment_ThrowsException_WhenGroupOpen() {
        MatchingGroup openGroup = new MatchingGroup();
        openGroup.setMatchingGroupId(groupId);
        openGroup.setStatus(MatchingGroupStatus.OPEN);

        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Đăng ảnh khi chưa đi")
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg"))
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(openGroup));

        assertThatThrownBy(() -> momentService.createGroupMoment(groupId, authorUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_NOT_READY_FOR_MOMENT);
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thất bại khi nhóm bị hủy (trạng thái CANCELLED)")
    void createGroupMoment_ThrowsException_WhenGroupCancelled() {
        MatchingGroup cancelledGroup = new MatchingGroup();
        cancelledGroup.setMatchingGroupId(groupId);
        cancelledGroup.setStatus(MatchingGroupStatus.CANCELLED);

        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Đăng ảnh khi nhóm đã hủy")
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg"))
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(cancelledGroup));

        assertThatThrownBy(() -> momentService.createGroupMoment(groupId, authorUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GROUP_NOT_READY_FOR_MOMENT);
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thành công khi nhóm đang đi (trạng thái IN_PROGRESS)")
    void createGroupMoment_Success_WhenGroupInProgress() {
        MatchingGroup inProgressGroup = new MatchingGroup();
        inProgressGroup.setMatchingGroupId(groupId);
        inProgressGroup.setStatus(MatchingGroupStatus.IN_PROGRESS);

        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Đang check-in trên đường đi")
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg"))
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(inProgressGroup));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, authorUserId)).thenReturn(Optional.of(authorMember));
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
            Moment m = invocation.getArgument(0);
            m.setMomentId(UUID.randomUUID());
            return m;
        });

        MomentResponse response = momentService.createGroupMoment(groupId, authorUserId, request);

        assertThat(response).isNotNull();
        assertThat(response.getCaption()).isEqualTo("Đang check-in trên đường đi");
        verify(momentRepository).save(any(Moment.class));
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thất bại khi không có hình ảnh nào")
    void createGroupMoment_ThrowsException_WhenNoMedia() {
        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Không có ảnh")
                .mediaUrls(Collections.emptyList())
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(matchingGroup));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, authorUserId)).thenReturn(Optional.of(authorMember));

        assertThatThrownBy(() -> momentService.createGroupMoment(groupId, authorUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MOMENT_MEDIA_REQUIRED);
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thất bại khi tài khoản người dùng bị khóa (LOCKED)")
    void createGroupMoment_ThrowsException_WhenUserLocked() {
        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Đăng khi bị khóa tài khoản")
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg"))
                .build();

        User lockedUser = new User();
        lockedUser.setUserId(authorUserId);
        lockedUser.setStatus(UserStatus.LOCKED);

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> momentService.createGroupMoment(groupId, authorUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("Tạo khoảnh khắc thất bại khi không tìm thấy tài khoản người dùng")
    void createGroupMoment_ThrowsException_WhenUserNotFound() {
        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Đăng khi không có tài khoản")
                .mediaUrls(List.of("https://cloudinary.com/photo1.jpg"))
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> momentService.createGroupMoment(groupId, authorUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // ==========================================
    // CREATE PERSONAL MOMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Tạo khoảnh khắc cá nhân tự do thành công")
    void createPersonalMoment_Success() {
        MomentCreateRequest request = MomentCreateRequest.builder()
                .caption("Solo trek Mẫu Sơn")
                .placeName("Đỉnh Mẫu Sơn")
                .visibility(MomentVisibility.PUBLIC_PROFILE)
                .mediaUrls(List.of("https://cloudinary.com/solo1.jpg"))
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
            Moment m = invocation.getArgument(0);
            m.setMomentId(UUID.randomUUID());
            return m;
        });

        MomentResponse response = momentService.createPersonalMoment(authorUserId, request);

        assertThat(response).isNotNull();
        assertThat(response.getMatchingGroupId()).isNull();
        assertThat(response.getCaption()).isEqualTo("Solo trek Mẫu Sơn");
        assertThat(response.getVisibility()).isEqualTo(MomentVisibility.PUBLIC_PROFILE);
    }

    // ==========================================
    // UPDATE & DELETE MOMENT TESTS
    // ==========================================

    @Test
    @DisplayName("Tác giả cập nhật khoảnh khắc thành công")
    void updateGroupMoment_Success() {
        MomentUpdateRequest request = MomentUpdateRequest.builder()
                .caption("Caption đã sửa mới")
                .placeName("Địa điểm mới")
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));
        when(momentRepository.save(any(Moment.class))).thenReturn(groupMoment);

        MomentResponse response = momentService.updateGroupMoment(groupId, momentId, authorUserId, request);

        assertThat(response).isNotNull();
        assertThat(groupMoment.getCaption()).isEqualTo("Caption đã sửa mới");
        assertThat(groupMoment.getPlaceName()).isEqualTo("Địa điểm mới");
    }

    @Test
    @DisplayName("Cập nhật thất bại khi không phải tác giả của khoảnh khắc")
    void updateGroupMoment_ThrowsException_WhenNotAuthor() {
        MomentUpdateRequest request = MomentUpdateRequest.builder()
                .caption("Người khác cố tình sửa")
                .build();

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));

        assertThatThrownBy(() -> momentService.updateGroupMoment(groupId, momentId, otherUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_MOMENT_ACTION);
    }

    @Test
    @DisplayName("Tác giả xóa khoảnh khắc thành công (Soft-delete)")
    void deleteGroupMoment_Success() {
        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));

        momentService.deleteGroupMoment(groupId, momentId, authorUserId);

        assertThat(groupMoment.getIsDeleted()).isTrue();
        verify(momentRepository).save(groupMoment);
    }

    @Test
    @DisplayName("Tác giả thay đổi quyền hiển thị khoảnh khắc thành công")
    void updateGroupMomentVisibility_Success() {
        MomentVisibilityUpdateRequest request = MomentVisibilityUpdateRequest.builder()
                .visibility(MomentVisibility.PUBLIC_PROFILE)
                .build();

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));
        when(momentRepository.save(any(Moment.class))).thenReturn(groupMoment);

        MomentResponse response = momentService.updateGroupMomentVisibility(
                groupId, momentId, authorUserId, request);

        assertThat(response).isNotNull();
        assertThat(groupMoment.getVisibility()).isEqualTo(MomentVisibility.PUBLIC_PROFILE);
    }

    // ==========================================
    // MODERATION (HIDE / UNHIDE) TESTS
    // ==========================================

    @Test
    @DisplayName("Leader ẩn khoảnh khắc vi phạm trong nhóm thành công")
    void hideGroupMoment_Success_WhenLeader() {
        MomentHideRequest request = MomentHideRequest.builder()
                .hiddenReason("Hình ảnh không phù hợp")
                .build();

        when(userRepository.findById(leaderUserId)).thenReturn(Optional.of(leaderUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId)).thenReturn(Optional.of(leaderMember));
        when(momentRepository.save(any(Moment.class))).thenReturn(groupMoment);

        MomentResponse response = momentService.hideGroupMoment(groupId, momentId, leaderUserId, request);

        assertThat(response).isNotNull();
        assertThat(groupMoment.getStatus()).isEqualTo(MomentStatus.HIDDEN);
        assertThat(groupMoment.getHiddenReason()).isEqualTo("Hình ảnh không phù hợp");
        assertThat(groupMoment.getHiddenByUser()).isEqualTo(leaderUser);
    }

    @Test
    @DisplayName("Thành viên thường cố ẩn khoảnh khắc bị từ chối")
    void hideGroupMoment_ThrowsException_WhenNotLeader() {
        MomentHideRequest request = MomentHideRequest.builder()
                .hiddenReason("Thành viên thường đòi ẩn")
                .build();

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, otherUserId)).thenReturn(Optional.of(authorMember)); // role = MEMBER

        assertThatThrownBy(() -> momentService.hideGroupMoment(groupId, momentId, otherUserId, request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED_MOMENT_ACTION);
    }

    @Test
    @DisplayName("Leader bỏ ẩn khoảnh khắc thành công")
    void unhideGroupMoment_Success_WhenLeader() {
        groupMoment.setStatus(MomentStatus.HIDDEN);
        groupMoment.setHiddenReason("Lý do cũ");
        groupMoment.setHiddenByUser(leaderUser);

        when(userRepository.findById(leaderUserId)).thenReturn(Optional.of(leaderUser));
        when(momentRepository.findByMomentIdAndIsDeletedFalse(momentId)).thenReturn(Optional.of(groupMoment));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId)).thenReturn(Optional.of(leaderMember));
        when(momentRepository.save(any(Moment.class))).thenReturn(groupMoment);

        MomentResponse response = momentService.unhideGroupMoment(groupId, momentId, leaderUserId);

        assertThat(response).isNotNull();
        assertThat(groupMoment.getStatus()).isEqualTo(MomentStatus.VISIBLE);
        assertThat(groupMoment.getHiddenReason()).isNull();
        assertThat(groupMoment.getHiddenByUser()).isNull();
    }

    // ==========================================
    // RETRIEVAL & SHOWCASE TESTS
    // ==========================================

    @Test
    @DisplayName("Lấy timeline nhóm: Leader thấy cả khoảnh khắc ẩn")
    void getGroupMoments_Success_LeaderViewsAll() {
        MomentFilterRequest filter = new MomentFilterRequest();
        filter.setPage(0);
        filter.setSize(10);
        Pageable pageable = filter.getPageable();
        Page<Moment> page = new PageImpl<>(List.of(groupMoment), pageable, 1);

        when(userRepository.findById(leaderUserId)).thenReturn(Optional.of(leaderUser));
        when(matchingGroupRepository.findById(groupId)).thenReturn(Optional.of(matchingGroup));
        when(matchingMemberRepository.findByGroupIdAndUserId(groupId, leaderUserId)).thenReturn(Optional.of(leaderMember));
        when(momentRepository.findByMatchingGroup_MatchingGroupIdAndIsDeletedFalse(groupId, pageable))
                .thenReturn(page);

        PaginationResponse<MomentResponse> response = momentService.getGroupMoments(
                groupId, leaderUserId, filter);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Lấy danh sách khoảnh khắc cá nhân của chính mình")
    void getMyMoments_Success() {
        MomentFilterRequest filter = new MomentFilterRequest();
        filter.setPage(0);
        filter.setSize(10);
        Pageable pageable = filter.getPageable();
        Page<Moment> page = new PageImpl<>(List.of(groupMoment, personalMoment), pageable, 2);

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(momentRepository.findByAuthorUser_UserIdAndIsDeletedFalse(authorUserId, pageable))
                .thenReturn(page);

        PaginationResponse<MomentResponse> response = momentService.getMyMoments(
                authorUserId, filter);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Cộng đồng xem các khoảnh khắc PUBLIC_PROFILE của một user")
    void getUserPublicMoments_Success() {
        MomentFilterRequest filter = new MomentFilterRequest();
        filter.setPage(0);
        filter.setSize(10);
        Pageable pageable = filter.getPageable();
        Page<Moment> page = new PageImpl<>(List.of(personalMoment), pageable, 1);

        when(userRepository.findById(authorUserId)).thenReturn(Optional.of(authorUser));
        when(momentRepository.findByAuthorUser_UserIdAndVisibilityAndStatusAndIsDeletedFalse(
                authorUserId, MomentVisibility.PUBLIC_PROFILE, MomentStatus.VISIBLE, pageable))
                .thenReturn(page);

        PaginationResponse<MomentResponse> response = momentService.getUserPublicMoments(
                authorUserId, filter);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getVisibility()).isEqualTo(MomentVisibility.PUBLIC_PROFILE);
    }
}
