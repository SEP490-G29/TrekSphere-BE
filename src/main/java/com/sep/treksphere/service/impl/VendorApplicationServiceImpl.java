package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.VendorApplicationRepository;
import com.sep.treksphere.service.VendorApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorApplicationServiceImpl implements VendorApplicationService {

    private final VendorApplicationRepository vendorApplicationRepository;
}
