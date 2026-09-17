package com.sep.treksphere.user;

import com.sep.treksphere.common.constant.MessageConstant;
import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.common.exception.AppException;
import com.sep.treksphere.common.exception.ErrorCode;
import com.sep.treksphere.common.util.PhoneNumberUtil;
import com.sep.treksphere.file.FileService;
import com.sep.treksphere.notification.NotificationEventType;
import com.sep.treksphere.notification.NotificationService;
import com.sep.treksphere.notification.ReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final FileService fileService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        return userMapper.toUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public PublicHikingSummaryResponse getPublicHikingSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        return userMapper.toPublicHikingSummaryResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request, MultipartFile avatar) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isEmpty()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, MessageConstant.FULL_NAME_REQUIRED);
            }
            user.setFullName(fullName);
        }
        if (request.getPhone() != null) {
            String rawPhone = request.getPhone().trim();
            if (rawPhone.isEmpty()) {
                user.setPhone(null);
            } else {
                String normalizedPhone = PhoneNumberUtil.normalize(rawPhone);
                String currentNormalized = PhoneNumberUtil.normalize(user.getPhone());
                if (!Objects.equals(normalizedPhone, currentNormalized)) {
                    List<String> variants = PhoneNumberUtil.getPhoneVariants(normalizedPhone);
                    boolean phoneExists = userRepository.existsByPhoneInAndUserIdNot(variants, user.getUserId());
                    if (phoneExists) {
                        throw new AppException(ErrorCode.PHONE_EXISTED);
                    }
                }
                user.setPhone(normalizedPhone);
            }
        }
        if (request.getDateOfBirth() != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            if (request.getDateOfBirth().isAfter(today.minusYears(18))) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, MessageConstant.INVALID_DOB);
            }
            if (request.getDateOfBirth().isBefore(today.minusYears(100))) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Ngày sinh không hợp lệ (không được vượt quá 100 tuổi)");
            }
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().isBlank() ? null : request.getBio().trim());
        }
        if (request.getExperienceLevel() != null) {
            user.setExperienceLevel(request.getExperienceLevel());
        }
        if (request.getPreferredDifficulty() != null) {
            user.setPreferredDifficulty(request.getPreferredDifficulty());
        }
        if (request.getPreferredAreas() != null) {
            user.setPreferredAreas(normalizeTags(request.getPreferredAreas(), 100));
        }
        if (request.getSkills() != null) {
            user.setSkills(normalizeTags(request.getSkills(), 100));
        }

        validatePreferenceCompatibility(user);

        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = fileService.uploadFile(avatar, "avatars");
            user.setAvatarUrl(avatarUrl);
        }

        userRepository.save(user);

        return userMapper.toUserProfileResponse(user);
    }

    private List<String> normalizeTags(List<String> values, int maxLength) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.length() > maxLength ? value.substring(0, maxLength) : value)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void validatePreferenceCompatibility(User user) {
        if (user.getExperienceLevel() != null
                && user.getPreferredDifficulty() != null
                && user.getPreferredDifficulty().ordinal() > user.getExperienceLevel().ordinal()) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Độ khó yêu thích không được vượt quá cấp độ kinh nghiệm.");
        }
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserProfileResponse> getUsers(UserFilterRequest request) {
        Page<User> usersPage = userRepository.findAllUsersWithFilter(
                request.getStatus(),
                request.getRoleName(),
                request.getKeyword(),
                request.getPageable()
        );

        List<UserProfileResponse> responses = usersPage.getContent().stream()
                .map(userMapper::toUserProfileResponse)
                .toList();

        return PaginationResponse.<UserProfileResponse>builder()
                .content(responses)
                .pageNumber(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .last(usersPage.isLast())
                .build();
    }

    @Transactional
    public void changeUserStatus(String userId, UserStatus status) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(status);
        userRepository.save(user);

        notificationService.notify(
                user.getUserId(),
                NotificationEventType.USER_STATUS_CHANGED,
                ReferenceType.USER, user.getUserId(),
                "/profile",
                switch (status) {
                    case ACTIVE -> "được kích hoạt lại";
                    case LOCKED -> "bị khóa";
                    case DEACTIVATED -> "bị vô hiệu hoá";
                });
    }
}
