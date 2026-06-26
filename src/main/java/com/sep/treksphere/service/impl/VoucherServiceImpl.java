package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.VoucherRepository;
import com.sep.treksphere.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
}
