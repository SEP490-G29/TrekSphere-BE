package com.sep.treksphere.user;

import com.sep.treksphere.tour.DifficultyLevel;
import com.sep.treksphere.user.Gender;
import com.sep.treksphere.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.sep.treksphere.tour.DifficultyLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatarUrl;
    private UserStatus status;
    private Boolean emailVerified;
    private String bio;
    private ExperienceLevel experienceLevel;
    private DifficultyLevel preferredDifficulty;
    private List<String> preferredAreas;
    private List<String> skills;
    private Short trustScore;
    private Integer trustReviewCount;
    private List<String> roles;
    private String bio;
    private ExperienceLevel experienceLevel;
    private DifficultyLevel preferredDifficulty;
    private List<String> preferredAreas;
    private List<String> skills;
}
