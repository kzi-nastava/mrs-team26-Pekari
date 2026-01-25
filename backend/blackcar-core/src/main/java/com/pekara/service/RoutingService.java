package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.RouteDto;

import java.util.List;

public interface RoutingService {
    RouteDto calculateRoute(List<LocationPointDto> waypoints);
}
