package com.pekara.security;

import com.pekara.service.JwtService;
import com.pekara.service.RideTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final RideTrackingService rideTrackingService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (SimpMessageType.CONNECT.equals(accessor.getMessageType()) || StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return;
        }

        String raw = authHeaders.get(0);
        if (raw == null || !raw.startsWith("Bearer ")) {
            return;
        }

        String token = raw.substring(7);
        try {
            if (jwtService.isTokenValid(token)) {
                String email = jwtService.getEmailFromToken(token);
                String role = jwtService.getRoleFromToken(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                accessor.setUser(auth);
            }
        } catch (Exception ex) {
            log.warn("STOMP auth failed: {}", ex.getMessage());
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || accessor.getUser() == null) {
            return;
        }

        // Only guard ride tracking topics
        if (destination.startsWith("/topic/rides/") && destination.endsWith("/tracking")) {
            String[] parts = destination.split("/");
            if (parts.length < 4) {
                return;
            }
            try {
                Long rideId = Long.parseLong(parts[3]);
                String email = accessor.getUser().getName();
                // Will throw if unauthorized or ride not active
                rideTrackingService.getTracking(rideId, email);
            } catch (Exception ex) {
                log.warn("Blocking subscription to {}: {}", destination, ex.getMessage());
                throw new IllegalArgumentException("Not authorized for ride tracking");
            }
        }
    }
}
