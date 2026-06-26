package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.RefreshTokenRepository;
import com.sep.treksphere.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
}
