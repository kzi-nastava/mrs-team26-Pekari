-- =============================================================================
-- BlackCar / Pekari - Seed Data for Neon SQL Editor
-- Run this in Neon SQL Editor to populate tables with dev/test data.
-- All seeded accounts use password: password
-- =============================================================================

-- Clear existing data (DELETE in FK order - avoids TRUNCATE restrictions in some SQL editors)
DELETE FROM ride_ratings;
DELETE FROM ride_passengers;
DELETE FROM ride_stops;
DELETE FROM inconsistency_reports;
DELETE FROM driver_work_logs;
DELETE FROM messages;
DELETE FROM conversation_participants;
DELETE FROM rides;
DELETE FROM conversations;
DELETE FROM favorite_route_stops;
DELETE FROM favorite_routes;
DELETE FROM account_activation_tokens;
DELETE FROM driver_states;
DELETE FROM users;
DELETE FROM pricing;

-- Reset sequences for identity columns
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS rides_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS ride_stops_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS ride_ratings_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS favorite_routes_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS favorite_route_stops_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS driver_work_logs_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS conversations_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS messages_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS account_activation_tokens_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS inconsistency_reports_id_seq RESTART WITH 1;

-- BCrypt hash for "password" (all seeded accounts use this)
-- Generated with cost 10. Login with: admin@test.com / password, etc.

-- =============================================================================
-- 1. PRICING (vehicle types must match: STANDARD, VAN, LUX)
-- =============================================================================
INSERT INTO pricing (vehicle_type, base_price, price_per_km) VALUES
('STANDARD', 200.00, 120.00),
('VAN', 300.00, 120.00),
('LUX', 500.00, 120.00);

