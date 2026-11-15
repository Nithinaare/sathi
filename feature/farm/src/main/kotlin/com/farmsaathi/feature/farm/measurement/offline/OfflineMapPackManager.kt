package com.farmsaathi.feature.farm.measurement.offline

import android.content.Context
import androidx.annotation.MainThread
import com.farmsaathi.feature.farm.measurement.location.GpsLocationSample
import com.farmsaathi.feature.farm.measurement.ui.MapLibreNetworkConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

sealed interface OfflineMapDownloadState {
    data object Idle : OfflineMapDownloadState
    data class Downloading(
        val plotId: Long,
        val completedTiles: Long,
        val downloadedBytes: Long,
        val warning: String? = null,
    ) : OfflineMapDownloadState
    data class Saved(
        val plotId: Long,
        val regionId: Long,
        val completedTiles: Long,
        val downloadedBytes: Long,
    ) : OfflineMapDownloadState
    data class LimitReached(val plotId: Long) : OfflineMapDownloadState
    data class Failed(val reason: String, val plotId: Long? = null) : OfflineMapDownloadState
}

/**
 * Saves a bounded OpenFreeMap street-map region around the measured plot.
 * The region is stored by MapLibre in its offline database and is reused automatically
 * when the same style URL is displayed without a network connection.
 *
 * Satellite imagery is intentionally not included in offline packs. Its provider terms and
 * large raster payloads need a separate explicit review.
 */
