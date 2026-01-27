package com.pekara.mapper;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.common.WebLocationPoint;
import com.pekara.dto.response.FavoriteRouteResponse;
import com.pekara.dto.response.WebFavouriteRouteResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FavoriteRouteMapper {

    public WebFavouriteRouteResponse toWebResponse(FavoriteRouteResponse route) {
        if (route == null) {
            return null;
        }

        List<WebLocationPoint> stops = route.getStops() != null
                ? route.getStops().stream()
                    .map(this::toWebLocationPoint)
                    .collect(Collectors.toList())
                : null;

        return WebFavouriteRouteResponse.builder()
                .id(route.getId())
                .name(route.getName())
                .pickup(toWebLocationPoint(route.getPickup()))
                .stops(stops)
                .dropoff(toWebLocationPoint(route.getDropoff()))
                .vehicleType(route.getVehicleType())
                .babyTransport(route.getBabyTransport())
                .petTransport(route.getPetTransport())
                .build();
    }

    private WebLocationPoint toWebLocationPoint(LocationPointDto dto) {
        if (dto == null) {
            return null;
        }
        return WebLocationPoint.builder()
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    public LocationPointDto toLocationPointDto(WebLocationPoint web) {
        if (web == null) {
            return null;
        }
        return LocationPointDto.builder()
                .address(web.getAddress())
                .latitude(web.getLatitude())
                .longitude(web.getLongitude())
                .build();
    }

    public List<LocationPointDto> toLocationPointDtoList(List<WebLocationPoint> webList) {
        if (webList == null) {
            return null;
        }
        return webList.stream()
                .map(this::toLocationPointDto)
                .collect(Collectors.toList());
    }
}
