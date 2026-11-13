package com.mindgate.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.mindgate.BuildConfig
import com.mindgate.data.local.PreferencesManager
import com.mindgate.data.local.SessionManager
import com.mindgate.data.local.database.MindGateDatabase
import com.mindgate.data.remote.api.MindGateApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager =
        SessionManager(context)

    @Provides @Singleton
    fun providePreferences(session: SessionManager): PreferencesManager =
        PreferencesManager(session)

    @Provides @Singleton @Named("BaseUrlInterceptor")
    fun provideBaseUrlInterceptor(session: SessionManager): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val savedBaseUrlString = runBlocking { session.apiBaseUrl.first() }
        
        // 1. Resolve effective Base URL
        val effectiveBaseUrl = savedBaseUrlString.toHttpUrlOrNull() ?: BuildConfig.API_BASE_URL.toHttpUrlOrNull()!!
        val retrofitBaseUrl = BuildConfig.API_BASE_URL.toHttpUrlOrNull()!!

        // 2. Map original request URL to new base URL
        val newUrl = if (effectiveBaseUrl.host != retrofitBaseUrl.host || effectiveBaseUrl.port != retrofitBaseUrl.port || effectiveBaseUrl.scheme != retrofitBaseUrl.scheme) {
            val originalUrl = originalRequest.url
            val path = originalUrl.encodedPath.removePrefix(retrofitBaseUrl.encodedPath)
            
            effectiveBaseUrl.newBuilder()
                .encodedPath(effectiveBaseUrl.encodedPath.removeSuffix("/") + "/" + path.removePrefix("/"))
                .query(originalUrl.query)
                .build()
        } else {
            originalRequest.url
        }

        var request = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        if (BuildConfig.DEBUG) {
            android.util.Log.d("MINDGATE_NETWORK", "Request to: ${request.url}")
        }

        // 3. Add Auth Token if available
        val token = runBlocking { session.authToken.first() }
        if (!token.isNullOrBlank()) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(request)

        // 4. Handle Auth failures
        if (response.code == 401) {
            // Only clear session if we were actually authenticated (avoid loops on login fails)
            if (!token.isNullOrBlank()) {
                runBlocking { session.clearSession() }
            }
        }

        response
    }

    @Provides @Singleton
    fun provideOkHttp(@Named("BaseUrlInterceptor") baseUrlInterceptor: Interceptor): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(baseUrlInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): MindGateApiService =
        retrofit.create(MindGateApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MindGateDatabase =
        Room.databaseBuilder(context, MindGateDatabase::class.java, "mindgate.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: MindGateDatabase) = db.userDao()
    @Provides fun provideAssessmentDao(db: MindGateDatabase) = db.assessmentDao()
    @Provides fun provideAppLimitDao(db: MindGateDatabase) = db.appLimitDao()
    @Provides fun provideUsageDao(db: MindGateDatabase) = db.usageDao()
    @Provides fun provideSettingsDao(db: MindGateDatabase) = db.settingsDao()
    @Provides fun provideExtensionGrantDao(db: MindGateDatabase) = db.extensionGrantDao()
    @Provides fun provideParentDao(db: MindGateDatabase) = db.parentDao()
    @Provides fun provideUsageLimitEventDao(db: MindGateDatabase) = db.usageLimitEventDao()
    @Provides fun provideBehaviorSignalDao(db: MindGateDatabase) = db.behaviorSignalDao()
    @Provides fun provideCheckInDao(db: MindGateDatabase) = db.checkInDao()
    @Provides fun provideReflectionDao(db: MindGateDatabase) = db.reflectionDao()
    @Provides fun provideStudySessionDao(db: MindGateDatabase) = db.studySessionDao()
    @Provides fun provideLimitWarningDao(db: MindGateDatabase) = db.limitWarningDao()
}
