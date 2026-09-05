package com.sep.treksphere.matching.service;

import com.sep.treksphere.matching.repository.MatchingMemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchingMemberService {

    private final MatchingMemberRepository matchingMemberRepository;
}
