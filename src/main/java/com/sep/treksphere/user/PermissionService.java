package com.sep.treksphere.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permission -> PermissionResponse.builder()
                        .permissionId(permission.getPermissionId())
                        .resource(permission.getResource())
                        .action(permission.getAction())
                        .description(permission.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
