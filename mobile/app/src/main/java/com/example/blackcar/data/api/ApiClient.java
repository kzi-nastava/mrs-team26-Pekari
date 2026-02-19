package com.example.blackcar.data.api;

import android.content.Context;

import com.example.blackcar.BuildConfig;
import com.example.blackcar.data.api.service.AdminApiService;
import com.example.blackcar.data.api.service.AuthApiService;
import com.example.blackcar.data.api.service.NotificationApiService;
import com.example.blackcar.data.api.service.ProfileApiService;
import com.example.blackcar.data.api.service.RideApiService;
import com.example.blackcar.data.api.service.DriversApiService;
import com.example.blackcar.data.api.service.ChatApiService;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

import com.example.blackcar.data.auth.TokenManager;

public class ApiClient {

    private static Retrofit retrofit;
    private static AuthApiService authApiService;
    private static ProfileApiService profileApiService;
    private static RideApiService rideApiService;
    private static DriversApiService driversApiService;
    private static ChatApiService chatApiService;
    private static AdminApiService adminApiService;
    private static NotificationApiService notificationApiService;
    private static Context appContext;
    private static SimpleCookieJar cookieJar;

    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
            try {
                if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(appContext);
                    Log.i("ApiClient", "[DEBUG_LOG] FirebaseApp initialized manually in ApiClient.init()");
                }
            } catch (Exception e) {
                Log.e("ApiClient", "[DEBUG_LOG] Failed to initialize FirebaseApp in ApiClient.init()", e);
            }
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
        notificationApiService = null;
    }

    public static boolean hasAuthToken() {
        if (appContext == null) return false;
        String jwt = TokenManager.getInstance(appContext).getToken();
        return jwt != null && !jwt.isEmpty();
    }

    private static Retrofit getRetrofit() {
        if (appContext == null) {
            Log.e("ApiClient", "ApiClient not initialized! Call ApiClient.init(context) first.");
            // We should not throw here to avoid app crash, but the interceptor will likely fail
        }

        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            cookieJar = new SimpleCookieJar(appContext);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    // Attach Authorization header with JWT if available
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            if (appContext == null) {
                                Log.e("ApiClient", "[DEBUG_LOG] Interceptor: appContext is null, cannot check for JWT");
                                return chain.proceed(original);
                            }

                            String jwt = TokenManager.getInstance(appContext).getToken();
                            if (jwt != null && !jwt.isEmpty()) {
                                Log.i("ApiClient", "[DEBUG_LOG] Interceptor: Attaching Authorization header (JWT starts with: " + jwt.substring(0, Math.min(jwt.length(), 10)) + ")");
                                Request authed = original.newBuilder()
                                        .addHeader("Authorization", "Bearer " + jwt)
                                        .build();
                                return chain.proceed(authed);
                            } else {
                                Log.i("ApiClient", "[DEBUG_LOG] Interceptor: No JWT found in TokenManager");
                            }
                            return chain.proceed(original);
                        }
                    })
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

    public static NotificationApiService getNotificationService() {
        if (notificationApiService == null) {
            notificationApiService = getRetrofit().create(NotificationApiService.class);
        }
        return notificationApiService;
    }
}