@Singleton
class OfflineMapPackManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    init {
        MapLibreNetworkConfiguration.install()
        MapLibre.getInstance(context.applicationContext)
    }

    private val offlineManager: OfflineManager by lazy { OfflineManager.getInstance(context) }
    private val mutableState = MutableStateFlow<OfflineMapDownloadState>(OfflineMapDownloadState.Idle)
    val state: StateFlow<OfflineMapDownloadState> = mutableState.asStateFlow()

    private var activeRegion: OfflineRegion? = null
    private var activeObserver: OfflineRegion.OfflineRegionObserver? = null
    private var activePlotId: Long? = null
    private var pendingCreate = false
    private var terminal = AtomicBoolean(false)

    @MainThread
    fun refreshForPlot(plotId: Long) {
        // Do not let a second plot's screen inspect/delete regions while a download is active.
        // The ViewModel filters the shared state by plotId.
        if (activePlotId != null) return
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val matching = offlineRegions.orEmpty().filter { region ->
                    runCatching {
                        val metadata = JSONObject(region.metadata.toString(Charsets.UTF_8))
                        metadata.optString("app") == "FarmSaathi" &&
                            metadata.optLong("plotId") == plotId &&
                            metadata.optString("styleUrl") == STREET_STYLE_URL
                    }.getOrDefault(false)
                }
                if (matching.isEmpty()) {
                    mutableState.value = OfflineMapDownloadState.Idle
                    return
                }

                val completed = mutableListOf<Pair<OfflineRegion, OfflineRegionStatus>>()
                val lock = Any()
                var statusError: String? = null
                val remaining = AtomicInteger(matching.size)
                fun finishOne() {
                    if (remaining.decrementAndGet() != 0) return
                    val newest = synchronized(lock) { completed.maxByOrNull { it.first.id } }
                    if (newest != null) {
                        mutableState.value = OfflineMapDownloadState.Saved(
                            plotId = plotId,
                            regionId = newest.first.id,
                            completedTiles = newest.second.completedTileCount,
                            downloadedBytes = newest.second.completedResourceSize,
                        )
                        completed.filter { it.first.id != newest.first.id }.forEach { (older, _) ->
                            older.setDownloadState(OfflineRegion.STATE_INACTIVE)
                            older.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                                override fun onDelete() = Unit
                                override fun onError(error: String) = Unit
                            })
                        }
                    } else {
                        val failure = synchronized(lock) { statusError }
                        mutableState.value = failure?.let { OfflineMapDownloadState.Failed(it, plotId) }
                            ?: OfflineMapDownloadState.Idle
                    }
                }

                matching.forEach { region ->
                    region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            if (status?.isComplete == true) {
                                synchronized(lock) { completed += region to status }
                                finishOne()
                            } else if (status != null) {
                                // Delete a partial region left behind by a process death. A partial
                                // pack is not a reliable offline map and must not be labelled Saved.
                                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                                    override fun onDelete() = finishOne()
                                    override fun onError(error: String) {
                                        synchronized(lock) { statusError = "Could not remove an interrupted map download: $error" }
                                        finishOne()
                                    }
                                })
                            } else {
                                finishOne()
                            }
                        }

                        override fun onError(error: String?) {
                            synchronized(lock) { statusError = error ?: "Could not read the saved map state." }
                            finishOne()
                        }
                    })
                }
            }

            override fun onError(error: String) {
                mutableState.value = OfflineMapDownloadState.Failed(error, plotId)
            }
        })
    }

    @MainThread
    fun downloadForBoundary(plotId: Long, points: List<GpsLocationSample>) {
        if (points.size < 3) {
            mutableState.value = OfflineMapDownloadState.Failed("At least three boundary points are needed.", plotId)
            return
        }
        if (activeRegion != null || pendingCreate || mutableState.value is OfflineMapDownloadState.Downloading) {
            mutableState.value = OfflineMapDownloadState.Failed("A map download is already running.", plotId)
            return
        }
        val bounds = createPaddedBounds(points)
        if (bounds == null) {
            mutableState.value = OfflineMapDownloadState.Failed("The boundary coordinates are invalid.", plotId)
            return
        }

        terminal = AtomicBoolean(false)
        pendingCreate = true
        activePlotId = plotId
        mutableState.value = OfflineMapDownloadState.Downloading(plotId, 0L, 0L)
        val definition = OfflineTilePyramidRegionDefinition(
            STREET_STYLE_URL,
            bounds,
            MIN_ZOOM,
            MAX_ZOOM,
            context.resources.displayMetrics.density.coerceIn(1f, 2f),
            false,
        )
        val metadata = JSONObject()
            .put("app", "FarmSaathi")
            .put("kind", "plot-street-map")
            .put("plotId", plotId)
            .put("createdAtEpochMillis", System.currentTimeMillis())
            .put("styleUrl", STREET_STYLE_URL)
            .toString()
            .toByteArray(Charsets.UTF_8)

        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(region: OfflineRegion) {
                    pendingCreate = false
                    if (terminal.get()) {
                        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                            override fun onDelete() {
                                activePlotId = null
                                mutableState.value = OfflineMapDownloadState.Idle
                            }

                            override fun onError(error: String) {
                                activePlotId = null
                                mutableState.value = OfflineMapDownloadState.Failed(
                                    "The cancelled map request could not be cleaned up: $error",
                                    plotId,
                                )
                            }
                        })
                        return
                    }
                    activeRegion = region
                    val observer = object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            val tiles = status.completedTileCount
                            val bytes = status.completedResourceSize
                            if (OfflineMapDownloadBudget.isLimitReached(tiles, bytes)) {
                                stopAndDeleteRegion(region)
                                return
                            }
                            if (status.isComplete) {
                                if (terminal.compareAndSet(false, true)) {
                                    region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                    mutableState.value = OfflineMapDownloadState.Saved(
                                        plotId = plotId,
                                        regionId = region.id,
                                        completedTiles = tiles,
                                        downloadedBytes = bytes,
                                    )
                                    clearActive(region)
                                    removeOlderRegionsForPlot(plotId = plotId, keepRegionId = region.id)
                                }
                            } else if (!terminal.get()) {
                                mutableState.value = OfflineMapDownloadState.Downloading(plotId, tiles, bytes)
                            }
                        }

                        override fun onError(error: OfflineRegionError) {
                            if (terminal.get()) return
                            val current = mutableState.value as? OfflineMapDownloadState.Downloading
                            mutableState.value = OfflineMapDownloadState.Downloading(
                                plotId = plotId,
                                completedTiles = current?.completedTiles ?: 0L,
                                downloadedBytes = current?.downloadedBytes ?: 0L,
                                warning = error.message,
                            )
                        }

                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            stopAndDeleteRegion(region)
                        }
                    }
                    activeObserver = observer
                    region.setObserver(observer)
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) {
                    pendingCreate = false
                    mutableState.value = if (terminal.get()) {
                        // Cancellation may win the race before MapLibre creates the region.
                        // Clear the transient Downloading state when creation eventually fails.
                        OfflineMapDownloadState.Idle
                    } else {
                        OfflineMapDownloadState.Failed(error, plotId)
                    }
                    activeRegion = null
                    activeObserver = null
                    activePlotId = null
                }
            },
        )
    }

    @MainThread
    fun cancelDownload() {
        terminal.set(true)
        val region = activeRegion
        if (region == null) {
            // The createOfflineRegion callback may still be pending. Keep the request slot
            // occupied until its callback deletes the region, so a retry cannot race it.
            mutableState.value = OfflineMapDownloadState.Downloading(
                plotId = activePlotId ?: -1L,
                completedTiles = 0L,
                downloadedBytes = 0L,
                warning = "Cancel requested; waiting for map storage to finish creating the region.",
            )
            return
        }
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                clearActive(region)
                mutableState.value = OfflineMapDownloadState.Idle
            }

            override fun onError(error: String) {
                val plotId = activePlotId
                clearActive(region)
                mutableState.value = OfflineMapDownloadState.Failed("Could not remove partial map: $error", plotId)
            }
        })
    }

    private fun stopAndDeleteRegion(region: OfflineRegion) {
        if (!terminal.compareAndSet(false, true)) return
        region.setDownloadState(OfflineRegion.STATE_INACTIVE)
        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                val plotId = activePlotId ?: -1L
                clearActive(region)
                mutableState.value = OfflineMapDownloadState.LimitReached(plotId)
            }

            override fun onError(error: String) {
                val plotId = activePlotId
                clearActive(region)
                mutableState.value = OfflineMapDownloadState.Failed(
                    "The provider-safe limit was reached and the partial pack could not be removed: $error",
                    plotId,
                )
            }
        })
    }


    /** Keep only the newest completed street-map pack for this plot after a replacement succeeds. */
    private fun removeOlderRegionsForPlot(plotId: Long, keepRegionId: Long) {
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                offlineRegions.orEmpty()
                    .filter { region ->
                        region.id != keepRegionId && runCatching {
                            val metadata = JSONObject(region.metadata.toString(Charsets.UTF_8))
                            metadata.optString("app") == "FarmSaathi" &&
                                metadata.optLong("plotId") == plotId &&
                                metadata.optString("styleUrl") == STREET_STYLE_URL
                        }.getOrDefault(false)
                    }
                    .forEach { olderRegion ->
                        olderRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        olderRegion.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                            override fun onDelete() = Unit
                            override fun onError(error: String) = Unit
                        })
                    }
            }

            override fun onError(error: String) = Unit
        })
    }

    private fun clearActive(region: OfflineRegion) {
        if (activeRegion?.id == region.id) {
            region.setObserver(null)
            activeRegion = null
            activeObserver = null
            activePlotId = null
        }
    }

    private fun createPaddedBounds(points: List<GpsLocationSample>): LatLngBounds? {
        if (points.any { !it.latitude.isFinite() || !it.longitude.isFinite() || it.latitude !in -90.0..90.0 || it.longitude !in -180.0..180.0 }) {
            return null
        }
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latPadding = max((maxLat - minLat) * PADDING_RATIO, MIN_PADDING_DEGREES)
        val lonPadding = max((maxLon - minLon) * PADDING_RATIO, MIN_PADDING_DEGREES)
        return runCatching {
            LatLngBounds.Builder()
                .include(LatLng((maxLat + latPadding).coerceAtMost(89.9), (maxLon + lonPadding).coerceAtMost(179.9)))
                .include(LatLng((minLat - latPadding).coerceAtLeast(-89.9), (minLon - lonPadding).coerceAtLeast(-179.9)))
                .build()
        }.getOrNull()
    }

    private companion object {
        const val STREET_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
        const val MIN_ZOOM = 12.0
        const val MAX_ZOOM = 16.0
        const val PADDING_RATIO = 0.15
        const val MIN_PADDING_DEGREES = 0.0006
    }
}
