package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.TourImageRepository;
import com.sep.treksphere.service.TourImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourImageServiceImpl implements TourImageService {

    private final TourImageRepository tourImageRepository;
}
