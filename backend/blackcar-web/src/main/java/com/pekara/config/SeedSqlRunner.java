package com.pekara.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Runs seed-data.sql on startup when app.dev.seed-sql=true.
 * Use this to populate the database from the backend instead of Neon SQL Editor.
 * Set app.dev.seed=false when using seed-sql to avoid duplicate Java seeding.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.dev", name = "seed-sql", havingValue = "true")
public class SeedSqlRunner implements ApplicationRunner {

    private final DataSource dataSource;

    public SeedSqlRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Running seed-data.sql from backend ===");
        Resource script = findSeedScript();
        if (script == null || !script.exists()) {
            log.error("seed-data.sql not found. Tried classpath:seed-data.sql and file:seed-data.sql (from backend/)");
            return;
        }
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
            populator.setSeparator(";");
            populator.setCommentPrefix("--");
            populator.execute(dataSource);
            log.info("=== seed-data.sql completed successfully ===");
        } catch (Exception e) {
            log.error("Failed to run seed-data.sql", e);
            throw new RuntimeException("Seed SQL failed", e);
        }
    }

    private Resource findSeedScript() {
        Resource classpath = new ClassPathResource("seed-data.sql");
        if (classpath.exists()) {
            return classpath;
        }
        Resource file = new FileSystemResource("seed-data.sql");
        if (file.exists()) {
            return file;
        }
        return null;
    }
}
