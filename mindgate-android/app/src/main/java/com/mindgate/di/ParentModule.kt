package com.mindgate.di

import com.mindgate.data.local.database.MindGateDatabase
import com.mindgate.data.remote.api.ParentApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ParentModule {
    @Provides @Singleton
    fun provideParentApi(retrofit: Retrofit): ParentApiService =
        retrofit.create(ParentApiService::class.java)
}
