package com.example.blackcar.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.example.blackcar.data.api.model.GeocodeResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Geocoding via Nominatim (OpenStreetMap).
 * Requires User-Agent header. Uses Serbia viewbox and country filter.
 */
public class GeocodingRepository {

    private static final String NOMINATIM_BASE = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "PekariApp/1.0 (Android; contact@pekari.app)";
    private static final String VIEWBOX_SERBIA = "19.7,45.3,19.9,45.2";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request original = chain.request();
                Request withHeaders = original.newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept-Language", "en")
                        .build();
                return chain.proceed(withHeaders);
            })
            .build();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface GeocodeCallback {
        void onSuccess(List<GeocodeResult> results);
        void onError(String message);
    }

    public interface ReverseGeocodeCallback {
        void onSuccess(GeocodeResult result);
        void onError(String message);
    }

    /**
     * Search addresses. Min 3 chars recommended. Debounce in caller.
     */
    public void searchAddress(String query, GeocodeCallback callback) {
        if (query == null || query.trim().length() < 3) {
            mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
            return;
        }

        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name());
                String url = NOMINATIM_BASE + "/search?q=" + encoded
                        + "&format=json&addressdetails=1&limit=8"
                        + "&countrycodes=rs"
                        + "&viewbox=" + VIEWBOX_SERBIA + "&bounded=1";

                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Geocoding failed"));
                        return;
                    }
                    String body = response.body().string();
                    List<GeocodeResult> results = parseSearchResponse(body);
                    mainHandler.post(() -> callback.onSuccess(results));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Geocoding failed"));
            }
        });
    }

    /**
     * Reverse geocode: coordinates to address
     */
    public void reverseGeocode(double latitude, double longitude, ReverseGeocodeCallback callback) {
        executor.execute(() -> {
            try {
                String url = NOMINATIM_BASE + "/reverse?lat=" + latitude + "&lon=" + longitude
                        + "&format=json";

                Request request = new Request.Builder().url(url).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onSuccess(new GeocodeResult(
                                String.format("%.6f, %.6f", latitude, longitude), latitude, longitude)));
                        return;
                    }
                    String body = response.body().string();
                    GeocodeResult result = parseReverseResponse(body, latitude, longitude);
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onSuccess(new GeocodeResult(
                        String.format("%.6f, %.6f", latitude, longitude), latitude, longitude)));
            }
        });
    }

    private List<GeocodeResult> parseSearchResponse(String json) throws Exception {
        List<GeocodeResult> results = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String displayName = obj.optString("display_name", "");
            double lat = obj.optDouble("lat", 0);
            double lon = obj.optDouble("lon", 0);
            if (!displayName.isEmpty()) {
                String shortName = buildShortAddress(obj.optJSONObject("address"));
                results.add(new GeocodeResult(
                        shortName != null && !shortName.isEmpty() ? shortName : displayName,
                        lat, lon));
            }
        }
        return results;
    }

    private String buildShortAddress(JSONObject address) {
        if (address == null) return null;
        StringBuilder sb = new StringBuilder();
        if (address.has("road")) {
            sb.append(address.optString("road"));
            if (address.has("house_number")) {
                sb.append(" ").append(address.optString("house_number"));
            }
        }
        String city = address.optString("city", address.optString("town", address.optString("village", "")));
        if (!city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (address.has("country") && sb.length() > 0) {
            sb.append(", ").append(address.optString("country"));
        }
        return sb.toString().trim();
    }

    private GeocodeResult parseReverseResponse(String json, double lat, double lon) throws Exception {
        JSONObject obj = new JSONObject(json);
        String displayName = obj.optString("display_name", String.format("%.6f, %.6f", lat, lon));
        return new GeocodeResult(displayName, lat, lon);
    }
}
