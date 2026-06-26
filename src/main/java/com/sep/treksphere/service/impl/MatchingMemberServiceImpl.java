package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.MatchingMemberRepository;
import com.sep.treksphere.service.MatchingMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingMemberServiceImpl implements MatchingMemberService {

    private final MatchingMemberRepository matchingMemberRepository;
}
