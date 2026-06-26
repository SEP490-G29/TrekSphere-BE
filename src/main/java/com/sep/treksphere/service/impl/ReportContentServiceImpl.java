package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.ReportContentRepository;
import com.sep.treksphere.service.ReportContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportContentServiceImpl implements ReportContentService {

    private final ReportContentRepository reportContentRepository;
}
