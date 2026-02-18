package com.example.blackcar.presentation.history.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.LocationPoint;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for drawing routes and markers on OpenStreetMap (OSMDroid)
 * Similar to Leaflet map component used in frontend.
 * Uses app colors: pickup green (#22c55e), dropoff red (#ef4444), stops blue (#3b82f6)
 */
public class MapHelper {

    private final Context context;
    private final MapView mapView;
    private final List<Marker> vehicleMarkers = new ArrayList<>();

    public interface OnMapClickListener {
        void onMapClick(double latitude, double longitude);
    }

    private OnMapClickListener mapClickListener;
    private MapEventsOverlay mapEventsOverlay;

    public MapHelper(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
    }

    /**
     * Set listener for map taps. When set, map taps will emit coordinates.
     * Pass null to disable.
     */
    public void setOnMapClickListener(OnMapClickListener listener) {
        this.mapClickListener = listener;
    }

    /**
     * Draw a polyline route on the map
     * @param points List of location points
     * @param color Color of the route line
     */
    public void drawRoute(List<LocationPoint> points, int color) {
        if (points == null || points.size() < 2) {
            return;
        }

        List<GeoPoint> geoPoints = new ArrayList<>();
        for (LocationPoint point : points) {
            if (point.getLatitude() != null && point.getLongitude() != null) {
                geoPoints.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
            }
        }

        if (geoPoints.size() < 2) {
            return;
        }

        Polyline line = new Polyline();
        line.setPoints(geoPoints);
        line.setColor(color);
        line.setWidth(5f);
        mapView.getOverlayManager().add(line);
    }

    /**
     * Add a marker for pickup location (green)
     */
    public Marker addPickupMarker(double latitude, double longitude, String title) {
        return addMarker(latitude, longitude, title, MarkerType.PICKUP);
    }

    /**
     * Add a marker for dropoff location (red)
     */
    public Marker addDropoffMarker(double latitude, double longitude, String title) {
        return addMarker(latitude, longitude, title, MarkerType.DROPOFF);
    }

    /**
     * Add a marker for a stop (blue)
     */
    public Marker addStopMarker(double latitude, double longitude, String title) {
        return addMarker(latitude, longitude, title, MarkerType.STOP);
    }

    /**
     * Add a generic marker with custom type
     */
    private Marker addMarker(double latitude, double longitude, String title, MarkerType type) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);

        // Set marker icon based on type
        Drawable icon = getMarkerIcon(type);
        if (icon != null) {
            marker.setIcon(icon);
        }

        mapView.getOverlayManager().add(marker);
        return marker;
    }

    /**
     * Fit map bounds to show all points
     */
    public void fitBounds(List<LocationPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;

        for (LocationPoint point : points) {
            if (point.getLatitude() != null && point.getLongitude() != null) {
                minLat = Math.min(minLat, point.getLatitude());
                maxLat = Math.max(maxLat, point.getLatitude());
                minLon = Math.min(minLon, point.getLongitude());
                maxLon = Math.max(maxLon, point.getLongitude());
            }
        }

        if (minLat != Double.MAX_VALUE) {
            GeoPoint center = new GeoPoint((minLat + maxLat) / 2, (minLon + maxLon) / 2);
            mapView.getController().setCenter(center);

            // Calculate appropriate zoom level based on bounds
            double latDiff = maxLat - minLat;
            double lonDiff = maxLon - minLon;
            double maxDiff = Math.max(latDiff, lonDiff);

            double zoomLevel = 15; // default
            if (maxDiff > 0.1) zoomLevel = 11;
            else if (maxDiff > 0.05) zoomLevel = 12;
            else if (maxDiff > 0.01) zoomLevel = 14;

            mapView.getController().setZoom(zoomLevel);
        }
    }

    /**
     * Clear all overlays from map (markers, polylines).
     * Preserves and re-adds the map tap overlay so tap-to-select-location keeps working.
     */
    public void clearMap() {
        MapEventsOverlay tapOverlay = this.mapEventsOverlay;
        mapView.getOverlayManager().clear();
        if (tapOverlay != null) {
            mapView.getOverlays().add(0, tapOverlay);
        }
    }

    /**
     * Get marker icon based on type
     * Uses app colors: pickup green, dropoff red, stop blue
     */
    private Drawable getMarkerIcon(MarkerType type) {
        int drawableId;
        switch (type) {
            case PICKUP:
                drawableId = R.drawable.marker_pickup;
                break;
            case DROPOFF:
                drawableId = R.drawable.marker_dropoff;
                break;
            case STOP:
                drawableId = R.drawable.marker_stop;
                break;
            case VEHICLE_AVAILABLE:
                // Reuse pickup (green) icon for available vehicles
                drawableId = R.drawable.marker_pickup;
                break;
            case VEHICLE_BUSY:
                // Reuse dropoff (red) icon for occupied vehicles
                drawableId = R.drawable.marker_dropoff;
                break;
            default:
                drawableId = R.drawable.marker_pickup;
        }
        Drawable d = ContextCompat.getDrawable(context, drawableId);
        if (d != null) {
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }
        return d;
    }

    /**
     * Draw a polyline route using app accent color (#3b82f6)
     */
    public void drawRoute(List<LocationPoint> points) {
        drawRoute(points, Color.parseColor("#3b82f6"));
    }

    /**
     * Enable map click handling for setting location (e.g. pickup/dropoff).
     * Call from fragment after map is ready. Call setOnMapClickListener first.
     */
    public void setupMapTapOverlay() {
        if (mapEventsOverlay != null) return;
        mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (mapClickListener != null) {
                    mapClickListener.onMapClick(p.getLatitude(), p.getLongitude());
                }
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    /**
     * Remove map tap overlay (e.g. when form is locked)
     */
    public void removeMapTapOverlay() {
        if (mapEventsOverlay != null) {
            mapView.getOverlays().remove(mapEventsOverlay);
            mapEventsOverlay = null;
        }
    }

    public enum MarkerType {
        PICKUP,
        DROPOFF,
        STOP,
        VEHICLE_AVAILABLE,
        VEHICLE_BUSY
    }

    // ---- Vehicle markers helpers ----
    public void clearVehicleMarkers() {
        for (Marker m : vehicleMarkers) {
            mapView.getOverlayManager().remove(m);
        }
        vehicleMarkers.clear();
        mapView.invalidate();
    }

    public Marker addVehicleMarker(double latitude, double longitude, String title, boolean busy) {
        MarkerType type = busy ? MarkerType.VEHICLE_BUSY : MarkerType.VEHICLE_AVAILABLE;
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(title);
        Drawable icon = getMarkerIcon(type);
        if (icon != null) marker.setIcon(icon);
        mapView.getOverlayManager().add(marker);
        vehicleMarkers.add(marker);
        return marker;
    }
}
