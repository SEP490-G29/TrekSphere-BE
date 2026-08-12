package com.sep.treksphere.repository;

import com.sep.treksphere.entity.PaymentWebhookEvent;
import com.sep.treksphere.enums.booking.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {
    boolean existsByProviderAndGatewayEventKey(PaymentProvider provider, String gatewayEventKey);
}
