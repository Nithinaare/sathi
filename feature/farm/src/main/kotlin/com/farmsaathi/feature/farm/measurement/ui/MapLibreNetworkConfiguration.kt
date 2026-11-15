package com.farmsaathi.feature.farm.measurement.ui

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.maplibre.android.module.http.HttpRequestUtil

/**
 * Shared MapLibre HTTP configuration.
 * OpenFreeMap asks offline-download clients to identify themselves and limits downloads
 * to four parallel requests. Keep this configuration global because MapLibre's HTTP client
 * is shared across MapView instances and OfflineManager downloads.
 */
internal object MapLibreNetworkConfiguration {
    private val lock = Any()
    @Volatile private var installed = false

    fun install() {
        if (installed) return
        synchronized(lock) {
            if (installed) return
            val dispatcher = Dispatcher().apply {
                maxRequests = MAX_PARALLEL_REQUESTS
                maxRequestsPerHost = MAX_PARALLEL_REQUESTS
            }
            val client = OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val updated = if (request.url.host == OPENFREEMAP_HOST) {
                        request.newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build()
                    } else {
                        request
                    }
                    chain.proceed(updated)
                }
                .build()
            HttpRequestUtil.setOkHttpClient(client)
            installed = true
        }
    }

    private const val MAX_PARALLEL_REQUESTS = 4
    private const val OPENFREEMAP_HOST = "tiles.openfreemap.org"
    private const val USER_AGENT = "FarmSaathi/1.0 (Android; offline field maps)"
}
