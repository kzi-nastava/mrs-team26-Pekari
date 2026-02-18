package com.pekara.repository;

import com.pekara.config.TestConfig;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for DriverStateRepository - ride ordering (find online drivers, lock for update).
 */
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@Transactional
public class DriverStateRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DriverStateRepository driverStateRepository;

    private Driver driver1;
    private Driver driver2;

    @BeforeMethod
    public void setUp() {
        entityManager.clear();

        driver1 = Driver.builder()
                .email("driver1@test.com")
                .username("driver1")
                .password("password")
                .firstName("Bob")
                .lastName("Driver")
                .phoneNumber("+381641111111")
                .address("Address 1")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SEDAN")
                .licensePlate("NS-111-AB")
                .build();

        driver2 = Driver.builder()
                .email("driver2@test.com")
                .username("driver2")
                .password("password")
                .firstName("Alice")
                .lastName("Driver")
                .phoneNumber("+381642222222")
                .address("Address 2")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SUV")
                .licensePlate("NS-222-CD")
                .build();

        entityManager.persist(driver1);
        entityManager.persist(driver2);
        entityManager.flush();
    }

    @Test(description = "Should return empty list when no driver states exist")
    public void findAllOnlineDrivers_NoStates_ReturnsEmpty() {
        List<DriverState> result = driverStateRepository.findAllOnlineDrivers();
        assertThat(result).isEmpty();
    }

    @Test(description = "Should return only online driver states")
    public void findAllOnlineDrivers_OnlyOnlineReturned() {
        DriverState state1 = DriverState.builder()
                .id(driver1.getId())
                .driver(driver1)
                .online(true)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        DriverState state2 = DriverState.builder()
                .id(driver2.getId())
                .driver(driver2)
                .online(false)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persist(state1);
        entityManager.persist(state2);
        entityManager.flush();

        List<DriverState> result = driverStateRepository.findAllOnlineDrivers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDriver().getId()).isEqualTo(driver1.getId());
        assertThat(result.get(0).getOnline()).isTrue();
    }

    @Test(description = "Should return all online driver states")
    public void findAllOnlineDrivers_MultipleOnline_ReturnsAll() {
        DriverState state1 = DriverState.builder()
                .id(driver1.getId())
                .driver(driver1)
                .online(true)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        DriverState state2 = DriverState.builder()
                .id(driver2.getId())
                .driver(driver2)
                .online(true)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persist(state1);
        entityManager.persist(state2);
        entityManager.flush();

        List<DriverState> result = driverStateRepository.findAllOnlineDrivers();

        assertThat(result).hasSize(2);
    }

    @Test(description = "Should return correct page of online drivers")
    public void findOnlineDrivers_WithPageable_ReturnsPage() {
        DriverState state1 = DriverState.builder()
                .id(driver1.getId())
                .driver(driver1)
                .online(true)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        DriverState state2 = DriverState.builder()
                .id(driver2.getId())
                .driver(driver2)
                .online(true)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persist(state1);
        entityManager.persist(state2);
        entityManager.flush();

        var page0 = driverStateRepository.findOnlineDrivers(PageRequest.of(0, 1));
        assertThat(page0.getContent()).hasSize(1);
        assertThat(page0.getTotalElements()).isEqualTo(2);

        var page1 = driverStateRepository.findOnlineDrivers(PageRequest.of(1, 1));
        assertThat(page1.getContent()).hasSize(1);
    }

    @Test(description = "Should return driver state by driver id for update")
    public void findByDriverIdForUpdate_ExistingState_ReturnsState() {
        DriverState state = DriverState.builder()
                .id(driver1.getId())
                .driver(driver1)
                .online(true)
                .busy(false)
                .updatedAt(LocalDateTime.now())
                .build();
        entityManager.persist(state);
        entityManager.flush();

        var result = driverStateRepository.findByDriverIdForUpdate(driver1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(driver1.getId());
        assertThat(result.get().getDriver().getId()).isEqualTo(driver1.getId());
    }

    @Test(description = "Should return empty when driver state does not exist")
    public void findByDriverIdForUpdate_NonExistent_ReturnsEmpty() {
        var result = driverStateRepository.findByDriverIdForUpdate(99999L);
        assertThat(result).isEmpty();
    }
}
