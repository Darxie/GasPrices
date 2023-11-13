package cz.feldis.gasprices

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import cz.feldis.gasprices.BuildConfig

object ServiceBuilder {
    private const val BASE_URL = "https://data.statistics.sk/"

    private fun getRetrofit() = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        // (Optional) Add a logging interceptor for debugging
        .client(OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                addInterceptor(loggingInterceptor)
            }
        }.build())
        .build()

    val apiService: ApiService = getRetrofit().create(ApiService::class.java)
}
