package com.mindgate.di

import com.mindgate.data.local.SessionManager
import com.mindgate.data.local.database.MindGateDatabase
import com.mindgate.util.UsageStatsHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MindGateEntryPoint {
    fun database(): MindGateDatabase
    fun sessionManager(): SessionManager
    fun usageStatsHelper(): UsageStatsHelper
    fun parentRepository(): com.mindgate.data.repository.ParentRepository
    fun appInfoHelper(): com.mindgate.util.AppInfoHelper
}
