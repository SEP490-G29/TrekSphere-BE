package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.RoleRepository;
import com.sep.treksphere.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
}
