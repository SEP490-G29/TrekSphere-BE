package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.BlogRepository;
import com.sep.treksphere.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
}
