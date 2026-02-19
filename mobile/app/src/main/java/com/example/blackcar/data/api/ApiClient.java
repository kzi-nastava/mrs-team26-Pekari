package com.example.blackcar.data.api;

import android.content.Context;

import com.example.blackcar.BuildConfig;
import com.example.blackcar.data.api.service.AdminApiService;
import com.example.blackcar.data.api.service.AuthApiService;
import com.example.blackcar.data.api.service.ProfileApiService;
import com.example.blackcar.data.api.service.RideApiService;
import com.example.blackcar.data.api.service.DriversApiService;
import com.example.blackcar.data.api.service.ChatApiService;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;
    private static AuthApiService authApiService;
    private static ProfileApiService profileApiService;
    private static RideApiService rideApiService;
    private static DriversApiService driversApiService;
    private static ChatApiService chatApiService;
    private static AdminApiService adminApiService;
    private static Context appContext;
    private static SimpleCookieJar cookieJar;

    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    public static void clearCookies() {
        if (cookieJar != null) {
            cookieJar.clear();
        }
        // Reset retrofit to force new client without old cookies
        retrofit = null;
        authApiService = null;
        profileApiService = null;
        rideApiService = null;
        driversApiService = null;
        chatApiService = null;
        adminApiService = null;
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            cookieJar = new SimpleCookieJar(appContext);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .addInterceptor(logging)
                    .build();

            String baseUrl = BuildConfig.API_BASE_URL;
            if (!baseUrl.endsWith("/")) {
                baseUrl = baseUrl + "/";
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApiService getAuthService() {
        if (authApiService == null) {
            authApiService = getRetrofit().create(AuthApiService.class);
        }
        return authApiService;
    }

    public static ProfileApiService getProfileService() {
        if (profileApiService == null) {
            profileApiService = getRetrofit().create(ProfileApiService.class);
        }
        return profileApiService;
    }
    public static RideApiService getRideService() {
        if (rideApiService == null) {
            rideApiService = getRetrofit().create(RideApiService.class);
        }
        return rideApiService;
    }
    public static DriversApiService getDriversService() {
        if (driversApiService == null) {
            driversApiService = getRetrofit().create(DriversApiService.class);
        }
        return driversApiService;
    }

    public static ChatApiService getChatService() {
        if (chatApiService == null) {
            chatApiService = getRetrofit().create(ChatApiService.class);
        }
        return chatApiService;
    }

    public static AdminApiService getAdminService() {
        if (adminApiService == null) {
            adminApiService = getRetrofit().create(AdminApiService.class);
        }
        return adminApiService;
    }
}
