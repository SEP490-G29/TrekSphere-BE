package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.SystemPolicyRepository;
import com.sep.treksphere.service.SystemPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemPolicyServiceImpl implements SystemPolicyService {

    private final SystemPolicyRepository systemPolicyRepository;
}
