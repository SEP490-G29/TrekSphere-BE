package com.sep.treksphere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "application.payment")
public class PaymentWorkflowProperties {
    private Duration holdDuration = Duration.ofMinutes(15);
    private Duration vendorConfirmationDuration = Duration.ofHours(24);
    private Duration checkoutLinkDuration = Duration.ofMinutes(15);
    private String returnUrl;
    private String cancelUrl;
    private String webhookBaseUrl;
    private String credentialEncryptionKey;

    public Duration getHoldDuration() { return holdDuration; }
    public void setHoldDuration(Duration holdDuration) { this.holdDuration = holdDuration; }
    public Duration getVendorConfirmationDuration() { return vendorConfirmationDuration; }
    public void setVendorConfirmationDuration(Duration vendorConfirmationDuration) { this.vendorConfirmationDuration = vendorConfirmationDuration; }
    public Duration getCheckoutLinkDuration() { return checkoutLinkDuration; }
    public void setCheckoutLinkDuration(Duration checkoutLinkDuration) { this.checkoutLinkDuration = checkoutLinkDuration; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    public String getWebhookBaseUrl() { return webhookBaseUrl; }
    public void setWebhookBaseUrl(String webhookBaseUrl) { this.webhookBaseUrl = webhookBaseUrl; }
    public String getCredentialEncryptionKey() { return credentialEncryptionKey; }
    public void setCredentialEncryptionKey(String credentialEncryptionKey) { this.credentialEncryptionKey = credentialEncryptionKey; }
}