-- =============================================================================
-- 2. USERS (single-table inheritance: user_type = 'USER' or 'DRIVER')
-- Admins (id 1-2)
-- Passengers (id 3-6)
-- Drivers (id 7-10)
-- =============================================================================
INSERT INTO users (
    id, email, username, password, first_name, last_name, phone_number, address,
    profile_picture, role, user_type, is_active, blocked, blocked_note, total_rides,
    average_rating, created_at, updated_at,
    license_number, license_expiry, vehicle_type, vehicle_model, vehicle_license_plate,
    vehicle_number_of_seats, vehicle_baby_friendly, vehicle_pet_friendly
) VALUES
-- Admins
(1, 'admin@test.com', 'admin_main', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Super', 'Admin', '+381601234567', 'Novi Sad', NULL, 'ADMIN', 'USER', true, false, NULL, 0, NULL,
 NOW(), NOW(),
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(2, 'support@test.com', 'admin_support', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Support', 'Tech', '+381602345678', 'Novi Sad', NULL, 'ADMIN', 'USER', true, false, NULL, 0, NULL,
 NOW(), NOW(),
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- Passengers (total_rides = completed count for stats display)
(3, 'passenger1@test.com', 'p_alice', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Alice', 'Smith', '+381603456789', 'Novi Sad', NULL, 'PASSENGER', 'USER', true, false, NULL, 11, 4.7,
 NOW(), NOW(),
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(4, 'passenger2@test.com', 'p_bob', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Bob', 'Jones', '+381604567890', 'Novi Sad', NULL, 'PASSENGER', 'USER', true, false, NULL, 11, 4.8,
 NOW(), NOW(),
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(5, 'passenger3@test.com', 'p_charlie', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Charlie', 'Brown', '+381605678901', 'Novi Sad', NULL, 'PASSENGER', 'USER', true, false, NULL, 10, 4.8,
 NOW(), NOW(),
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(6, 'passenger4@test.com', 'p_dana', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Dana', 'White', '+381606789012', 'Novi Sad', NULL, 'PASSENGER', 'USER', true, false, NULL, 10, 4.7,
 NOW(), NOW(),
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
-- Drivers (user_type = DRIVER, driver-specific columns filled)
(7, 'driver_std@test.com', 'd_john', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'John', 'Doe', '+381617111111', 'Novi Sad', NULL, 'DRIVER', 'DRIVER', true, false, NULL, 0, NULL,
 NOW(), NOW(),
 'LIC-NS-111-AA', '2030-01', 'STANDARD', 'Toyota Corolla', 'NS-111-AA', 4, true, false),
(8, 'driver_lux@test.com', 'd_jane_lux', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Jane', 'Smith', '+381617222222', 'Novi Sad', NULL, 'DRIVER', 'DRIVER', true, false, NULL, 0, NULL,
 NOW(), NOW(),
 'LIC-NS-222-BB', '2030-01', 'LUX', 'Mercedes E-Class', 'NS-222-BB', 4, false, true),
(9, 'driver_std2@test.com', 'd_mike', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Mike', 'Ross', '+381617333333', 'Novi Sad', NULL, 'DRIVER', 'DRIVER', true, false, NULL, 0, NULL,
 NOW(), NOW(),
 'LIC-NS-333-CC', '2030-01', 'STANDARD', 'Toyota Camry', 'NS-333-CC', 4, true, true),
(10, 'driver_van@test.com', 'd_sarah', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
 'Sarah', 'Connor', '+381617444444', 'Novi Sad', NULL, 'DRIVER', 'DRIVER', true, false, NULL, 0, NULL,
 NOW(), NOW(),
 'LIC-NS-444-DD', '2030-01', 'VAN', 'Volkswagen Transporter', 'NS-444-DD', 7, true, true);

-- Override sequence for users
SELECT setval('users_id_seq', 10);

-- =============================================================================
-- 3. DRIVER_STATES (one per driver, driver_id is PK via @MapsId)
-- =============================================================================
INSERT INTO driver_states (driver_id, online, busy, latitude, longitude, updated_at, version) VALUES
(7, true, false, 45.2671, 19.8335, NOW(), 1),
(8, true, false, 45.2464, 19.8517, NOW(), 1),
(9, true, false, 45.2600, 19.8000, NOW(), 1),
(10, true, false, 45.2396, 19.8227, NOW(), 1);

-- =============================================================================
-- 4. RIDES (creator = passenger, driver = driver, status)
-- Stats graphs use COMPLETED rides only. Spread across ~30 days for charting.
-- Ride 1-4: recent (today/yesterday) + 1 scheduled + 1 in_progress
-- Rides 5+: historical completed rides for statistics
-- =============================================================================
INSERT INTO rides (
    id, creator_user_id, driver_user_id, status, vehicle_type, baby_transport, pet_transport,
    scheduled_at, estimated_price, distance_km, estimated_duration_minutes, route_coordinates,
    started_at, completed_at, created_at, updated_at, panic_activated
) VALUES
-- Recent rides (today/yesterday)
(1, 3, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 350.00, 2.5, 8, '[]',
 NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', NOW(), false),
(2, 4, 8, 'COMPLETED', 'LUX', false, true, NULL, 650.00, 3.0, 10, '[]',
 NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW(), false),
(3, 5, NULL, 'SCHEDULED', 'STANDARD', true, false, NOW() + INTERVAL '2 hours', 400.00, 3.0, 10, NULL,
 NULL, NULL, NOW(), NOW(), false),
(4, 3, 9, 'IN_PROGRESS', 'STANDARD', true, false, NULL, 280.00, 1.8, 6, '[]',
 NOW() - INTERVAL '2 minutes', NULL, NOW(), NOW(), false),
-- Historical completed rides for stats (spread across past 30 days)
(5, 3, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 420.00, 3.2, 10, '[]',
 (CURRENT_DATE - 2) + TIME '09:15', (CURRENT_DATE - 2) + TIME '09:25', (CURRENT_DATE - 2) + TIME '09:10', NOW(), false),
(6, 4, 9, 'COMPLETED', 'STANDARD', true, false, NULL, 380.00, 2.8, 9, '[]',
 (CURRENT_DATE - 2) + TIME '14:00', (CURRENT_DATE - 2) + TIME '14:12', (CURRENT_DATE - 2) + TIME '13:55', NOW(), false),
(7, 5, 8, 'COMPLETED', 'LUX', false, true, NULL, 720.00, 3.5, 12, '[]',
 (CURRENT_DATE - 3) + TIME '11:30', (CURRENT_DATE - 3) + TIME '11:45', (CURRENT_DATE - 3) + TIME '11:20', NOW(), false),
(8, 6, 10, 'COMPLETED', 'VAN', true, true, NULL, 550.00, 4.0, 14, '[]',
 (CURRENT_DATE - 3) + TIME '16:45', (CURRENT_DATE - 3) + TIME '17:02', (CURRENT_DATE - 3) + TIME '16:40', NOW(), false),
(9, 3, 9, 'COMPLETED', 'STANDARD', false, false, NULL, 290.00, 1.5, 5, '[]',
 (CURRENT_DATE - 4) + TIME '08:00', (CURRENT_DATE - 4) + TIME '08:08', (CURRENT_DATE - 4) + TIME '07:55', NOW(), false),
(10, 4, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 450.00, 3.5, 11, '[]',
 (CURRENT_DATE - 4) + TIME '18:20', (CURRENT_DATE - 4) + TIME '18:33', (CURRENT_DATE - 4) + TIME '18:15', NOW(), false),
(11, 5, 10, 'COMPLETED', 'VAN', false, true, NULL, 480.00, 3.2, 11, '[]',
 (CURRENT_DATE - 5) + TIME '10:00', (CURRENT_DATE - 5) + TIME '10:14', (CURRENT_DATE - 5) + TIME '09:55', NOW(), false),
(12, 6, 8, 'COMPLETED', 'LUX', false, false, NULL, 580.00, 2.2, 8, '[]',
 (CURRENT_DATE - 5) + TIME '19:30', (CURRENT_DATE - 5) + TIME '19:40', (CURRENT_DATE - 5) + TIME '19:25', NOW(), false),
(13, 3, 7, 'COMPLETED', 'STANDARD', true, false, NULL, 360.00, 2.6, 8, '[]',
 (CURRENT_DATE - 6) + TIME '07:30', (CURRENT_DATE - 6) + TIME '07:40', (CURRENT_DATE - 6) + TIME '07:25', NOW(), false),
(14, 4, 9, 'COMPLETED', 'STANDARD', false, true, NULL, 410.00, 3.0, 9, '[]',
 (CURRENT_DATE - 6) + TIME '12:15', (CURRENT_DATE - 6) + TIME '12:26', (CURRENT_DATE - 6) + TIME '12:10', NOW(), false),
(15, 5, 10, 'COMPLETED', 'VAN', true, true, NULL, 620.00, 4.5, 15, '[]',
 (CURRENT_DATE - 7) + TIME '09:00', (CURRENT_DATE - 7) + TIME '09:18', (CURRENT_DATE - 7) + TIME '08:55', NOW(), false),
(16, 6, 8, 'COMPLETED', 'LUX', false, true, NULL, 890.00, 5.2, 18, '[]',
 (CURRENT_DATE - 7) + TIME '15:00', (CURRENT_DATE - 7) + TIME '15:21', (CURRENT_DATE - 7) + TIME '14:55', NOW(), false),
(17, 3, 9, 'COMPLETED', 'STANDARD', false, false, NULL, 320.00, 2.0, 6, '[]',
 (CURRENT_DATE - 8) + TIME '08:45', (CURRENT_DATE - 8) + TIME '08:53', (CURRENT_DATE - 8) + TIME '08:40', NOW(), false),
(18, 4, 7, 'COMPLETED', 'STANDARD', true, false, NULL, 390.00, 2.7, 9, '[]',
 (CURRENT_DATE - 8) + TIME '17:30', (CURRENT_DATE - 8) + TIME '17:41', (CURRENT_DATE - 8) + TIME '17:25', NOW(), false),
(19, 5, 10, 'COMPLETED', 'VAN', false, false, NULL, 440.00, 3.0, 10, '[]',
 (CURRENT_DATE - 9) + TIME '11:00', (CURRENT_DATE - 9) + TIME '11:12', (CURRENT_DATE - 9) + TIME '10:55', NOW(), false),
(20, 6, 8, 'COMPLETED', 'LUX', false, true, NULL, 750.00, 4.0, 14, '[]',
 (CURRENT_DATE - 9) + TIME '20:00', (CURRENT_DATE - 9) + TIME '20:17', (CURRENT_DATE - 9) + TIME '19:55', NOW(), false),
(21, 3, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 270.00, 1.2, 4, '[]',
 (CURRENT_DATE - 10) + TIME '06:30', (CURRENT_DATE - 10) + TIME '06:36', (CURRENT_DATE - 10) + TIME '06:25', NOW(), false),
(22, 4, 9, 'COMPLETED', 'STANDARD', true, true, NULL, 430.00, 3.1, 10, '[]',
 (CURRENT_DATE - 10) + TIME '13:00', (CURRENT_DATE - 10) + TIME '13:12', (CURRENT_DATE - 10) + TIME '12:55', NOW(), false),
(23, 5, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 510.00, 3.8, 12, '[]',
 (CURRENT_DATE - 11) + TIME '10:30', (CURRENT_DATE - 11) + TIME '10:45', (CURRENT_DATE - 11) + TIME '10:25', NOW(), false),
(24, 6, 10, 'COMPLETED', 'VAN', true, false, NULL, 530.00, 3.6, 12, '[]',
 (CURRENT_DATE - 11) + TIME '16:00', (CURRENT_DATE - 11) + TIME '16:15', (CURRENT_DATE - 11) + TIME '15:55', NOW(), false),
(25, 3, 8, 'COMPLETED', 'LUX', false, false, NULL, 680.00, 3.2, 11, '[]',
 (CURRENT_DATE - 12) + TIME '09:15', (CURRENT_DATE - 12) + TIME '09:28', (CURRENT_DATE - 12) + TIME '09:10', NOW(), false),
(26, 4, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 340.00, 2.3, 7, '[]',
 (CURRENT_DATE - 12) + TIME '18:45', (CURRENT_DATE - 12) + TIME '18:54', (CURRENT_DATE - 12) + TIME '18:40', NOW(), false),
(27, 5, 9, 'COMPLETED', 'STANDARD', true, false, NULL, 400.00, 2.9, 9, '[]',
 (CURRENT_DATE - 14) + TIME '08:00', (CURRENT_DATE - 14) + TIME '08:11', (CURRENT_DATE - 14) + TIME '07:55', NOW(), false),
(28, 6, 10, 'COMPLETED', 'VAN', false, true, NULL, 590.00, 4.2, 14, '[]',
 (CURRENT_DATE - 14) + TIME '14:30', (CURRENT_DATE - 14) + TIME '14:47', (CURRENT_DATE - 14) + TIME '14:25', NOW(), false),
(29, 3, 8, 'COMPLETED', 'LUX', false, true, NULL, 820.00, 4.5, 15, '[]',
 (CURRENT_DATE - 15) + TIME '11:00', (CURRENT_DATE - 15) + TIME '11:18', (CURRENT_DATE - 15) + TIME '10:55', NOW(), false),
(30, 4, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 370.00, 2.5, 8, '[]',
 (CURRENT_DATE - 15) + TIME '19:00', (CURRENT_DATE - 15) + TIME '19:10', (CURRENT_DATE - 15) + TIME '18:55', NOW(), false),
(31, 5, 9, 'COMPLETED', 'STANDARD', true, true, NULL, 460.00, 3.4, 11, '[]',
 (CURRENT_DATE - 17) + TIME '07:30', (CURRENT_DATE - 17) + TIME '07:43', (CURRENT_DATE - 17) + TIME '07:25', NOW(), false),
(32, 6, 10, 'COMPLETED', 'VAN', true, false, NULL, 500.00, 3.5, 12, '[]',
 (CURRENT_DATE - 17) + TIME '16:00', (CURRENT_DATE - 17) + TIME '16:15', (CURRENT_DATE - 17) + TIME '15:55', NOW(), false),
(33, 3, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 310.00, 1.8, 6, '[]',
 (CURRENT_DATE - 18) + TIME '09:00', (CURRENT_DATE - 18) + TIME '09:08', (CURRENT_DATE - 18) + TIME '08:55', NOW(), false),
(34, 4, 8, 'COMPLETED', 'LUX', false, true, NULL, 710.00, 3.8, 13, '[]',
 (CURRENT_DATE - 18) + TIME '20:30', (CURRENT_DATE - 18) + TIME '20:46', (CURRENT_DATE - 18) + TIME '20:25', NOW(), false),
(35, 5, 9, 'COMPLETED', 'STANDARD', false, false, NULL, 350.00, 2.4, 8, '[]',
 (CURRENT_DATE - 20) + TIME '10:15', (CURRENT_DATE - 20) + TIME '10:25', (CURRENT_DATE - 20) + TIME '10:10', NOW(), false),
(36, 6, 10, 'COMPLETED', 'VAN', true, true, NULL, 640.00, 4.3, 14, '[]',
 (CURRENT_DATE - 20) + TIME '15:45', (CURRENT_DATE - 20) + TIME '16:02', (CURRENT_DATE - 20) + TIME '15:40', NOW(), false),
(37, 3, 7, 'COMPLETED', 'STANDARD', true, false, NULL, 420.00, 3.0, 9, '[]',
 (CURRENT_DATE - 22) + TIME '08:30', (CURRENT_DATE - 22) + TIME '08:41', (CURRENT_DATE - 22) + TIME '08:25', NOW(), false),
(38, 4, 9, 'COMPLETED', 'STANDARD', false, true, NULL, 380.00, 2.7, 9, '[]',
 (CURRENT_DATE - 22) + TIME '17:00', (CURRENT_DATE - 22) + TIME '17:11', (CURRENT_DATE - 22) + TIME '16:55', NOW(), false),
(39, 5, 8, 'COMPLETED', 'LUX', false, false, NULL, 600.00, 2.8, 10, '[]',
 (CURRENT_DATE - 25) + TIME '12:00', (CURRENT_DATE - 25) + TIME '12:12', (CURRENT_DATE - 25) + TIME '11:55', NOW(), false),
(40, 6, 7, 'COMPLETED', 'STANDARD', false, false, NULL, 440.00, 3.2, 10, '[]',
 (CURRENT_DATE - 25) + TIME '18:30', (CURRENT_DATE - 25) + TIME '18:42', (CURRENT_DATE - 25) + TIME '18:25', NOW(), false),
(41, 3, 10, 'COMPLETED', 'VAN', true, true, NULL, 570.00, 4.0, 13, '[]',
 (CURRENT_DATE - 28) + TIME '09:00', (CURRENT_DATE - 28) + TIME '09:16', (CURRENT_DATE - 28) + TIME '08:55', NOW(), false),
(42, 4, 8, 'COMPLETED', 'LUX', false, true, NULL, 780.00, 4.2, 14, '[]',
 (CURRENT_DATE - 28) + TIME '14:00', (CURRENT_DATE - 28) + TIME '14:17', (CURRENT_DATE - 28) + TIME '13:55', NOW(), false),
(43, 5, 9, 'COMPLETED', 'STANDARD', false, false, NULL, 290.00, 1.6, 5, '[]',
 (CURRENT_DATE - 29) + TIME '07:00', (CURRENT_DATE - 29) + TIME '07:07', (CURRENT_DATE - 29) + TIME '06:55', NOW(), false),
(44, 6, 7, 'COMPLETED', 'STANDARD', true, false, NULL, 410.00, 3.0, 9, '[]',
 (CURRENT_DATE - 29) + TIME '16:30', (CURRENT_DATE - 29) + TIME '16:41', (CURRENT_DATE - 29) + TIME '16:25', NOW(), false);

SELECT setval('rides_id_seq', 44);

-- =============================================================================
-- 5. RIDE_STOPS (pickup, dropoff for each ride)
-- =============================================================================
INSERT INTO ride_stops (ride_id, sequence_index, address, latitude, longitude) VALUES
-- Rides 1-4
(1, 0, 'Bulevar oslobodjenja 10, Novi Sad', 45.2671, 19.8335),
(1, 1, 'Trg slobode 5, Novi Sad', 45.2671, 19.8389),
(2, 0, 'Bulevar oslobodjenja 50, Novi Sad', 45.2671, 19.8400),
(2, 1, 'Futoska 20, Novi Sad', 45.2464, 19.8517),
(3, 0, 'Bulevar oslobodjenja 30, Novi Sad', 45.2671, 19.8350),
(3, 1, 'Liman 4, Novi Sad', 45.2464, 19.8517),
(4, 0, 'Bulevar oslobodjenja 15, Novi Sad', 45.2671, 19.8340),
(4, 1, 'Novo Naselje 12, Novi Sad', 45.2600, 19.8000),
-- Rides 5-44 (historical)
(5, 0, 'Bulevar oslobodjenja 20, Novi Sad', 45.2671, 19.8340),
(5, 1, 'Futoska 15, Novi Sad', 45.2464, 19.8500),
(6, 0, 'Liman 2, Novi Sad', 45.2464, 19.8510),
(6, 1, 'Bulevar oslobodjenja 40, Novi Sad', 45.2671, 19.8380),
(7, 0, 'Trg slobode 3, Novi Sad', 45.2671, 19.8385),
(7, 1, 'Liman 6, Novi Sad', 45.2464, 19.8520),
(8, 0, 'Novo Naselje 8, Novi Sad', 45.2600, 19.8010),
(8, 1, 'Petrovaradin, Novi Sad', 45.2396, 19.8230),
(9, 0, 'Bulevar oslobodjenja 5, Novi Sad', 45.2671, 19.8325),
(9, 1, 'Futoska 10, Novi Sad', 45.2464, 19.8490),
(10, 0, 'Liman 1, Novi Sad', 45.2464, 19.8505),
(10, 1, 'Bulevar oslobodjenja 60, Novi Sad', 45.2671, 19.8420),
(11, 0, 'Novo Naselje 5, Novi Sad', 45.2600, 19.7995),
(11, 1, 'Petrovaradin 2, Novi Sad', 45.2396, 19.8215),
(12, 0, 'Bulevar oslobodjenja 25, Novi Sad', 45.2671, 19.8355),
(12, 1, 'Liman 3, Novi Sad', 45.2464, 19.8515),
(13, 0, 'Futoska 25, Novi Sad', 45.2464, 19.8525),
(13, 1, 'Bulevar oslobodjenja 35, Novi Sad', 45.2671, 19.8365),
(14, 0, 'Trg slobode 8, Novi Sad', 45.2671, 19.8390),
(14, 1, 'Novo Naselje 15, Novi Sad', 45.2600, 19.8015),
(15, 0, 'Petrovaradin 5, Novi Sad', 45.2396, 19.8240),
(15, 1, 'Bulevar oslobodjenja 45, Novi Sad', 45.2671, 19.8395),
(16, 0, 'Liman 7, Novi Sad', 45.2464, 19.8528),
(16, 1, 'Futoska 30, Novi Sad', 45.2464, 19.8535),
(17, 0, 'Bulevar oslobodjenja 12, Novi Sad', 45.2671, 19.8338),
(17, 1, 'Trg slobode 2, Novi Sad', 45.2671, 19.8382),
(18, 0, 'Novo Naselje 3, Novi Sad', 45.2600, 19.7998),
(18, 1, 'Bulevar oslobodjenja 55, Novi Sad', 45.2671, 19.8410),
(19, 0, 'Futoska 18, Novi Sad', 45.2464, 19.8508),
(19, 1, 'Liman 5, Novi Sad', 45.2464, 19.8518),
(20, 0, 'Bulevar oslobodjenja 70, Novi Sad', 45.2671, 19.8430),
(20, 1, 'Petrovaradin 8, Novi Sad', 45.2396, 19.8250),
(21, 0, 'Trg slobode 1, Novi Sad', 45.2671, 19.8380),
(21, 1, 'Bulevar oslobodjenja 8, Novi Sad', 45.2671, 19.8328),
(22, 0, 'Liman 9, Novi Sad', 45.2464, 19.8530),
(22, 1, 'Novo Naselje 20, Novi Sad', 45.2600, 19.8025),
(23, 0, 'Bulevar oslobodjenja 28, Novi Sad', 45.2671, 19.8358),
(23, 1, 'Futoska 22, Novi Sad', 45.2464, 19.8512),
(24, 0, 'Petrovaradin 3, Novi Sad', 45.2396, 19.8220),
(24, 1, 'Bulevar oslobodjenja 38, Novi Sad', 45.2671, 19.8370),
(25, 0, 'Novo Naselje 7, Novi Sad', 45.2600, 19.8005),
(25, 1, 'Liman 8, Novi Sad', 45.2464, 19.8522),
(26, 0, 'Futoska 12, Novi Sad', 45.2464, 19.8495),
(26, 1, 'Trg slobode 6, Novi Sad', 45.2671, 19.8392),
(27, 0, 'Bulevar oslobodjenja 18, Novi Sad', 45.2671, 19.8345),
(27, 1, 'Novo Naselje 9, Novi Sad', 45.2600, 19.8008),
(28, 0, 'Liman 10, Novi Sad', 45.2464, 19.8532),
(28, 1, 'Petrovaradin 6, Novi Sad', 45.2396, 19.8235),
(29, 0, 'Bulevar oslobodjenja 48, Novi Sad', 45.2671, 19.8398),
(29, 1, 'Futoska 28, Novi Sad', 45.2464, 19.8528),
(30, 0, 'Trg slobode 4, Novi Sad', 45.2671, 19.8387),
(30, 1, 'Bulevar oslobodjenja 22, Novi Sad', 45.2671, 19.8348),
(31, 0, 'Novo Naselje 11, Novi Sad', 45.2600, 19.8012),
(31, 1, 'Liman 4, Novi Sad', 45.2464, 19.8517),
(32, 0, 'Petrovaradin 4, Novi Sad', 45.2396, 19.8225),
(32, 1, 'Bulevar oslobodjenja 52, Novi Sad', 45.2671, 19.8405),
(33, 0, 'Futoska 8, Novi Sad', 45.2464, 19.8485),
(33, 1, 'Trg slobode 7, Novi Sad', 45.2671, 19.8395),
(34, 0, 'Bulevar oslobodjenja 32, Novi Sad', 45.2671, 19.8362),
(34, 1, 'Liman 11, Novi Sad', 45.2464, 19.8535),
(35, 0, 'Novo Naselje 14, Novi Sad', 45.2600, 19.8018),
(35, 1, 'Bulevar oslobodjenja 42, Novi Sad', 45.2671, 19.8390),
(36, 0, 'Liman 12, Novi Sad', 45.2464, 19.8538),
(36, 1, 'Petrovaradin 9, Novi Sad', 45.2396, 19.8255),
(37, 0, 'Bulevar oslobodjenja 14, Novi Sad', 45.2671, 19.8342),
(37, 1, 'Futoska 14, Novi Sad', 45.2464, 19.8502),
(38, 0, 'Trg slobode 9, Novi Sad', 45.2671, 19.8398),
(38, 1, 'Novo Naselje 18, Novi Sad', 45.2600, 19.8022),
(39, 0, 'Bulevar oslobodjenja 58, Novi Sad', 45.2671, 19.8415),
(39, 1, 'Liman 13, Novi Sad', 45.2464, 19.8540),
(40, 0, 'Futoska 20, Novi Sad', 45.2464, 19.8517),
(40, 1, 'Bulevar oslobodjenja 26, Novi Sad', 45.2671, 19.8352),
(41, 0, 'Petrovaradin 7, Novi Sad', 45.2396, 19.8245),
(41, 1, 'Novo Naselje 22, Novi Sad', 45.2600, 19.8028),
(42, 0, 'Liman 14, Novi Sad', 45.2464, 19.8542),
(42, 1, 'Bulevar oslobodjenja 62, Novi Sad', 45.2671, 19.8425),
(43, 0, 'Bulevar oslobodjenja 6, Novi Sad', 45.2671, 19.8322),
(43, 1, 'Trg slobode 10, Novi Sad', 45.2671, 19.8400),
(44, 0, 'Novo Naselje 6, Novi Sad', 45.2600, 19.8002),
(44, 1, 'Futoska 24, Novi Sad', 45.2464, 19.8522);

-- =============================================================================
-- 6. RIDE_PASSENGERS (many-to-many, creator is primary passenger)
-- =============================================================================
INSERT INTO ride_passengers (ride_id, user_id) VALUES
(1, 3), (2, 4), (3, 5), (4, 3),
(5, 3), (6, 4), (7, 5), (8, 6), (9, 3), (10, 4), (11, 5), (12, 6), (13, 3), (14, 4),
(15, 5), (16, 6), (17, 3), (18, 4), (19, 5), (20, 6), (21, 3), (22, 4), (23, 5), (24, 6),
(25, 3), (26, 4), (27, 5), (28, 6), (29, 3), (30, 4), (31, 5), (32, 6), (33, 3), (34, 4),
(35, 5), (36, 6), (37, 3), (38, 4), (39, 5), (40, 6), (41, 3), (42, 4), (43, 5), (44, 6);

-- =============================================================================
-- 7. RIDE_RATINGS (one per completed ride, passenger rates driver)
-- =============================================================================
INSERT INTO ride_ratings (ride_id, passenger_user_id, vehicle_rating, driver_rating, comment, created_at) VALUES
(1, 3, 5, 5, 'Smooth ride, great driver!', NOW() - INTERVAL '2 hours'),
(2, 4, 5, 5, 'Luxury car as expected.', NOW() - INTERVAL '1 day'),
(5, 3, 5, 4, 'Good ride.', (CURRENT_DATE - 2) + TIME '09:25'),
(6, 4, 4, 5, 'On time.', (CURRENT_DATE - 2) + TIME '14:12'),
(7, 5, 5, 5, 'Excellent luxury service.', (CURRENT_DATE - 3) + TIME '11:45'),
(8, 6, 5, 4, 'Spacious van, thanks!', (CURRENT_DATE - 3) + TIME '17:02'),
(9, 3, 4, 5, 'Quick and efficient.', (CURRENT_DATE - 4) + TIME '08:08'),
(10, 4, 5, 5, 'Perfect.', (CURRENT_DATE - 4) + TIME '18:33'),
(11, 5, 4, 4, 'Good for pets.', (CURRENT_DATE - 5) + TIME '10:14'),
(12, 6, 5, 5, 'Loved the luxury ride.', (CURRENT_DATE - 5) + TIME '19:40'),
(13, 3, 5, 4, 'Comfortable.', (CURRENT_DATE - 6) + TIME '07:40'),
(14, 4, 4, 5, 'Nice driver.', (CURRENT_DATE - 6) + TIME '12:26'),
(15, 5, 5, 5, 'Great van for family.', (CURRENT_DATE - 7) + TIME '09:18'),
(16, 6, 5, 5, 'Worth every dinar.', (CURRENT_DATE - 7) + TIME '15:21'),
(17, 3, 4, 4, 'Fine.', (CURRENT_DATE - 8) + TIME '08:53'),
(18, 4, 5, 5, 'Very professional.', (CURRENT_DATE - 8) + TIME '17:41'),
(19, 5, 4, 5, 'Smooth.', (CURRENT_DATE - 9) + TIME '11:12'),
(20, 6, 5, 5, 'Best ride ever!', (CURRENT_DATE - 9) + TIME '20:17'),
(21, 3, 5, 5, 'Early morning, no issues.', (CURRENT_DATE - 10) + TIME '06:36'),
(22, 4, 4, 5, 'Good.', (CURRENT_DATE - 10) + TIME '13:12'),
(23, 5, 5, 4, 'Reliable.', (CURRENT_DATE - 11) + TIME '10:45'),
(24, 6, 4, 5, 'Comfortable van.', (CURRENT_DATE - 11) + TIME '16:15'),
(25, 3, 5, 5, 'Luxury at its best.', (CURRENT_DATE - 12) + TIME '09:28'),
(26, 4, 4, 4, 'OK.', (CURRENT_DATE - 12) + TIME '18:54'),
(27, 5, 5, 5, 'Great with baby seat.', (CURRENT_DATE - 14) + TIME '08:11'),
(28, 6, 5, 4, 'Pet friendly, thanks!', (CURRENT_DATE - 14) + TIME '14:47'),
(29, 3, 5, 5, 'Premium experience.', (CURRENT_DATE - 15) + TIME '11:18'),
(30, 4, 4, 5, 'Good value.', (CURRENT_DATE - 15) + TIME '19:10'),
(31, 5, 5, 5, 'Perfect for our needs.', (CURRENT_DATE - 17) + TIME '07:43'),
(32, 6, 4, 5, 'Spacious.', (CURRENT_DATE - 17) + TIME '16:15'),
(33, 3, 5, 4, 'Fast.', (CURRENT_DATE - 18) + TIME '09:08'),
(34, 4, 5, 5, 'Luxury ride, recommended.', (CURRENT_DATE - 18) + TIME '20:46'),
(35, 5, 4, 5, 'Clean car.', (CURRENT_DATE - 20) + TIME '10:25'),
(36, 6, 5, 5, 'Family trip went well.', (CURRENT_DATE - 20) + TIME '16:02'),
(37, 3, 5, 4, 'Baby seat worked great.', (CURRENT_DATE - 22) + TIME '08:41'),
(38, 4, 4, 5, 'Punctual.', (CURRENT_DATE - 22) + TIME '17:11'),
(39, 5, 5, 5, 'Luxury worth it.', (CURRENT_DATE - 25) + TIME '12:12'),
(40, 6, 5, 4, 'Good driver.', (CURRENT_DATE - 25) + TIME '18:42'),
(41, 3, 5, 5, 'Van was perfect.', (CURRENT_DATE - 28) + TIME '09:16'),
(42, 4, 5, 5, 'Best luxury ride.', (CURRENT_DATE - 28) + TIME '14:17'),
(43, 5, 4, 5, 'Short but sweet.', (CURRENT_DATE - 29) + TIME '07:07'),
(44, 6, 5, 4, 'Comfortable with baby.', (CURRENT_DATE - 29) + TIME '16:41');

-- =============================================================================
-- 8. FAVORITE_ROUTES (passenger saved routes)
-- =============================================================================
INSERT INTO favorite_routes (user_id, name, pickup_address, pickup_latitude, pickup_longitude,
    dropoff_address, dropoff_latitude, dropoff_longitude, vehicle_type, baby_transport, pet_transport,
    created_at, updated_at) VALUES
(3, 'Home to Work', 'Bulevar oslobodjenja 10, Novi Sad', 45.2671, 19.8335,
 'Trg slobode 5, Novi Sad', 45.2671, 19.8389, 'STANDARD', false, false, NOW(), NOW()),
(4, 'Airport Run', 'Bulevar oslobodjenja 50, Novi Sad', 45.2671, 19.8400,
 'Nikola Tesla Airport', 45.0333, 20.3167, 'LUX', false, true, NOW(), NOW());

-- Favorite route stops (optional intermediate stops)
INSERT INTO favorite_route_stops (favorite_route_id, sequence_index, address, latitude, longitude) VALUES
(1, 0, 'Bulevar oslobodjenja 25, Novi Sad', 45.2671, 19.8360);

-- =============================================================================
-- 9. DRIVER_WORK_LOGS (tracks driver work time for completed rides)
-- =============================================================================
INSERT INTO driver_work_logs (driver_user_id, ride_id, started_at, ended_at, completed, created_at) VALUES
(7, 1, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', true, NOW() - INTERVAL '2 hours'),
(8, 2, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', true, NOW() - INTERVAL '1 day'),
(9, 4, NOW() - INTERVAL '2 minutes', NULL, false, NOW()),
(7, 5, (CURRENT_DATE - 2) + TIME '09:15', (CURRENT_DATE - 2) + TIME '09:25', true, (CURRENT_DATE - 2) + TIME '09:10'),
(9, 6, (CURRENT_DATE - 2) + TIME '14:00', (CURRENT_DATE - 2) + TIME '14:12', true, (CURRENT_DATE - 2) + TIME '13:55'),
(8, 7, (CURRENT_DATE - 3) + TIME '11:30', (CURRENT_DATE - 3) + TIME '11:45', true, (CURRENT_DATE - 3) + TIME '11:20'),
(10, 8, (CURRENT_DATE - 3) + TIME '16:45', (CURRENT_DATE - 3) + TIME '17:02', true, (CURRENT_DATE - 3) + TIME '16:40'),
(9, 9, (CURRENT_DATE - 4) + TIME '08:00', (CURRENT_DATE - 4) + TIME '08:08', true, (CURRENT_DATE - 4) + TIME '07:55'),
(7, 10, (CURRENT_DATE - 4) + TIME '18:20', (CURRENT_DATE - 4) + TIME '18:33', true, (CURRENT_DATE - 4) + TIME '18:15'),
(10, 11, (CURRENT_DATE - 5) + TIME '10:00', (CURRENT_DATE - 5) + TIME '10:14', true, (CURRENT_DATE - 5) + TIME '09:55'),
(8, 12, (CURRENT_DATE - 5) + TIME '19:30', (CURRENT_DATE - 5) + TIME '19:40', true, (CURRENT_DATE - 5) + TIME '19:25'),
(7, 13, (CURRENT_DATE - 6) + TIME '07:30', (CURRENT_DATE - 6) + TIME '07:40', true, (CURRENT_DATE - 6) + TIME '07:25'),
(9, 14, (CURRENT_DATE - 6) + TIME '12:15', (CURRENT_DATE - 6) + TIME '12:26', true, (CURRENT_DATE - 6) + TIME '12:10'),
(10, 15, (CURRENT_DATE - 7) + TIME '09:00', (CURRENT_DATE - 7) + TIME '09:18', true, (CURRENT_DATE - 7) + TIME '08:55'),
(8, 16, (CURRENT_DATE - 7) + TIME '15:00', (CURRENT_DATE - 7) + TIME '15:21', true, (CURRENT_DATE - 7) + TIME '14:55'),
(9, 17, (CURRENT_DATE - 8) + TIME '08:45', (CURRENT_DATE - 8) + TIME '08:53', true, (CURRENT_DATE - 8) + TIME '08:40'),
(7, 18, (CURRENT_DATE - 8) + TIME '17:30', (CURRENT_DATE - 8) + TIME '17:41', true, (CURRENT_DATE - 8) + TIME '17:25'),
(10, 19, (CURRENT_DATE - 9) + TIME '11:00', (CURRENT_DATE - 9) + TIME '11:12', true, (CURRENT_DATE - 9) + TIME '10:55'),
(8, 20, (CURRENT_DATE - 9) + TIME '20:00', (CURRENT_DATE - 9) + TIME '20:17', true, (CURRENT_DATE - 9) + TIME '19:55'),
(7, 21, (CURRENT_DATE - 10) + TIME '06:30', (CURRENT_DATE - 10) + TIME '06:36', true, (CURRENT_DATE - 10) + TIME '06:25'),
(9, 22, (CURRENT_DATE - 10) + TIME '13:00', (CURRENT_DATE - 10) + TIME '13:12', true, (CURRENT_DATE - 10) + TIME '12:55'),
(7, 23, (CURRENT_DATE - 11) + TIME '10:30', (CURRENT_DATE - 11) + TIME '10:45', true, (CURRENT_DATE - 11) + TIME '10:25'),
(10, 24, (CURRENT_DATE - 11) + TIME '16:00', (CURRENT_DATE - 11) + TIME '16:15', true, (CURRENT_DATE - 11) + TIME '15:55'),
(8, 25, (CURRENT_DATE - 12) + TIME '09:15', (CURRENT_DATE - 12) + TIME '09:28', true, (CURRENT_DATE - 12) + TIME '09:10'),
(7, 26, (CURRENT_DATE - 12) + TIME '18:45', (CURRENT_DATE - 12) + TIME '18:54', true, (CURRENT_DATE - 12) + TIME '18:40'),
(9, 27, (CURRENT_DATE - 14) + TIME '08:00', (CURRENT_DATE - 14) + TIME '08:11', true, (CURRENT_DATE - 14) + TIME '07:55'),
(10, 28, (CURRENT_DATE - 14) + TIME '14:30', (CURRENT_DATE - 14) + TIME '14:47', true, (CURRENT_DATE - 14) + TIME '14:25'),
(8, 29, (CURRENT_DATE - 15) + TIME '11:00', (CURRENT_DATE - 15) + TIME '11:18', true, (CURRENT_DATE - 15) + TIME '10:55'),
(7, 30, (CURRENT_DATE - 15) + TIME '19:00', (CURRENT_DATE - 15) + TIME '19:10', true, (CURRENT_DATE - 15) + TIME '18:55'),
(9, 31, (CURRENT_DATE - 17) + TIME '07:30', (CURRENT_DATE - 17) + TIME '07:43', true, (CURRENT_DATE - 17) + TIME '07:25'),
(10, 32, (CURRENT_DATE - 17) + TIME '16:00', (CURRENT_DATE - 17) + TIME '16:15', true, (CURRENT_DATE - 17) + TIME '15:55'),
(7, 33, (CURRENT_DATE - 18) + TIME '09:00', (CURRENT_DATE - 18) + TIME '09:08', true, (CURRENT_DATE - 18) + TIME '08:55'),
(8, 34, (CURRENT_DATE - 18) + TIME '20:30', (CURRENT_DATE - 18) + TIME '20:46', true, (CURRENT_DATE - 18) + TIME '20:25'),
(9, 35, (CURRENT_DATE - 20) + TIME '10:15', (CURRENT_DATE - 20) + TIME '10:25', true, (CURRENT_DATE - 20) + TIME '10:10'),
(10, 36, (CURRENT_DATE - 20) + TIME '15:45', (CURRENT_DATE - 20) + TIME '16:02', true, (CURRENT_DATE - 20) + TIME '15:40'),
(7, 37, (CURRENT_DATE - 22) + TIME '08:30', (CURRENT_DATE - 22) + TIME '08:41', true, (CURRENT_DATE - 22) + TIME '08:25'),
(9, 38, (CURRENT_DATE - 22) + TIME '17:00', (CURRENT_DATE - 22) + TIME '17:11', true, (CURRENT_DATE - 22) + TIME '16:55'),
(8, 39, (CURRENT_DATE - 25) + TIME '12:00', (CURRENT_DATE - 25) + TIME '12:12', true, (CURRENT_DATE - 25) + TIME '11:55'),
(7, 40, (CURRENT_DATE - 25) + TIME '18:30', (CURRENT_DATE - 25) + TIME '18:42', true, (CURRENT_DATE - 25) + TIME '18:25'),
(10, 41, (CURRENT_DATE - 28) + TIME '09:00', (CURRENT_DATE - 28) + TIME '09:16', true, (CURRENT_DATE - 28) + TIME '08:55'),
(8, 42, (CURRENT_DATE - 28) + TIME '14:00', (CURRENT_DATE - 28) + TIME '14:17', true, (CURRENT_DATE - 28) + TIME '13:55'),
(9, 43, (CURRENT_DATE - 29) + TIME '07:00', (CURRENT_DATE - 29) + TIME '07:07', true, (CURRENT_DATE - 29) + TIME '06:55'),
(7, 44, (CURRENT_DATE - 29) + TIME '16:30', (CURRENT_DATE - 29) + TIME '16:41', true, (CURRENT_DATE - 29) + TIME '16:25');

-- =============================================================================
-- 10. CONVERSATIONS & MESSAGES (chat between passenger and driver)
-- =============================================================================
INSERT INTO conversations (id, created_at) VALUES (1, NOW());
INSERT INTO conversation_participants (conversation_id, user_id) VALUES (1, 3), (1, 9);  -- Alice & Mike
INSERT INTO messages (conversation_id, sender_id, content, created_at) VALUES
(1, 3, 'Hi, I''m on my way to the pickup point.', NOW() - INTERVAL '5 minutes'),
(1, 9, 'Great, I''m waiting. See you soon!', NOW() - INTERVAL '4 minutes');

SELECT setval('conversations_id_seq', 1);

-- =============================================================================
-- 11. ACCOUNT_ACTIVATION_TOKENS (optional - for inactive users)
-- =============================================================================
-- Skipped - all seeded users are active. Add if testing activation flow.

-- =============================================================================
-- 12. INCONSISTENCY_REPORTS (optional - passenger reports during ride)
-- =============================================================================
-- Skipped - no reports in seed. Add if testing report flow.

-- =============================================================================
-- DONE. Summary:
-- - 2 admins, 4 passengers, 4 drivers (all password: password)
-- - 44 rides: 42 completed (for stats graphs), 1 scheduled, 1 in progress
-- - Rides spread across past ~30 days for graphed statistics (daily ride count, distance, revenue)
-- - Pricing for STANDARD, VAN, LUX
-- - Driver states, work logs, favorite routes, 1 conversation with messages
-- =============================================================================
