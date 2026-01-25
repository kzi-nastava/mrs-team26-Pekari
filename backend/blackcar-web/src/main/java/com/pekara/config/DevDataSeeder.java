package com.pekara.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * NOTE: blackcar-web depends on blackcar-core at runtime scope only.
 * The actual seeding implementation lives in blackcar-core so it can access entities/repositories.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.dev", name = "seed", havingValue = "true")
public class DevDataSeeder {
    public DevDataSeeder() {
        log.info("Dev seeding enabled (implementation provided by blackcar-core)");
    }
}
