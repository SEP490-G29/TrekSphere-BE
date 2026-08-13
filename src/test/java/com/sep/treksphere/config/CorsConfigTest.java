package com.sep.treksphere.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    @Test
    void bookingPreflightAllowsIdempotencyKeyHeader() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "frontendUrl", "https://treksphere.io.vn");
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS", "/api/v1/bookings");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertNotNull(cors);
        assertTrue(cors.getAllowedHeaders().stream()
                .anyMatch(header -> header.equalsIgnoreCase("Idempotency-Key")));
    }
}
