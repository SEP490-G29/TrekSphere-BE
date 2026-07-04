package com.sep.treksphere.service;

import com.sep.treksphere.dto.response.TourDetailResponse;
import com.sep.treksphere.dto.response.TourSummaryResponse;
import com.sep.treksphere.enums.tour.DifficultyLevel;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TourService {
    Page<TourSummaryResponse> getTours(
            String keyword,
            String location,
            DifficultyLevel difficulty,
            int page,
            int size,
            String sortBy,
            String sortDir
    );
    TourDetailResponse getTourById(UUID tourId);
}

