package com.sep.treksphere.config;

import com.sep.treksphere.security.CustomUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    private static final String SYSTEM_AUDITOR = "SYSTEM";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of(SYSTEM_AUDITOR);
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof CustomUserDetails userDetails
                    && userDetails.getUser() != null
                    && userDetails.getUser().getUserId() != null) {
                return Optional.of(userDetails.getUser().getUserId().toString());
            }

            return Optional.ofNullable(authentication.getName()).or(() -> Optional.of(SYSTEM_AUDITOR));
        };
    }
}