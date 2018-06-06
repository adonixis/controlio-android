package ru.adonixis.controlio.network;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.util.Utils;

public class ServiceFactory {

    private static ControlioService controlioService;

    public static ControlioService getControlioService(final Context context) {
        if (controlioService == null || BuildConfig.DEBUG) {

            Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    if (Utils.isNetworkAvailable(context)) {
                        int maxAge = 60; // read from cache for 1 minute
                        request = request.newBuilder()
                                .header("Cache-Control", "public, max-age=" + maxAge)
                                .build();
                    } else {
                        int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
                        request = request.newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                                .build();
                    }
                    return chain.proceed(request);
                }
            };

            OkHttpClient client;

            //setup cache
            File httpCacheDirectory = new File(context.getCacheDir(), "responses");
            int cacheSize = 10 * 1024 * 1024; // 10 MiB
            Cache cache = new Cache(httpCacheDirectory, cacheSize);

            //add cache to the client
            client = new OkHttpClient.Builder()
                    .addInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
                    .cache(cache)
                    .build();
            if (!BuildConfig.DEBUG) {
                controlioService = createRetrofitService(ControlioService.class, BuildConfig.BASE_URL, client);
            } else {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                String apiUrl = settings.getString("preference_api_url", BuildConfig.BASE_URL);
                controlioService = createRetrofitService(ControlioService.class, apiUrl, client);
            }

        }
        return controlioService;
    }
    /**
     * Creates a retrofit service from an arbitrary class (clazz)
     * @param clazz Java interface of the retrofit service
     * @param baseUrl REST endpoint url
     * @return retrofit service with defined endpoint
     */
    private static <T> T createRetrofitService(final Class<T> clazz, final String baseUrl, final OkHttpClient client) {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        T service = retrofit.create(clazz);

        return service;
    }
}