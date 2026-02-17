package com.example.blackcar.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class SimpleCookieJar implements CookieJar {
    private static final String COOKIE_PREFS = "app_cookies";
    private final SharedPreferences prefs;
    private final Set<Cookie> cookieStore = new HashSet<>();

    public SimpleCookieJar(Context context) {
        prefs = context.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie newCookie : cookies) {
            cookieStore.removeIf(existing ->
                    existing.name().equals(newCookie.name()) &&
                    existing.domain().equals(newCookie.domain()) &&
                    existing.path().equals(newCookie.path()));
            cookieStore.add(newCookie);
        }
        saveToPrefs();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> validCookies = new ArrayList<>();
        List<Cookie> expiredCookies = new ArrayList<>();

        for (Cookie cookie : cookieStore) {
            if (cookie.expiresAt() < System.currentTimeMillis()) {
                expiredCookies.add(cookie);
            } else if (cookie.matches(url)) {
                validCookies.add(cookie);
            }
        }

        if (!expiredCookies.isEmpty()) {
            cookieStore.removeAll(expiredCookies);
            saveToPrefs();
        }

        return validCookies;
    }

    public void clear() {
        cookieStore.clear();
        prefs.edit().clear().apply();
    }

    private void saveToPrefs() {
        Set<String> cookieStrings = new HashSet<>();
        for (Cookie cookie : cookieStore) {
            cookieStrings.add(serializeCookie(cookie));
        }
        prefs.edit().putStringSet("cookies", cookieStrings).apply();
    }

    private void loadFromPrefs() {
        Set<String> cookieStrings = prefs.getStringSet("cookies", new HashSet<>());
        for (String cookieString : cookieStrings) {
            Cookie cookie = deserializeCookie(cookieString);
            if (cookie != null) {
                cookieStore.add(cookie);
            }
        }
    }

    private String serializeCookie(Cookie cookie) {
        return cookie.name() + "=" + cookie.value() +
                "|domain=" + cookie.domain() +
                "|path=" + cookie.path() +
                "|expires=" + cookie.expiresAt() +
                "|secure=" + cookie.secure() +
                "|httpOnly=" + cookie.httpOnly();
    }

    private Cookie deserializeCookie(String cookieString) {
        try {
            String[] parts = cookieString.split("\\|");
            String[] nameValue = parts[0].split("=", 2);

            Cookie.Builder builder = new Cookie.Builder()
                    .name(nameValue[0])
                    .value(nameValue.length > 1 ? nameValue[1] : "");

            for (int i = 1; i < parts.length; i++) {
                String[] kv = parts[i].split("=", 2);
                switch (kv[0]) {
                    case "domain":
                        builder.domain(kv[1]);
                        break;
                    case "path":
                        builder.path(kv[1]);
                        break;
                    case "expires":
                        builder.expiresAt(Long.parseLong(kv[1]));
                        break;
                    case "secure":
                        if (Boolean.parseBoolean(kv[1])) builder.secure();
                        break;
                    case "httpOnly":
                        if (Boolean.parseBoolean(kv[1])) builder.httpOnly();
                        break;
                }
            }
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }
}
