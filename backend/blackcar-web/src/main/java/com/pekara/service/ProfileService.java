package com.pekara.service;

import com.pekara.dto.response.ProfileResponse;
import com.pekara.dto.response.VehicleResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class ProfileService {

    public ProfileResponse getCurrentProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "john@example.com";

        String role = resolveRole(auth != null ? auth.getAuthorities() : null);

        ProfileResponse response = new ProfileResponse(
                "1",
                email,
                emailToUsername(email),
                "John",
                "Doe",
                "+381 64 000 000",
                "123 Main Street, City",
                role,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        if ("driver".equalsIgnoreCase(role)) {
            response.setVehicle(fetchVehicleForDriver(email));
        }

        return response;
    }

    private static String resolveRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return "passenger";
        }

        for (GrantedAuthority authority : authorities) {
            if (authority == null || authority.getAuthority() == null) {
                continue;
            }

            String value = authority.getAuthority();
            // Accept both ROLE_DRIVER and ROLE_driver (JwtAuthFilter currently creates ROLE_ + role)
            if (value.equalsIgnoreCase("ROLE_DRIVER")) {
                return "driver";
            }
            if (value.equalsIgnoreCase("ROLE_ADMIN")) {
                return "admin";
            }
            if (value.equalsIgnoreCase("ROLE_PASSENGER") || value.equalsIgnoreCase("ROLE_USER")) {
                return "passenger";
            }
        }

        return "passenger";
    }

    private static String emailToUsername(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    /**
     * Placeholder vehicle lookup.
     * Replace with persistence lookup once Vehicle/Driver entities + repositories exist.
     */
    private static VehicleResponse fetchVehicleForDriver(String email) {
        // Dummy data keyed by email so different drivers can be distinguished in dev.
        String suffix = Integer.toHexString(Math.abs(email.hashCode())).substring(0, 6);

        return new VehicleResponse(
                "veh-" + suffix,
                "Tesla",
                "Model 3",
                2023,
                "BG-" + suffix.toUpperCase(),
                "VIN" + suffix.toUpperCase() + "000000000"
        );
    }
}
