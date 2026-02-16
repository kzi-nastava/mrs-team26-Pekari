package com.pekara.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.pekara.model")
@EnableJpaRepositories(basePackages = "com.pekara.repository")
public class TestConfig {
}
