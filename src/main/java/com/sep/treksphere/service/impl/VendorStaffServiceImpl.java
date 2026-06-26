package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.VendorStaffRepository;
import com.sep.treksphere.service.VendorStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorStaffServiceImpl implements VendorStaffService {

    private final VendorStaffRepository vendorStaffRepository;
}
