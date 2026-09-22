package com.sep.treksphere.tour.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourImageService {

    private final TourImageRepository tourImageRepository;
}
