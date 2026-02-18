package com.pekara.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pekara.dto.PricingDto;
import com.pekara.dto.RouteDto;
import com.pekara.dto.common.LocationPointDto;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RideEstimationServiceImpl - ride ordering flow (route and price calculation).
 */
@Listeners(MockitoTestNGListener.class)
public class RideEstimationServiceImplTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private PricingService pricingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RideEstimationServiceImpl rideEstimationService;

    private LocationPointDto pickup;
    private LocationPointDto dropoff;
    private List<LocationPointDto> routePoints;

    @BeforeMethod
    public void setUp() {
        pickup = LocationPointDto.builder()
                .address("Pickup Address")
                .latitude(45.25)
                .longitude(19.84)
                .build();
        dropoff = LocationPointDto.builder()
                .address("Dropoff Address")
                .latitude(45.27)
                .longitude(19.85)
                .build();
        routePoints = List.of(pickup, dropoff);
    }

    @Test(description = "Should throw when pickup is null")
    public void calculateRouteWithStops_NullPickup_Throws() {
        assertThatThrownBy(() -> rideEstimationService.calculateRouteWithStops(null, dropoff, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pickup is required");
    }

    @Test(description = "Should throw when dropoff is null")
    public void calculateRouteWithStops_NullDropoff_Throws() {
        assertThatThrownBy(() -> rideEstimationService.calculateRouteWithStops(pickup, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dropoff is required");
    }

    @Test(description = "Should throw when pickup address is blank")
    public void calculateRouteWithStops_BlankPickupAddress_Throws() {
        pickup.setAddress("   ");
        assertThatThrownBy(() -> rideEstimationService.calculateRouteWithStops(pickup, dropoff, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pickup address is required");
    }

    @Test(description = "Should delegate to routing and return RouteData for valid pickup and dropoff")
    public void calculateRouteWithStops_ValidPickupDropoff_ReturnsRouteData() {
        RouteDto routeDto = RouteDto.builder()
                .distanceKm(10.5)
                .durationMinutes(20)
                .routePoints(routePoints)
                .build();
        when(routingService.calculateRoute(anyList())).thenReturn(routeDto);

        RideEstimationService.RouteData result = rideEstimationService.calculateRouteWithStops(pickup, dropoff, null);

        assertThat(result.getDistanceKm()).isEqualTo(10.5);
        assertThat(result.getDurationMinutes()).isEqualTo(20);
        assertThat(result.getRoutePoints()).isEqualTo(routePoints);
        verify(routingService).calculateRoute(List.of(pickup, dropoff));
    }

    @Test(description = "Should include optional stops in waypoints passed to routing")
    public void calculateRouteWithStops_WithStops_IncludesStopsInWaypoints() {
        LocationPointDto stop1 = LocationPointDto.builder().address("Stop 1").latitude(45.26).longitude(19.845).build();
        List<LocationPointDto> stops = List.of(stop1);
        RouteDto routeDto = RouteDto.builder()
                .distanceKm(15.0)
                .durationMinutes(25)
                .routePoints(List.of(pickup, stop1, dropoff))
                .build();
        when(routingService.calculateRoute(anyList())).thenReturn(routeDto);

        RideEstimationService.RouteData result = rideEstimationService.calculateRouteWithStops(pickup, dropoff, stops);

        assertThat(result.getDistanceKm()).isEqualTo(15.0);
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        verify(routingService).calculateRoute(List.of(pickup, stop1, dropoff));
    }

    @Test(description = "Should throw when a stop has null address")
    public void calculateRouteWithStops_StopWithNullAddress_Throws() {
        LocationPointDto badStop = LocationPointDto.builder().address(null).latitude(45.26).longitude(19.84).build();
        assertThatThrownBy(() -> rideEstimationService.calculateRouteWithStops(pickup, dropoff, List.of(badStop)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stop address is required");
    }

    @Test(description = "Should calculate price as basePrice + pricePerKm * distanceKm")
    public void calculatePrice_ValidInput_ReturnsCorrectPrice() {
        when(pricingService.getPricingByVehicleType("SEDAN"))
                .thenReturn(PricingDto.builder()
                        .vehicleType("SEDAN")
                        .basePrice(new BigDecimal("200"))
                        .pricePerKm(new BigDecimal("120"))
                        .build());

        BigDecimal result = rideEstimationService.calculatePrice("SEDAN", 10.0);

        assertThat(result).isEqualByComparingTo(new BigDecimal("1400.00")); // 200 + 10 * 120
        verify(pricingService).getPricingByVehicleType("SEDAN");
    }

    @Test(description = "Should round price to 2 decimal places")
    public void calculatePrice_RoundsToTwoDecimals() {
        when(pricingService.getPricingByVehicleType(eq("SUV")))
                .thenReturn(PricingDto.builder()
                        .basePrice(new BigDecimal("300"))
                        .pricePerKm(new BigDecimal("120"))
                        .build());

        BigDecimal result = rideEstimationService.calculatePrice("SUV", 1.111);

        assertThat(result).isEqualByComparingTo(new BigDecimal("433.32"));
    }

    @Test(description = "Should round km to 3 decimal places")
    public void roundKm_RoundsToThreeDecimals() {
        assertThat(rideEstimationService.roundKm(10.12345)).isEqualTo(10.123);
        assertThat(rideEstimationService.roundKm(5.9996)).isEqualTo(6.0);
    }

    @Test(description = "Should return null when route points is null")
    public void serializeRouteCoordinates_Null_ReturnsNull() {
        String result = rideEstimationService.serializeRouteCoordinates(null);
        assertThat(result).isNull();
    }

    @Test(description = "Should return null when route points is empty")
    public void serializeRouteCoordinates_Empty_ReturnsNull() {
        String result = rideEstimationService.serializeRouteCoordinates(Collections.emptyList());
        assertThat(result).isNull();
    }

    @Test(description = "Should serialize route points to JSON array")
    public void serializeRouteCoordinates_ValidPoints_ReturnsJson() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("[[45.25,19.84],[45.27,19.85]]");

        String result = rideEstimationService.serializeRouteCoordinates(routePoints);

        assertThat(result).isEqualTo("[[45.25,19.84],[45.27,19.85]]");
        verify(objectMapper).writeValueAsString(any());
    }

    @Test(description = "Should return null when JSON serialization fails")
    public void serializeRouteCoordinates_JsonFails_ReturnsNull() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail") {});

        String result = rideEstimationService.serializeRouteCoordinates(routePoints);

        assertThat(result).isNull();
    }

    @Test(description = "Should throw when location point is null")
    public void validateLocation_NullPoint_Throws() {
        assertThatThrownBy(() -> rideEstimationService.validateLocation(null, "pickup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pickup is required");
    }

    @Test(description = "Should throw when address is null")
    public void validateLocation_NullAddress_Throws() {
        pickup.setAddress(null);
        assertThatThrownBy(() -> rideEstimationService.validateLocation(pickup, "pickup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pickup address is required");
    }

    @Test(description = "Should throw when address is blank")
    public void validateLocation_BlankAddress_Throws() {
        pickup.setAddress("   ");
        assertThatThrownBy(() -> rideEstimationService.validateLocation(pickup, "dropoff"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dropoff address is required");
    }
}
