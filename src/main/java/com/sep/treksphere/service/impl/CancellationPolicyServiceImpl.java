package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.CancellationPolicyRepository;
import com.sep.treksphere.service.CancellationPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancellationPolicyServiceImpl implements CancellationPolicyService {

    private final CancellationPolicyRepository cancellationPolicyRepository;
}
