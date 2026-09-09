package com.sep.treksphere.matching.service.impl;

import com.sep.treksphere.matching.repository.MatchingMemberRepository;
import com.sep.treksphere.matching.service.MatchingMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingMemberServiceImpl implements MatchingMemberService {

    private final MatchingMemberRepository matchingMemberRepository;
}
