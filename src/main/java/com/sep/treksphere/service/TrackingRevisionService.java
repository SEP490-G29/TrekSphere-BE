package com.sep.treksphere.service;

import java.util.UUID;

public interface TrackingRevisionService {
    long increment(UUID sessionId);
}
