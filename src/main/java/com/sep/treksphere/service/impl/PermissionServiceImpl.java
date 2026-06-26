package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.PermissionRepository;
import com.sep.treksphere.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
}
