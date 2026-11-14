package com.farmsaathi.feature.farm.measurement.location

import com.farmsaathi.feature.farm.measurement.calculation.GeodesicAreaCalculator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GpsLocationBindingModule {
    @Binds
    @Singleton
    abstract fun bindGpsLocationDataSource(implementation: AndroidGpsLocationDataSource): GpsLocationDataSource
}

@Module
@InstallIn(SingletonComponent::class)
object LandMeasurementCalculationModule {
    @Provides
    @Singleton
    fun provideGeodesicAreaCalculator(): GeodesicAreaCalculator = GeodesicAreaCalculator()

    @Provides
    @Singleton
    fun provideGpsPointAcceptancePolicy(): GpsPointAcceptancePolicy = GpsPointAcceptancePolicy()
}
