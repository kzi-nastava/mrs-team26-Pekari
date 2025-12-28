package com.pekara.config;

import com.pekara.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Bean
    public JwtService jwtService() {
        try {
            Class<?> implClass = Class.forName("com.pekara.service.JwtServiceImpl");
            return (JwtService) implClass
                .getConstructor(String.class, Long.class)
                .newInstance(jwtSecret, jwtExpiration);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JwtService", e);
        }
    }
}
