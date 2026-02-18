package com.pekara.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.request.WebOrderRideRequest;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.Ride;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import com.pekara.service.JwtService;
import com.pekara.service.RideEstimationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RideController - Ride Ordering (POST /api/v1/rides/order).
 * Uses @MockBean for RideEstimationService so tests are deterministic and do not call external routing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class RideOrderControllerIntegrationTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DriverStateRepository driverStateRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RideEstimationService rideEstimationService;

    private User passenger;
    private Driver driver;
    private String passengerToken;
    private String driverToken;
    private String adminToken;
    private WebOrderRideRequest validRequest;

    @BeforeMethod
    public void setUp() {
        rideRepository.deleteAll();
        driverStateRepository.deleteAll();
        userRepository.deleteAll();

        passenger = User.builder()
                .email("passenger@test.com")
                .username("passenger")
                .password(passwordEncoder.encode("password"))
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+381641111111")
                .address("Test Address")
                .role(UserRole.PASSENGER)
                .isActive(true)
                .totalRides(0)
                .build();
        passenger = userRepository.save(passenger);

        driver = Driver.builder()
                .email("driver@test.com")
                .username("driver")
                .password(passwordEncoder.encode("password"))
                .firstName("Bob")
                .lastName("Driver")
                .phoneNumber("+381642222222")
                .address("Driver Address")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SEDAN")
                .licensePlate("NS-123-AB")
                .build();
        driver = userRepository.save(driver);

        DriverState driverState = DriverState.builder()
                .id(driver.getId())
                .driver(driver)
                .online(true)
                .busy(false)
                .latitude(45.25)
                .longitude(19.84)
                .updatedAt(LocalDateTime.now())
                .build();
        driverStateRepository.save(driverState);

        User admin = User.builder()
                .email("admin@test.com")
                .username("admin")
                .password(passwordEncoder.encode("password"))
                .firstName("Admin")
                .lastName("User")
                .phoneNumber("+381643333333")
                .address("Admin Address")
                .role(UserRole.ADMIN)
                .isActive(true)
                .totalRides(0)
                .build();
        userRepository.save(admin);

        passengerToken = jwtService.generateToken(passenger.getEmail(), passenger.getRole().toString());
        driverToken = jwtService.generateToken(driver.getEmail(), driver.getRole().toString());
        adminToken = jwtService.generateToken(admin.getEmail(), admin.getRole().toString());

        validRequest = new WebOrderRideRequest();
        validRequest.setPickup(WebLocationPoint.builder().address("Pickup").latitude(45.25).longitude(19.84).build());
        validRequest.setDropoff(WebLocationPoint.builder().address("Dropoff").latitude(45.27).longitude(19.85).build());
        validRequest.setVehicleType("SEDAN");
        validRequest.setBabyTransport(false);
        validRequest.setPetTransport(false);

        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(10.0, 20,
                        List.of(LocationPointDto.builder().address("A").latitude(45.25).longitude(19.84).build(),
                                LocationPointDto.builder().address("B").latitude(45.27).longitude(19.85).build())));
        when(rideEstimationService.calculatePrice(any(), anyDouble())).thenReturn(new BigDecimal("500.00"));
        when(rideEstimationService.roundKm(anyDouble())).thenAnswer(inv -> inv.getArgument(0));
        when(rideEstimationService.serializeRouteCoordinates(any())).thenReturn("[[45.25,19.84],[45.27,19.85]]");
    }

    private jakarta.servlet.http.Cookie createJwtCookie(String token) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    @Test(description = "Should return 201 and create ride when order is valid and passenger is authenticated")
    public void orderRide_ValidRequest_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").isNumber())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message", containsString("ordered successfully")))
                .andExpect(jsonPath("$.estimatedPrice").value(500))
                .andExpect(jsonPath("$.assignedDriverEmail").value("driver@test.com"));

        List<Ride> rides = rideRepository.findAll();
        assertThat(rides).hasSize(1);
        assertThat(rides.get(0).getCreator().getEmail()).isEqualTo(passenger.getEmail());
        assertThat(rides.get(0).getDriver().getEmail()).isEqualTo(driver.getEmail());
        assertThat(rides.get(0).getStatus()).isEqualTo(RideStatus.ACCEPTED);
    }

    @Test(description = "Should return 403 when no authentication provided")
    public void orderRide_NoAuth_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/rides/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @DataProvider(name = "unauthorizedRolesForOrder")
    public Object[][] unauthorizedRolesForOrder() {
        return new Object[][]{
                {"driver", "DRIVER"},
                {"admin", "ADMIN"}
        };
    }

    @Test(dataProvider = "unauthorizedRolesForOrder", description = "Should return 500 when unauthorized role tries to order ride")
    public void orderRide_WrongRole_Returns500(String roleName, String roleType) throws Exception {
        String token = "driver".equals(roleName) ? driverToken : adminToken;
        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test(description = "Should return 403 when creator is blocked")
    public void orderRide_BlockedUser_Returns403() throws Exception {
        passenger.setBlocked(true);
        passenger.setBlockedNote("Abuse");
        userRepository.save(passenger);

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_BLOCKED"))
                .andExpect(jsonPath("$.message", containsString("blocked")));
    }

    @Test(description = "Should return 409 when no active drivers")
    public void orderRide_NoActiveDrivers_Returns409() throws Exception {
        driverStateRepository.deleteAll();

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_DRIVERS_AVAILABLE"))
                .andExpect(jsonPath("$.message", containsString("no active drivers")));
    }

    @Test(description = "Should return 409 when creator has active ride")
    public void orderRide_ActiveRideConflict_Returns409() throws Exception {
        Ride activeRide = Ride.builder()
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.ACCEPTED)
                .vehicleType("SEDAN")
                .babyTransport(false)
                .petTransport(false)
                .estimatedPrice(new BigDecimal("300.00"))
                .distanceKm(5.0)
                .estimatedDurationMinutes(15)
                .routeCoordinates("[]")
                .build();
        activeRide.getPassengers().add(passenger);
        rideRepository.save(activeRide);

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_RIDE_CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("active ride")));
    }

    @Test(description = "Should return 400 when scheduled time is in the past")
    public void orderRide_ScheduledInPast_Returns400() throws Exception {
        validRequest.setScheduledAt(LocalDateTime.now().minusMinutes(10));

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_TIME"))
                .andExpect(jsonPath("$.message", containsString("future")));
    }

    @Test(description = "Should return 400 when scheduled more than 5 hours ahead")
    public void orderRide_ScheduledMoreThan5h_Returns400() throws Exception {
        validRequest.setScheduledAt(LocalDateTime.now().plusHours(6));

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_TIME"))
                .andExpect(jsonPath("$.message", containsString("5 hours")));
    }

    @Test(description = "Should return 400 when required fields are missing")
    public void orderRide_MissingPickup_Returns400() throws Exception {
        validRequest.setPickup(null);

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pickup").exists());
    }

    @Test(description = "Should return 400 when vehicle type is missing")
    public void orderRide_MissingVehicleType_Returns400() throws Exception {
        validRequest.setVehicleType(null);

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test(description = "Should return 201 with SCHEDULED when scheduled within 5 hours")
    public void orderRide_ScheduledWithin5h_Returns201Scheduled() throws Exception {
        validRequest.setScheduledAt(LocalDateTime.now().plusHours(2));

        mockMvc.perform(post("/api/v1/rides/order")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.message", containsString("scheduled")));
    }
}
