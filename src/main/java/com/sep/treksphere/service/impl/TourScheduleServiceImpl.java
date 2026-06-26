package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.TourScheduleRepository;
import com.sep.treksphere.service.TourScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourScheduleServiceImpl implements TourScheduleService {

    private final TourScheduleRepository tourScheduleRepository;
}
