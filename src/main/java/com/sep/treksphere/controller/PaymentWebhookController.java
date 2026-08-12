package com.sep.treksphere.controller;

import com.sep.treksphere.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

@RestController
@RequestMapping("/api/v1/payment-webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/payos/{channelId}")
    public ResponseEntity<String> handlePayOsWebhook(
            @PathVariable String channelId,
            @RequestBody Webhook webhook) {
        paymentService.handlePayOsWebhook(channelId, webhook);
        return ResponseEntity.ok("OK");
    }
}
