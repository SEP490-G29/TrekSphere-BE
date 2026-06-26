package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.TourRepository;
import com.sep.treksphere.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private final TourRepository tourRepository;
}
