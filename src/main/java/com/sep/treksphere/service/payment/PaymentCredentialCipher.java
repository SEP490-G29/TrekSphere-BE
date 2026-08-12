package com.sep.treksphere.service.payment;

import com.sep.treksphere.config.PaymentWorkflowProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PaymentCredentialCipher {

    private static final String VERSION = "v1";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PaymentWorkflowProperties properties;

    public PaymentCredentialCipher(PaymentWorkflowProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getEncoder().encodeToString(nonce) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể mã hóa thông tin payOS", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            String[] parts = encryptedValue.split(":", 3);
            if (parts.length != 3 || !VERSION.equals(parts[0])) {
                throw new IllegalStateException("Định dạng credential payOS không hợp lệ");
            }
            byte[] nonce = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Không thể giải mã thông tin payOS", exception);
        }
    }

    private SecretKeySpec encryptionKey() {
        String configured = properties.getCredentialEncryptionKey();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("PAYMENT_CREDENTIAL_ENCRYPTION_KEY chưa được cấu hình");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "PAYMENT_CREDENTIAL_ENCRYPTION_KEY phải là Base64 của khóa 256-bit", exception);
        }
        if (key.length != 32) {
            throw new IllegalStateException(
                    "PAYMENT_CREDENTIAL_ENCRYPTION_KEY phải giải mã thành đúng 32 byte");
        }
        return new SecretKeySpec(key, "AES");
    }
}
