package com.pekara.config;

import com.pekara.model.*;
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
import java.util.List;

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
        log.info("=== Starting bulk dev data seeding ===");

        // 1. Seed Admins
        seedUser("admin@test.com", "admin_main", "Super", "Admin", UserRole.ADMIN, "Admin1234");
        seedUser("support@test.com", "admin_support", "Support", "Tech", UserRole.ADMIN, "Admin1234");

        // 2. Seed Passengers
        seedUser("passenger1@test.com", "p_alice", "Alice", "Smith", UserRole.PASSENGER, "Pass1234");
        seedUser("passenger2@test.com", "p_bob", "Bob", "Jones", UserRole.PASSENGER, "Pass1234");
        seedUser("passenger3@test.com", "p_charlie", "Charlie", "Brown", UserRole.PASSENGER, "Pass1234");
        seedUser("passenger4@test.com", "p_dana", "Dana", "White", UserRole.PASSENGER, "Pass1234");

        // 3. Seed Drivers with different vehicle types and locations
        seedDriverWithState("driver_std@test.com", "d_john", "John", "Doe",
                "STANDARD", "NS-111-AA", 45.2671, 19.8335); // Center

        seedDriverWithState("driver_premium@test.com", "d_jane", "Jane", "Doe",
                "PREMIUM", "NS-222-BB", 45.2464, 19.8517); // Liman

        seedDriverWithState("driver_eco@test.com", "d_mike", "Mike", "Ross",
                "ECO", "NS-333-CC", 45.2600, 19.8000); // Novo Naselje

        seedDriverWithState("driver_van@test.com", "d_sarah", "Sarah", "Connor",
                "VAN", "NS-444-DD", 45.2396, 19.8227); // Petrovaradin

        log.info("=== Bulk dev data seeding complete ===");
    }

    /**
     * Helper to create or update generic Users (Admin/Passenger)
     */
    private void seedUser(String email, String username, String fName, String lName, UserRole role, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder().email(email).build());

        user.setUsername(username);
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setPhoneNumber("+38160" + (int)(Math.random() * 9000000 + 1000000));
        user.setAddress("Novi Sad");
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setIsActive(true);
        user.setTotalRides(0);

        userRepository.save(user);
        log.info("Seeded {}: {}", role, email);
    }

    /**
     * Helper to create or update Drivers and their associated real-time State
     */
    private void seedDriverWithState(String email, String username, String fName, String lName,
                                     String vType, String vReg, double lat, double lon) {

        Driver driver = driverRepository.findByEmail(email).orElseGet(() -> {
            Driver d = new Driver();
            d.setEmail(email);
            return d;
        });

        // Set basic info
        driver.setUsername(username);
        driver.setFirstName(fName);
        driver.setLastName(lName);
        driver.setPhoneNumber("+38161" + (int)(Math.random() * 9000000 + 1000000));
        driver.setAddress("Novi Sad");
        driver.setRole(UserRole.DRIVER);
        driver.setPassword(passwordEncoder.encode("Driver1234"));
        driver.setIsActive(true);
        
        // Driver specific info
        driver.setVehicleType(vType);
        driver.setVehicleRegistration(vReg);
        driver.setLicenseNumber("LIC-" + vReg);
        driver.setLicenseExpiry("2030-01");

        Driver savedDriver = driverRepository.saveAndFlush(driver);

        // Seed State
        DriverState state = driverStateRepository.findById(savedDriver.getId())
                .orElseGet(() -> {
                    DriverState s = new DriverState();
                    s.setDriver(savedDriver);
                    return s;
                });

        state.setOnline(true);
        state.setBusy(false);
        state.setLatitude(lat);
        state.setLongitude(lon);
        state.setUpdatedAt(LocalDateTime.now());

        driverStateRepository.save(state);
        log.info("Seeded Driver & State: {} ({}) at [{}, {}]", email, vType, lat, lon);
    }
}