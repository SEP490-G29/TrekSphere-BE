package com.sep.treksphere.user;

import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.tour.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
