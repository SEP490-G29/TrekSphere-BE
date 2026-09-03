package com.sep.treksphere.matching.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingMemberService {

    private final MatchingMemberRepository matchingMemberRepository;
}
