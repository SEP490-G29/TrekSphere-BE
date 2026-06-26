package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.ReviewRepository;
import com.sep.treksphere.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
}
