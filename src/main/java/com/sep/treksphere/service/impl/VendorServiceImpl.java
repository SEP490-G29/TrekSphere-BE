package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.VendorRepository;
import com.sep.treksphere.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
}
