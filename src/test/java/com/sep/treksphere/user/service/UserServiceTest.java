package com.sep.treksphere.user.service;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.file.service.FileService;
import com.sep.treksphere.tour.enums.DifficultyLevel;
import com.sep.treksphere.user.dto.request.UpdateProfileRequest;
import com.sep.treksphere.user.dto.response.PublicHikingSummaryResponse;
import com.sep.treksphere.user.dto.response.UserProfileResponse;
import com.sep.treksphere.user.entity.User;
import com.sep.treksphere.user.enums.ExperienceLevel;
import com.sep.treksphere.user.enums.Gender;
import com.sep.treksphere.user.mapper.UserMapper;
import com.sep.treksphere.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileService fileService;

    @Mock
    private UserMapper userMapper;

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
    @DisplayName("Cập nhật số điện thoại thành công khi số điện thoại chưa tồn tại")
    void updateProfile_WithNewPhone_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("0912345678");

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByPhoneInAndUserIdNot(List.of("0912345678", "+84912345678", "84912345678"), sampleUserId)).thenReturn(false);
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(new UserProfileResponse());

        userService.updateProfile("trekker@example.com", request, null);

        assertThat(sampleUser.getPhone()).isEqualTo("0912345678");
    }

    @Test
    @DisplayName("Cập nhật số điện thoại với định dạng +84 được chuẩn hóa về 0 và lưu thành công")
    void updateProfile_WithPlus84Phone_NormalizesTo0_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("+84837319199");

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByPhoneInAndUserIdNot(List.of("0837319199", "+84837319199", "84837319199"), sampleUserId)).thenReturn(false);
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(new UserProfileResponse());

        userService.updateProfile("trekker@example.com", request, null);

        assertThat(sampleUser.getPhone()).isEqualTo("0837319199");
    }

    @Test
    @DisplayName("Cập nhật số điện thoại thất bại khi số điện thoại đã tồn tại ở tài khoản khác -> PHONE_EXISTED")
    void updateProfile_WithDuplicatePhone_ThrowsPhoneExisted() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("+84837319199");

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByPhoneInAndUserIdNot(List.of("0837319199", "+84837319199", "84837319199"), sampleUserId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile("trekker@example.com", request, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_EXISTED);
    }

    @Test
    @DisplayName("Cùng một user đổi định dạng số điện thoại giữa +84 và 0 không bị báo lỗi trùng")
    void updateProfile_SameUserChangingFormat_Success() {
        sampleUser.setPhone("0987654321");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("+84987654321");

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(new UserProfileResponse());

        userService.updateProfile("trekker@example.com", request, null);

        assertThat(sampleUser.getPhone()).isEqualTo("0987654321");
    }

    @Test
    @DisplayName("Cập nhật ngày sinh chưa đủ 18 tuổi -> Bắn AppException VALIDATION_ERROR")
    void updateProfile_Underage_ThrowsValidationError() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDateOfBirth(java.time.LocalDate.now().minusYears(17));

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> userService.updateProfile("trekker@example.com", request, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Cập nhật ngày sinh đủ 18 tuổi trở lên -> Thành công")
    void updateProfile_ValidAge_Success() {
        java.time.LocalDate validDob = java.time.LocalDate.now().minusYears(20);
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDateOfBirth(validDob);

        when(userRepository.findByEmail("trekker@example.com")).thenReturn(Optional.of(sampleUser));
        when(userMapper.toUserProfileResponse(sampleUser)).thenReturn(new UserProfileResponse());

        userService.updateProfile("trekker@example.com", request, null);

        assertThat(sampleUser.getDateOfBirth()).isEqualTo(validDob);
    }
}
