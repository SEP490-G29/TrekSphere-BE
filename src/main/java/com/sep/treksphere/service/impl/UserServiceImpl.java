package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.UserRepository;
import com.sep.treksphere.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
}
