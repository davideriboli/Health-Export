package com.healthexport.di

import com.healthexport.data.healthconnect.HealthConnectManager
import com.healthexport.data.healthconnect.HealthConnectRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the Health Connect layer.
 *
 * [HealthConnectManager] and [HealthConnectRepository] are both @Singleton and
 * @Inject-annotated, so Hilt can construct them automatically — this module is
 * empty for now but keeps the DI structure consistent and ready for future
 * bindings (e.g. a fake manager for tests).
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule
