package com.pekara.service;

public interface JwtService {

    String generateToken(String email, String role);

    String getEmailFromToken(String token);

    String getRoleFromToken(String token);

    boolean isTokenValid(String token);
}
