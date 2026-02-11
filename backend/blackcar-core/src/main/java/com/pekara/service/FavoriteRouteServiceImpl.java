package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.response.FavoriteRouteResponse;
import com.pekara.model.FavoriteRoute;
import com.pekara.model.FavoriteRouteStop;
import com.pekara.model.User;
import com.pekara.repository.FavoriteRouteRepository;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteRouteServiceImpl implements FavoriteRouteService {

    private final FavoriteRouteRepository favoriteRouteRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteRouteResponse> getFavoriteRoutes(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        return favoriteRouteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteRouteResponse getFavoriteRouteById(String userEmail, Long id) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        FavoriteRoute route = favoriteRouteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Favorite route not found: " + id));
        
        if (!route.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Favorite route does not belong to user");
        }
        
        return toDto(route);
    }

    @Override
    @Transactional
    public FavoriteRouteResponse createFavoriteRoute(String userEmail, String name, LocationPointDto pickup, 
                                             List<LocationPointDto> stops, LocationPointDto dropoff, 
                                             String vehicleType, Boolean babyTransport, Boolean petTransport) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        // Log field lengths to identify which field exceeds 255 characters
        log.info("Creating favorite route - Field lengths:");
        log.info("  name: {} chars", name != null ? name.length() : 0);
        log.info("  pickupAddress: {} chars - '{}'", pickup.getAddress() != null ? pickup.getAddress().length() : 0, pickup.getAddress());
        log.info("  dropoffAddress: {} chars - '{}'", dropoff.getAddress() != null ? dropoff.getAddress().length() : 0, dropoff.getAddress());
        log.info("  vehicleType: {} chars", vehicleType != null ? vehicleType.length() : 0);

        FavoriteRoute route = FavoriteRoute.builder()
                .user(user)
                .name(name)
                .pickupAddress(pickup.getAddress())
                .pickupLatitude(pickup.getLatitude())
                .pickupLongitude(pickup.getLongitude())
                .dropoffAddress(dropoff.getAddress())
                .dropoffLatitude(dropoff.getLatitude())
                .dropoffLongitude(dropoff.getLongitude())
                .vehicleType(vehicleType != null ? vehicleType : "STANDARD")
                .babyTransport(babyTransport != null ? babyTransport : false)
                .petTransport(petTransport != null ? petTransport : false)
                .build();

        if (stops != null) {
            for (int i = 0; i < stops.size(); i++) {
                LocationPointDto stop = stops.get(i);
                FavoriteRouteStop routeStop = FavoriteRouteStop.builder()
                        .favoriteRoute(route)
                        .sequenceIndex(i)
                        .address(stop.getAddress())
                        .latitude(stop.getLatitude())
                        .longitude(stop.getLongitude())
                        .build();
                route.addStop(routeStop);
            }
        }

        FavoriteRoute saved = favoriteRouteRepository.save(route);
        return toDto(saved);
    }

    @Override
    @Transactional
    public FavoriteRouteResponse updateFavoriteRoute(String userEmail, Long id, String name, 
                                             LocationPointDto pickup, List<LocationPointDto> stops, 
                                             LocationPointDto dropoff, String vehicleType, 
                                             Boolean babyTransport, Boolean petTransport) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        FavoriteRoute route = favoriteRouteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Favorite route not found: " + id));
        
        if (!route.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Favorite route does not belong to user");
        }

        route.setName(name);
        route.setPickupAddress(pickup.getAddress());
        route.setPickupLatitude(pickup.getLatitude());
        route.setPickupLongitude(pickup.getLongitude());
        route.setDropoffAddress(dropoff.getAddress());
        route.setDropoffLatitude(dropoff.getLatitude());
        route.setDropoffLongitude(dropoff.getLongitude());
        route.setVehicleType(vehicleType != null ? vehicleType : "STANDARD");
        route.setBabyTransport(babyTransport != null ? babyTransport : false);
        route.setPetTransport(petTransport != null ? petTransport : false);

        // Clear existing stops
        route.getStops().clear();

        // Add new stops
        if (stops != null) {
            for (int i = 0; i < stops.size(); i++) {
                LocationPointDto stop = stops.get(i);
                FavoriteRouteStop routeStop = FavoriteRouteStop.builder()
                        .favoriteRoute(route)
                        .sequenceIndex(i)
                        .address(stop.getAddress())
                        .latitude(stop.getLatitude())
                        .longitude(stop.getLongitude())
                        .build();
                route.addStop(routeStop);
            }
        }

        FavoriteRoute saved = favoriteRouteRepository.save(route);
        return toDto(saved);
    }

    private FavoriteRouteResponse toDto(FavoriteRoute route) {
        List<LocationPointDto> stops = route.getStops() != null
                ? route.getStops().stream()
                    .sorted((s1, s2) -> Integer.compare(s1.getSequenceIndex(), s2.getSequenceIndex()))
                    .map(s -> LocationPointDto.builder()
                            .address(s.getAddress())
                            .latitude(s.getLatitude())
                            .longitude(s.getLongitude())
                            .build())
                    .collect(Collectors.toList())
                : null;

        return FavoriteRouteResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .pickup(LocationPointDto.builder()
                        .address(route.getPickupAddress())
                        .latitude(route.getPickupLatitude())
                        .longitude(route.getPickupLongitude())
                        .build())
                .stops(stops)
                .dropoff(LocationPointDto.builder()
                        .address(route.getDropoffAddress())
                        .latitude(route.getDropoffLatitude())
                        .longitude(route.getDropoffLongitude())
                        .build())
                .vehicleType(route.getVehicleType())
                .babyTransport(route.getBabyTransport())
                .petTransport(route.getPetTransport())
                .build();
    }

    @Override
    @Transactional
    public void deleteFavoriteRoute(String userEmail, Long id) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        if (!favoriteRouteRepository.existsByIdAndUserId(id, user.getId())) {
            throw new IllegalArgumentException("Favorite route not found or does not belong to user");
        }
        
        favoriteRouteRepository.deleteByIdAndUserId(id, user.getId());
    }
}
