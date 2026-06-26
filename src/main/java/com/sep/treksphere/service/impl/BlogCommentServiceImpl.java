package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.BlogCommentRepository;
import com.sep.treksphere.service.BlogCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlogCommentServiceImpl implements BlogCommentService {

    private final BlogCommentRepository blogCommentRepository;
}
