package com.example.usertool.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateAndValidateToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider("test-secret-key-for-jwt-token-provider-123456", 60000L);

        String token = jwtTokenProvider.generateToken("admin", "ADMIN");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo("ADMIN");
    }
}
