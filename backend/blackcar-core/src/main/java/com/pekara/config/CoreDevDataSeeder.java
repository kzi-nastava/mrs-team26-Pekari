package com.pekara.config;

import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seeds development data when app.dev.seed=true.
 * Add to application-dev.properties: app.dev.seed=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.dev", name = "seed", havingValue = "true")
public class CoreDevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final DriverStateRepository driverStateRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== Starting dev data seeding ===");
        seedPassenger();
        seedDriverWithState();
        log.info("=== Dev data seeding complete ===");
    }

    private void seedPassenger() {
        String email = "passenger@test.com";

        User passenger = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new passenger user");
                    return User.builder()
                            .email(email)
                            .username("passenger_test")
                            .firstName("Test")
                            .lastName("Passenger")
                            .phoneNumber("+38160000001")
                            .address("Novi Sad")
                            .role(UserRole.PASSENGER)
                            .isActive(true)
                            .totalRides(0)
                            .build();
                });

        passenger.setPassword(passwordEncoder.encode("Pass1234"));
        passenger.setIsActive(true);
        passenger.setRole(UserRole.PASSENGER);

        userRepository.save(passenger);
        log.info("Seeded/updated dev passenger: {} / Pass1234", email);
    }

    private void seedDriverWithState() {
        String email = "driver@test.com";

        // Find or create driver
        Driver driver = driverRepository.findByEmail(email).orElse(null);

        if (driver == null) {
            log.info("Creating new driver user");
            driver = new Driver();
            driver.setEmail(email);
            driver.setUsername("driver_test");
            driver.setFirstName("Test");
            driver.setLastName("Driver");
            driver.setPhoneNumber("+38160000002");
            driver.setAddress("Novi Sad");
            driver.setRole(UserRole.DRIVER);
            driver.setIsActive(true);
            driver.setTotalRides(0);
            driver.setLicenseNumber("TEST-LIC-001");
            driver.setLicenseExpiry("2028-12");
            driver.setVehicleRegistration("NS-TEST-01");
        }

        driver.setPassword(passwordEncoder.encode("Driver1234"));
        driver.setIsActive(true);
        driver.setRole(UserRole.DRIVER);
        driver = driverRepository.saveAndFlush(driver);

        log.info("Seeded/updated dev driver: {} / Driver1234", email);

        // Create or update driver state - use the saved driver directly
        final Driver savedDriver = driver;
        DriverState state = driverStateRepository.findById(driver.getId())
                .orElseGet(() -> {
                    log.info("Creating new driver state");
                    DriverState newState = new DriverState();
                    newState.setDriver(savedDriver);
                    newState.setOnline(false);
                    newState.setBusy(false);
                    return newState;
                });

        state.setOnline(true);
        state.setBusy(false);
        state.setLatitude(45.2671);
        state.setLongitude(19.8335);
        state.setUpdatedAt(LocalDateTime.now());

        driverStateRepository.save(state);
        log.info("Seeded dev driver state: online=true at (45.2671, 19.8335)");
    }
}
