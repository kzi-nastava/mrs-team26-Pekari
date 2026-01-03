package com.pekara.model;

/**
 * Enum representing different user roles in the system.
 * Used to distinguish between regular passengers, drivers, and administrators.
 */
public enum UserRole {
    /**
     * Regular passenger who can order rides
     */
    PASSENGER,

    /**
     * Driver who provides rides
     * Has additional fields in Driver entity
     */
    DRIVER,

    /**
     * System administrator with full access
     */
    ADMIN
}
