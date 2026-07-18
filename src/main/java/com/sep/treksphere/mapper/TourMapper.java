package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.request.CreateTourRequest;
import com.sep.treksphere.dto.request.UpdateTourRequest;
import com.sep.treksphere.entity.Tour;
import org.springframework.stereotype.Component;

@Component
public class TourMapper {

    public Tour toTour(CreateTourRequest request) {
        if (request == null) {
            return null;
        }

        Tour tour = new Tour();
        tour.setTourName(request.getTourName());
        tour.setDescription(request.getDescription());
        tour.setDifficulty(request.getDifficulty());
        tour.setLocation(request.getLocation());
        tour.setDurationDays(request.getDurationDays());
        tour.setBasePrice(request.getBasePrice());
        tour.setMinCapacity(request.getMinCapacity() != null ? request.getMinCapacity() : 1);
        tour.setMaxCapacity(request.getMaxCapacity());
        tour.setTotalDistanceKm(request.getTotalDistanceKm());
        tour.setHighlights(request.getHighlights());
        tour.setIncludes(request.getIncludes());
        tour.setExcludes(request.getExcludes());
        tour.setCoverImageUrl(request.getCoverImageUrl());
        
        return tour;
    }

    public void updateTourFromRequest(UpdateTourRequest request, Tour tour) {
        if (request == null || tour == null) {
            return;
        }

        if (request.getTourName() != null) {
            tour.setTourName(request.getTourName());
        }
        if (request.getDescription() != null) {
            tour.setDescription(request.getDescription());
        }
        if (request.getDifficulty() != null) {
            tour.setDifficulty(request.getDifficulty());
        }
        if (request.getLocation() != null) {
            tour.setLocation(request.getLocation());
        }
        if (request.getDurationDays() != null) {
            tour.setDurationDays(request.getDurationDays());
        }
        if (request.getBasePrice() != null) {
            tour.setBasePrice(request.getBasePrice());
        }
        if (request.getMinCapacity() != null) {
            tour.setMinCapacity(request.getMinCapacity());
        }
        if (request.getMaxCapacity() != null) {
            tour.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getTotalDistanceKm() != null) {
            tour.setTotalDistanceKm(request.getTotalDistanceKm());
        }
        if (request.getHighlights() != null) {
            tour.setHighlights(request.getHighlights());
        }
        if (request.getIncludes() != null) {
            tour.setIncludes(request.getIncludes());
        }
        if (request.getExcludes() != null) {
            tour.setExcludes(request.getExcludes());
        }
        if (request.getCoverImageUrl() != null) {
            tour.setCoverImageUrl(request.getCoverImageUrl());
        }
    }
}
