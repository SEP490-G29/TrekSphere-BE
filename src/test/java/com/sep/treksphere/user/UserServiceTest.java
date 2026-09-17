package com.sep.treksphere.user;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.tour.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileService fileService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private com.sep.treksphere.notification.NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = new User();
        sampleUser.setUserId(sampleUserId);
        sampleUser.setEmail("trekker@example.com");
        sampleUser.setPhone("0987654321");
        sampleUser.setFullName("Nguyen Van Trekker");
        sampleUser.setAvatarUrl("https://example.com/avatar.jpg");
        sampleUser.setGender(Gender.MALE);
        sampleUser.setBio("Enthusiastic hiker");
        sampleUser.setExperienceLevel(ExperienceLevel.INTERMEDIATE);
        sampleUser.setPreferredDifficulty(DifficultyLevel.MODERATE);
        sampleUser.setPreferredAreas(List.of("Ha Giang", "Lao Cai"));
        sampleUser.setSkills(List.of("First Aid", "Navigation"));
        sampleUser.setTrustScore((short) 95);
        sampleUser.setTrustReviewCount(12);
    }

    @Test
    @DisplayName("[P2-S1] Xem hồ sơ leo núi công khai: trả về đúng các trường hiking và không chứa email, sđt")
    void getPublicHikingSummary_Success() {
        PublicHikingSummaryResponse expectedResponse = PublicHikingSummaryResponse.builder()
                .userId(sampleUserId)
                .fullName("Nguyen Van Trekker")
                .bio("Enthusiastic hiker")
                .experienceLevel(ExperienceLevel.INTERMEDIATE)
                .preferredDifficulty(DifficultyLevel.MODERATE)
                .preferredAreas(List.of("Ha Giang", "Lao Cai"))
                .skills(List.of("First Aid", "Navigation"))
                .trustScore((short) 95)
                .trustReviewCount(12)
                .build();

        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toPublicHikingSummaryResponse(sampleUser)).thenReturn(expectedResponse);

        PublicHikingSummaryResponse response = userService.getPublicHikingSummary(sampleUserId);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(sampleUserId);
        assertThat(response.getFullName()).isEqualTo("Nguyen Van Trekker");
        assertThat(response.getBio()).isEqualTo("Enthusiastic hiker");
        assertThat(response.getExperienceLevel()).isEqualTo(ExperienceLevel.INTERMEDIATE);
        assertThat(response.getPreferredDifficulty()).isEqualTo(DifficultyLevel.MODERATE);
        assertThat(response.getPreferredAreas()).containsExactly("Ha Giang", "Lao Cai");
        assertThat(response.getSkills()).containsExactly("First Aid", "Navigation");
        assertThat(response.getTrustScore()).isEqualTo((short) 95);
        assertThat(response.getTrustReviewCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("[P2-S1] Xem hồ sơ leo núi công khai khi user không tồn tại -> Bắn AppException USER_NOT_FOUND")
    void getPublicHikingSummary_UserNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getPublicHikingSummary(nonExistentId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("[P2-S1] Cập nhật hồ sơ cá nhân: cập nhật đầy đủ các trường hiking profile")
    void updateProfile_WithHikingFields_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyen Van Trekker Updated");
        request.setGender(Gender.MALE);
        request.setBio("Updated bio");
        request.setExperienceLevel(ExperienceLevel.ADVANCED);
        request.setPreferredDifficulty(DifficultyLevel.HARD);
        request.setPreferredAreas(List.of("Yen Bai", "Lai Chau"));
        request.setSkills(List.of("Survival", "First Aid", "Climbing"));

        UserProfileResponse expectedResponse = new UserProfileResponse();
        expectedResponse.setFullName("Nguyen Van Trekker Updated");
        expectedResponse.setBio("Updated bio");

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(expectedResponse);

        UserProfileResponse response = userService.updateProfile("trekker@example.com", request, null);

        assertThat(response).isNotNull();
        assertThat(sampleUser.getFullName()).isEqualTo("Nguyen Van Trekker Updated");
        assertThat(sampleUser.getBio()).isEqualTo("Updated bio");
        assertThat(sampleUser.getExperienceLevel()).isEqualTo(ExperienceLevel.ADVANCED);
        assertThat(sampleUser.getPreferredDifficulty()).isEqualTo(DifficultyLevel.HARD);
        assertThat(sampleUser.getPreferredAreas()).containsExactly("yen bai", "lai chau");
        assertThat(sampleUser.getSkills()).containsExactly("survival", "first aid", "climbing");
    }

    @Test
    @DisplayName("getUserProfile: email tồn tại -> trả về hồ sơ người dùng")
    void getUserProfile_Success() {
        UserProfileResponse expected = new UserProfileResponse();
        expected.setFullName(sampleUser.getFullName());
        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(expected);

        UserProfileResponse response = userService.getUserProfile(sampleUser.getEmail());

        assertThat(response.getFullName()).isEqualTo(sampleUser.getFullName());
    }

    @Test
    @DisplayName("getUserProfile: email không tồn tại -> ném AppException USER_NOT_FOUND")
    void getUserProfile_NotFound_ThrowsException() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile("ghost@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("getUserById: userId tồn tại -> trả về hồ sơ người dùng")
    void getUserById_Success() {
        UserProfileResponse expected = new UserProfileResponse();
        expected.setFullName(sampleUser.getFullName());
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(expected);

        UserProfileResponse response = userService.getUserById(sampleUserId.toString());

        assertThat(response.getFullName()).isEqualTo(sampleUser.getFullName());
    }

    @Test
    @DisplayName("getUserById: userId không tồn tại -> ném AppException USER_NOT_FOUND")
    void getUserById_NotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(randomId.toString()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("getUsers: trả về danh sách user đã phân trang cho Admin")
    void getUsers_ReturnsPaginatedUsers() {
        UserFilterRequest filter = new UserFilterRequest();
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAllUsersWithFilter(eq(filter.getStatus()), eq(filter.getRoleName()),
                eq(filter.getKeyword()), any())).thenReturn(page);
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(new UserProfileResponse());

        PaginationResponse<UserProfileResponse> result = userService.getUsers(filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("changeUserStatus: khoá tài khoản -> lưu trạng thái LOCKED và gửi đúng thông báo")
    void changeUserStatus_ToLocked_NotifiesUser() {
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));

        userService.changeUserStatus(sampleUserId.toString(), UserStatus.LOCKED);

        assertThat(sampleUser.getStatus()).isEqualTo(UserStatus.LOCKED);
        verify(notificationService).notify(
                eq(sampleUserId), any(), any(), eq(sampleUserId), eq("/profile"), eq("bị khóa"));
    }

    @Test
    @DisplayName("changeUserStatus: kích hoạt lại tài khoản -> lưu trạng thái ACTIVE và gửi đúng thông báo")
    void changeUserStatus_ToActive_NotifiesUser() {
        sampleUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));

        userService.changeUserStatus(sampleUserId.toString(), UserStatus.ACTIVE);

        assertThat(sampleUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(notificationService).notify(
                eq(sampleUserId), any(), any(), eq(sampleUserId), eq("/profile"), eq("được kích hoạt lại"));
    }

    @Test
    @DisplayName("changeUserStatus: userId không tồn tại -> ném AppException USER_NOT_FOUND, không thông báo")
    void changeUserStatus_UserNotFound_ThrowsException() {
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserStatus(randomId.toString(), UserStatus.LOCKED))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verify(notificationService, never()).notify(any(UUID.class), any(), any(), any(), any(), any());
    }
}
