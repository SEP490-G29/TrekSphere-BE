package com.sep.treksphere.tour.service;

import com.sep.treksphere.tour.repository.TourImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourImageService {

    private final TourImageRepository tourImageRepository;
}
