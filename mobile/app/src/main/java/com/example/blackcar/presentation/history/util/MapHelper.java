package com.example.blackcar.presentation.history.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.example.blackcar.R;
import com.example.blackcar.data.api.model.LocationPoint;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for drawing routes and markers on OpenStreetMap (OSMDroid)
 * Similar to Leaflet map component used in frontend
 */
public class MapHelper {

    private final Context context;
    private final MapView mapView;

    public MapHelper(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
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
     * Clear all overlays from map
     */
    public void clearMap() {
        mapView.getOverlayManager().clear();
    }

    /**
     * Get marker icon based on type
     */
    private Drawable getMarkerIcon(MarkerType type) {
        try {
            int drawableId;
            switch (type) {
                case PICKUP:
                    drawableId = android.R.drawable.ic_menu_mylocation; // Green-ish default
                    break;
                case DROPOFF:
                    drawableId = android.R.drawable.ic_dialog_map; // Red-ish default
                    break;
                case STOP:
                    drawableId = android.R.drawable.ic_menu_compass; // Blue-ish default
                    break;
                default:
                    drawableId = android.R.drawable.ic_menu_mylocation;
            }
            return ContextCompat.getDrawable(context, drawableId);
        } catch (Exception e) {
            return null;
        }
    }

    public enum MarkerType {
        PICKUP,
        DROPOFF,
        STOP
    }
}
