package com.example.blackcar.data.api.model;

public class WebRideTrackingResponse {
    private Long rideId;
    private Double vehicleLatitude;
    private Double vehicleLongitude;
    private Integer estimatedTimeToDestinationMinutes;
    private Double distanceToDestinationKm;
    private String status;
    private String rideStatus;
    private String nextStopName;
    private Integer nextStopEta;
    private String updatedAt;
    private String recordedAt;
    private VehicleInfo vehicle;

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public Double getVehicleLatitude() { return vehicleLatitude; }
    public void setVehicleLatitude(Double vehicleLatitude) { this.vehicleLatitude = vehicleLatitude; }

    public Double getVehicleLongitude() { return vehicleLongitude; }
    public void setVehicleLongitude(Double vehicleLongitude) { this.vehicleLongitude = vehicleLongitude; }

    public Integer getEstimatedTimeToDestinationMinutes() { return estimatedTimeToDestinationMinutes; }
    public void setEstimatedTimeToDestinationMinutes(Integer estimatedTimeToDestinationMinutes) { this.estimatedTimeToDestinationMinutes = estimatedTimeToDestinationMinutes; }

    public Double getDistanceToDestinationKm() { return distanceToDestinationKm; }
    public void setDistanceToDestinationKm(Double distanceToDestinationKm) { this.distanceToDestinationKm = distanceToDestinationKm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRideStatus() { return rideStatus; }
    public void setRideStatus(String rideStatus) { this.rideStatus = rideStatus; }

    public String getNextStopName() { return nextStopName; }
    public void setNextStopName(String nextStopName) { this.nextStopName = nextStopName; }

    public Integer getNextStopEta() { return nextStopEta; }
    public void setNextStopEta(Integer nextStopEta) { this.nextStopEta = nextStopEta; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getRecordedAt() { return recordedAt; }
    public void setRecordedAt(String recordedAt) { this.recordedAt = recordedAt; }

    public VehicleInfo getVehicle() { return vehicle; }
    public void setVehicle(VehicleInfo vehicle) { this.vehicle = vehicle; }

    public static class VehicleInfo {
        private Long id;
        private String type;
        private String licensePlate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLicensePlate() { return licensePlate; }
        public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    }
}
