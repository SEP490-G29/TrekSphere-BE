package com.sep.treksphere.user;

import com.sep.treksphere.tour.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHikingSummaryResponse {
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private Gender gender;
    private String bio;
    private ExperienceLevel experienceLevel;
    private DifficultyLevel preferredDifficulty;
    private List<String> preferredAreas;
    private List<String> skills;
    private Short trustScore;
    private Integer trustReviewCount;
}
