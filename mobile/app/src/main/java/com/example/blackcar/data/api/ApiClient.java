package com.example.blackcar.data.api;

import com.example.blackcar.BuildConfig;
import com.example.blackcar.data.api.service.AuthApiService;
import com.example.blackcar.data.api.service.ProfileApiService;
import com.example.blackcar.data.session.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;
    private static AuthApiService authApiService;
    private static ProfileApiService profileApiService;

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = SessionManager.getToken();
                        if (token != null && !token.trim().isEmpty()) {
                            Request authed = original.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(authed);
                        }
                        return chain.proceed(original);
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
}
