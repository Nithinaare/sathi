package com.farmsaathi.core.common.repository

import com.farmsaathi.core.model.CurrentKarteInfo
import com.farmsaathi.core.model.LunarDayAlert
import com.farmsaathi.core.model.TeluguCalendarDay
import com.farmsaathi.core.model.TeluguKarte
import kotlinx.coroutines.flow.Flow

interface TeluguCalendarRepository {
    fun observeDay(epochDay: Long): Flow<TeluguCalendarDay?>

    suspend fun getDay(epochDay: Long): TeluguCalendarDay?

    fun observeMonth(
        year: Int,
        month: Int,
    ): Flow<List<TeluguCalendarDay>>

    suspend fun getMonthDays(
        year: Int,
        month: Int,
    ): List<TeluguCalendarDay>

    fun observeCurrentKarte(epochDay: Long): Flow<CurrentKarteInfo>

    suspend fun getCurrentKarte(epochDay: Long): CurrentKarteInfo

    fun observeTodayAlert(epochDay: Long): Flow<LunarDayAlert>

    suspend fun getTodayAlert(epochDay: Long): LunarDayAlert

    fun observeAllKartis(): Flow<List<TeluguKarte>>
}
