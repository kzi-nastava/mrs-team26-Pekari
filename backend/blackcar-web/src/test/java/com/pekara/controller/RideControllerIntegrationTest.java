package com.pekara.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pekara.constant.RideStatus;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.request.WebStopRideEarlyRequest;
import com.pekara.model.Driver;
import com.pekara.model.Ride;
import com.pekara.model.RideStop;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import com.pekara.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RideController - Early Ride Stoppage endpoints
 * Tests the full HTTP request/response cycle with real database (H2)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class RideControllerIntegrationTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User passenger;
    private Driver driver;
    private Ride ride;
    private String passengerToken;
    private String driverToken;
    private String adminToken;

    @BeforeMethod
    public void setUp() {
        rideRepository.deleteAll();
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

        Set<User> passengers = new HashSet<>();
        passengers.add(passenger);

        ride = Ride.builder()
                .creator(passenger)
                .driver(driver)
                .passengers(passengers)
                .status(RideStatus.IN_PROGRESS)
                .vehicleType("SEDAN")
                .babyTransport(false)
                .petTransport(false)
                .estimatedPrice(new BigDecimal("500.00"))
                .distanceKm(5.0)
                .estimatedDurationMinutes(15)
                .routeCoordinates("[[45.2551,19.8451],[45.2671,19.8335]]")
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        RideStop pickup = RideStop.builder()
                .ride(ride)
                .sequenceIndex(0)
                .address("Pickup Location")
                .latitude(45.2551)
                .longitude(19.8451)
                .build();

        RideStop dropoff = RideStop.builder()
                .ride(ride)
                .sequenceIndex(1)
                .address("Dropoff Location")
                .latitude(45.2671)
                .longitude(19.8335)
                .build();

        ride.getStops().add(pickup);
        ride.getStops().add(dropoff);
        ride = rideRepository.save(ride);

        passengerToken = jwtService.generateToken(passenger.getEmail(), passenger.getRole().toString());
        driverToken = jwtService.generateToken(driver.getEmail(), driver.getRole().toString());
        adminToken = jwtService.generateToken(admin.getEmail(), admin.getRole().toString());
    }

    @DataProvider(name = "unauthorizedRolesForRequestStop")
    public Object[][] unauthorizedRolesForRequestStop() {
        return new Object[][]{
                {"driver", "DRIVER"},
                {"admin", "ADMIN"}
        };
    }

    @DataProvider(name = "unauthorizedRolesForStopRide")
    public Object[][] unauthorizedRolesForStopRide() {
        return new Object[][]{
                {"passenger", "PASSENGER"},
                {"admin", "ADMIN"}
        };
    }

    @DataProvider(name = "invalidAddressData")
    public Object[][] invalidAddressData() {
        return new Object[][]{
                {null, "missing address"},
                {"   ", "blank address"}
        };
    }

    private jakarta.servlet.http.Cookie createJwtCookie(String token) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }

    // Testing: POST /{rideId}/request-stop

    @Test(description = "Should successfully request stop as passenger - 200 OK")
    public void requestStop_Success() throws Exception {
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/request-stop")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stop request sent to driver."));

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(updatedRide.getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);
    }

    @Test(description = "Should return 403 when no authentication (no token, no cookie) provided")
    public void requestStop_NoAuth_403() throws Exception {
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/request-stop")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test(dataProvider = "unauthorizedRolesForRequestStop",
            description = "Should return 500 when unauthorized role tries to request stop")
    public void requestStop_UnauthorizedRole_AccessDenied(String roleName, String roleType) throws Exception {
        String token = roleName.equals("driver") ? driverToken : adminToken;

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/request-stop")
                        .cookie(createJwtCookie(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test(description = "Should return 400 when ride does not exist")
    public void requestStop_RideNotFound_400() throws Exception {
        mockMvc.perform(post("/api/v1/rides/99999/request-stop")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Ride not found")));
    }

    @Test(description = "Should return 400 when ride is not IN_PROGRESS")
    public void requestStop_InvalidStatus_400() throws Exception {
        // Change ride status to ACCEPTED
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/request-stop")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only in-progress rides can be stopped")));
    }

    @Test(description = "Should return 400 when user is not a passenger on the ride")
    public void requestStop_NotPassenger_400() throws Exception {
        User anotherPassenger = User.builder()
                .email("other@test.com")
                .username("other")
                .password(passwordEncoder.encode("password"))
                .firstName("Other")
                .lastName("User")
                .phoneNumber("+381644444444")
                .address("Other Address")
                .role(UserRole.PASSENGER)
                .isActive(true)
                .totalRides(0)
                .build();
        userRepository.save(anotherPassenger);
        String otherToken = jwtService.generateToken(anotherPassenger.getEmail(), anotherPassenger.getRole().toString());

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/request-stop")
                        .cookie(createJwtCookie(otherToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You are not a passenger on this ride")));
    }

    // Testing: POST /{rideId}/stop

    @Test(description = "Should successfully complete ride at original destination - 200 OK")
    public void stopRide_CompleteNormally_Success() throws Exception {
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ride completed successfully."));

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(updatedRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(updatedRide.getCompletedAt()).isNotNull();
    }

    @Test(description = "Should successfully stop ride early with new location - 200 OK")
    public void stopRide_EarlyWithNewLocation_Success() throws Exception {
        WebStopRideEarlyRequest request = WebStopRideEarlyRequest.builder()
                .stopLocation(WebLocationPoint.builder()
                        .address("New Stop Location")
                        .latitude(45.2600)
                        .longitude(19.8400)
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ride completed at new location."));

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(updatedRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(updatedRide.getCompletedAt()).isNotNull();

        RideStop lastStop = updatedRide.getStops().get(updatedRide.getStops().size() - 1);
        assertThat(lastStop.getAddress()).isEqualTo("New Stop Location");
        assertThat(lastStop.getLatitude()).isEqualTo(45.2600);
        assertThat(lastStop.getLongitude()).isEqualTo(19.8400);
    }

    @Test(description = "Should successfully stop ride from STOP_REQUESTED status")
    public void stopRide_FromStopRequested_Success() throws Exception {
        ride.setStatus(RideStatus.STOP_REQUESTED);
        rideRepository.save(ride);

        WebStopRideEarlyRequest request = WebStopRideEarlyRequest.builder()
                .stopLocation(WebLocationPoint.builder()
                        .address("New Stop Location")
                        .latitude(45.2600)
                        .longitude(19.8400)
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ride completed at new location."));

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(updatedRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    @Test(description = "Should return 403 when no authentication (no token, no cookie) provided for stop")
    public void stopRide_NoAuth_403() throws Exception {
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test(dataProvider = "unauthorizedRolesForStopRide",
            description = "Should return 500 when unauthorized role tries to stop ride")
    public void stopRide_UnauthorizedRole_AccessDenied(String roleName, String roleType) throws Exception {
        String token = roleName.equals("passenger") ? passengerToken : adminToken;

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(token))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test(description = "Should return 400 when ride does not exist for stop")
    public void stopRide_RideNotFound_400() throws Exception {
        mockMvc.perform(post("/api/v1/rides/99999/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Ride not found")));
    }

    @Test(description = "Should return 400 when ride status is invalid for stop")
    public void stopRide_InvalidStatus_400() throws Exception {
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);

        WebStopRideEarlyRequest request = WebStopRideEarlyRequest.builder()
                .stopLocation(WebLocationPoint.builder()
                        .address("New Stop Location")
                        .latitude(45.2600)
                        .longitude(19.8400)
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test(description = "Should return 400 when driver is not assigned to ride")
    public void stopRide_WrongDriver_400() throws Exception {
        Driver anotherDriver = Driver.builder()
                .email("other-driver@test.com")
                .username("otherdriver")
                .password(passwordEncoder.encode("password"))
                .firstName("Other")
                .lastName("Driver")
                .phoneNumber("+381645555555")
                .address("Other Driver Address")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SUV")
                .licensePlate("NS-456-CD")
                .build();
        userRepository.save(anotherDriver);
        String otherDriverToken = jwtService.generateToken(anotherDriver.getEmail(), anotherDriver.getRole().toString());

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(otherDriverToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("You are not the assigned driver for this ride")));
    }

    @Test(dataProvider = "invalidAddressData",
            description = "Should return 400 when stop location address is invalid")
    public void stopRide_InvalidLocation_InvalidAddress_400(String address, String testCase) throws Exception {
        WebStopRideEarlyRequest request = WebStopRideEarlyRequest.builder()
                .stopLocation(WebLocationPoint.builder()
                        .address(address)
                        .latitude(45.2600)
                        .longitude(19.8400)
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Valid stop location is required")));
    }

    // Testing: Integration Flow

    @Test(description = "Should handle complete early stop flow: request -> stop")
    public void earlyStop_CompleteFlow() throws Exception {
        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/request-stop")
                        .cookie(createJwtCookie(passengerToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Ride afterRequest = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(afterRequest.getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);

        WebStopRideEarlyRequest request = WebStopRideEarlyRequest.builder()
                .stopLocation(WebLocationPoint.builder()
                        .address("New Stop Location")
                        .latitude(45.2600)
                        .longitude(19.8400)
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ride completed at new location."));

        Ride completed = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();

        RideStop lastStop = completed.getStops().get(completed.getStops().size() - 1);
        assertThat(lastStop.getAddress()).isEqualTo("New Stop Location");
    }

    @Test(description = "Should handle direct early stop without passenger request")
    public void earlyStop_DirectStopWithoutRequest() throws Exception {
        WebStopRideEarlyRequest request = WebStopRideEarlyRequest.builder()
                .stopLocation(WebLocationPoint.builder()
                        .address("Emergency Stop Location")
                        .latitude(45.2600)
                        .longitude(19.8400)
                        .build())
                .build();

        mockMvc.perform(post("/api/v1/rides/" + ride.getId() + "/stop")
                        .cookie(createJwtCookie(driverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ride completed at new location."));

        Ride completed = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(RideStatus.COMPLETED);

        RideStop lastStop = completed.getStops().get(completed.getStops().size() - 1);
        assertThat(lastStop.getAddress()).isEqualTo("Emergency Stop Location");
    }
}
