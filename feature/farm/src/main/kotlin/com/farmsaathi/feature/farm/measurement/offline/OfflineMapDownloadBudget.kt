package com.farmsaathi.feature.farm.measurement.offline

/** Conservative app-side guardrails below OpenFreeMap's published 3,000-tile / 50 MB cap. */
object OfflineMapDownloadBudget {
    const val MAX_COMPLETED_TILES: Long = 2_700L
    const val MAX_COMPLETED_BYTES: Long = 40L * 1024L * 1024L

    fun isLimitReached(completedTiles: Long, completedBytes: Long): Boolean =
        completedTiles >= MAX_COMPLETED_TILES || completedBytes >= MAX_COMPLETED_BYTES
}
