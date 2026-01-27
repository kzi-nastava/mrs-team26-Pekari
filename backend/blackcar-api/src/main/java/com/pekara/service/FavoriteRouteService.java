package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.response.FavoriteRouteResponse;

import java.util.List;

public interface FavoriteRouteService {
    List<FavoriteRouteResponse> getFavoriteRoutes(String userEmail);
    FavoriteRouteResponse getFavoriteRouteById(String userEmail, Long id);
    FavoriteRouteResponse createFavoriteRoute(String userEmail, String name, LocationPointDto pickup, List<LocationPointDto> stops, LocationPointDto dropoff, String vehicleType, Boolean babyTransport, Boolean petTransport);
    FavoriteRouteResponse updateFavoriteRoute(String userEmail, Long id, String name, LocationPointDto pickup, List<LocationPointDto> stops, LocationPointDto dropoff, String vehicleType, Boolean babyTransport, Boolean petTransport);
    void deleteFavoriteRoute(String userEmail, Long id);
}
