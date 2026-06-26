package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.MatchingGroupRepository;
import com.sep.treksphere.service.MatchingGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingGroupServiceImpl implements MatchingGroupService {

    private final MatchingGroupRepository matchingGroupRepository;
}
