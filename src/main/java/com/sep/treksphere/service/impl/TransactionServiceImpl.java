package com.sep.treksphere.service.impl;

import com.sep.treksphere.repository.TransactionRepository;
import com.sep.treksphere.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
}
