package com.farmsaathi.feature.farm.measurement.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.farmsaathi.feature.farm.measurement.location.GpsLocationSample
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * A real MapLibre map with a local GeoJSON overlay.
 *
 * Map tiles are a visual aid and may require internet access. GPS capture and the geodesic
 * calculator remain independent of map tile availability. Point coordinates are never sent
 * to the map provider as a feature request; they are rendered locally in the map style.
 */
@Composable
internal fun MapLibreBoundaryMap(
    points: List<GpsLocationSample>,
    modifier: Modifier = Modifier,
    useTelugu: Boolean,
    closeBoundary: Boolean,
    editMode: BoundaryMapEditMode = BoundaryMapEditMode.NONE,
    selectedPointIndex: Int? = null,
    onMapPointSelected: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var baseMapStyle by remember { mutableStateOf(BoundaryBaseMapStyle.STREET) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var isStyleReady by remember { mutableStateOf(false) }
    var mapLoadFailed by remember { mutableStateOf(false) }

    val latestPoints by rememberUpdatedState(points)
    val latestCloseBoundary by rememberUpdatedState(closeBoundary)
    val latestSelectedPointIndex by rememberUpdatedState(selectedPointIndex)
    val latestEditMode by rememberUpdatedState(editMode)
    val latestOnMapPointSelected by rememberUpdatedState(onMapPointSelected)

    val mapView = remember(context) {
        MapLibreNetworkConfiguration.install()
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(null)
            // The measurement screen is hosted in a vertically scrolling Compose column.
            // Let MapLibre receive map gestures instead of the parent scroll container.
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN,
                    android.view.MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    val mapClickListener = remember {
        MapLibreMap.OnMapClickListener { coordinate ->
            if (latestEditMode != BoundaryMapEditMode.NONE) {
                latestOnMapPointSelected(coordinate.latitude, coordinate.longitude)
                true
            } else {
                false
            }
        }
    }

    DisposableEffect(mapView, lifecycleOwner) {
        mapView.getMapAsync { readyMap ->
            mapInstance = readyMap
            readyMap.addOnMapClickListener(mapClickListener)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Compose can create this MapView after the Activity has already reached RESUMED.
        // Synchronize immediately instead of waiting for lifecycle events that already occurred.
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        val failureListener = MapView.OnDidFailLoadingMapListener { _ -> mapLoadFailed = true }
        mapView.addOnDidFailLoadingMapListener(failureListener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.removeOnDidFailLoadingMapListener(failureListener)
            mapInstance?.removeOnMapClickListener(mapClickListener)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
            mapInstance = null
        }
    }

    LaunchedEffect(mapInstance, baseMapStyle) {
        val map = mapInstance ?: return@LaunchedEffect
        isStyleReady = false
        mapLoadFailed = false
        val builder = when (baseMapStyle) {
            BoundaryBaseMapStyle.STREET -> Style.Builder().fromUri(STREET_STYLE_URL)
            BoundaryBaseMapStyle.SATELLITE -> Style.Builder().fromJson(SATELLITE_STYLE_JSON)
        }
        map.setStyle(builder) { style ->
            installBoundaryLayers(style, latestPoints, latestCloseBoundary, latestSelectedPointIndex)
            isStyleReady = true
        }
    }

    LaunchedEffect(isStyleReady, points, closeBoundary, selectedPointIndex) {
        if (!isStyleReady) return@LaunchedEffect
        val style = mapInstance?.style ?: return@LaunchedEffect
        val source = style.getSource(BOUNDARY_SOURCE_ID) as? GeoJsonSource ?: return@LaunchedEffect
        source.setGeoJson(buildBoundaryFeatureCollection(points, closeBoundary, selectedPointIndex))
    }

    LaunchedEffect(isStyleReady, points.size, closeBoundary, baseMapStyle) {
        val map = mapInstance ?: return@LaunchedEffect
        if (!isStyleReady || points.isEmpty()) return@LaunchedEffect
        if (closeBoundary && points.size >= 2) {
            val builder = LatLngBounds.Builder()
            points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            runCatching {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 52))
            }
        } else {
            val last = points.last()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 18.0))
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(18.dp))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        if (mapLoadFailed) {
            BoundaryMapOfflineFallback(
                points = points,
                useTelugu = useTelugu,
                closeBoundary = closeBoundary,
                selectedPointIndex = selectedPointIndex,
                editMode = editMode,
                onMapPointSelected = latestOnMapPointSelected,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (!isStyleReady) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (useTelugu) "మ్యాప్ తెరుస్తున్నాం…" else "Loading map…",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapStylePill(
                label = if (useTelugu) "మ్యాప్" else "Map",
                selected = baseMapStyle == BoundaryBaseMapStyle.STREET,
                onClick = { baseMapStyle = BoundaryBaseMapStyle.STREET },
            )
            MapStylePill(
                label = if (useTelugu) "శాటిలైట్" else "Satellite",
                selected = baseMapStyle == BoundaryBaseMapStyle.SATELLITE,
                onClick = { baseMapStyle = BoundaryBaseMapStyle.SATELLITE },
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(7.dp),
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = when (baseMapStyle) {
                    BoundaryBaseMapStyle.STREET -> "© OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors"
                    BoundaryBaseMapStyle.SATELLITE -> "Source: Esri, Vantor, Earthstar Geographics, and the GIS User Community"
                },
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF334438),
            )
        }

        if (editMode != BoundaryMapEditMode.NONE) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp),
                color = Color(0xFF183F2B),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    if (useTelugu) "మ్యాప్‌పై కొత్త స్థానాన్ని తాకండి" else "Tap the map to place the point",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private enum class BoundaryBaseMapStyle { STREET, SATELLITE }

enum class BoundaryMapEditMode { NONE, MOVE_SELECTED, INSERT_AFTER_SELECTED }

@Composable
private fun MapStylePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF245A3A) else Color.White.copy(alpha = 0.94f),
        contentColor = if (selected) Color.White else Color(0xFF24412E),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.padding(start = 4.dp),
        shadowElevation = 2.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun installBoundaryLayers(
    style: Style,
    points: List<GpsLocationSample>,
    closeBoundary: Boolean,
    selectedPointIndex: Int?,
) {
    style.addSource(GeoJsonSource(BOUNDARY_SOURCE_ID, buildBoundaryFeatureCollection(points, closeBoundary, selectedPointIndex)))
    style.addLayer(
        FillLayer(BOUNDARY_FILL_LAYER_ID, BOUNDARY_SOURCE_ID).withProperties(
            PropertyFactory.fillColor("#3B8B4B"),
            PropertyFactory.fillOpacity(0.24f),
            PropertyFactory.fillOutlineColor("#1E6334"),
        ),
    )
    style.addLayer(
        LineLayer(BOUNDARY_LINE_LAYER_ID, BOUNDARY_SOURCE_ID).withProperties(
            PropertyFactory.lineColor("#155C32"),
            PropertyFactory.lineWidth(4.0f),
            PropertyFactory.lineOpacity(0.96f),
            PropertyFactory.lineCap("round"),
            PropertyFactory.lineJoin("round"),
        ),
    )
    style.addLayer(
        CircleLayer(BOUNDARY_POINTS_LAYER_ID, BOUNDARY_SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(5.5f),
            PropertyFactory.circleColor("#245A3A"),
            PropertyFactory.circleStrokeColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.0f),
        ),
    )
    style.addLayer(
        CircleLayer(BOUNDARY_SELECTED_POINT_LAYER_ID, BOUNDARY_SOURCE_ID)
            .withFilter(Expression.eq(Expression.get("selected"), Expression.literal(true)))
            .withProperties(
                PropertyFactory.circleRadius(9.0f),
                PropertyFactory.circleColor("#E3A92C"),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
                PropertyFactory.circleStrokeWidth(2.5f),
            ),
    )
}

private fun buildBoundaryFeatureCollection(
    points: List<GpsLocationSample>,
    closeBoundary: Boolean,
    selectedPointIndex: Int? = null,
): String {
    val features = mutableListOf<String>()
    if (points.size >= 2) {
        val lineCoordinates = points.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        features += """{"type":"Feature","properties":{"kind":"boundary-line"},"geometry":{"type":"LineString","coordinates":[$lineCoordinates]}}"""
    }
    if (closeBoundary && points.size >= 3) {
        val ringPoints = points + points.first()
        val ringCoordinates = ringPoints.joinToString(",") { "[${it.longitude},${it.latitude}]" }
        features += """{"type":"Feature","properties":{"kind":"boundary-area"},"geometry":{"type":"Polygon","coordinates":[[$ringCoordinates]]}}"""
    }
    points.forEachIndexed { index, point ->
        val selected = index == selectedPointIndex
        features += """{"type":"Feature","properties":{"kind":"boundary-point","index":$index,"selected":$selected},"geometry":{"type":"Point","coordinates":[${point.longitude},${point.latitude}]}}"""
    }
    return """{"type":"FeatureCollection","features":[${features.joinToString(",")}] }"""
}

@Composable
private fun BoundaryMapOfflineFallback(
    points: List<GpsLocationSample>,
    useTelugu: Boolean,
    closeBoundary: Boolean,
    selectedPointIndex: Int?,
    editMode: BoundaryMapEditMode,
    onMapPointSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOnMapPointSelected by rememberUpdatedState(onMapPointSelected)
    Column(
        modifier = modifier.background(Color(0xFFEAF0E2)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (useTelugu) "మ్యాప్ అందుబాటులో లేదు — మీ కొలత సురక్షితంగా ఉంది" else "Map unavailable — your measurement is still available",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF36563B),
        )
        Text(
            if (editMode == BoundaryMapEditMode.NONE) {
                if (useTelugu) "ఇంటర్నెట్ వచ్చినప్పుడు మ్యాప్ చూపబడుతుంది." else "The map will load when internet is available."
            } else {
                if (useTelugu) "ఆకార నమూనాపై తాకి సుమారు కొత్త స్థానం పెట్టండి." else "Tap the sketch to place an approximate point."
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF637365),
        )
        if (editMode != BoundaryMapEditMode.NONE) {
            Text(
                if (useTelugu) "ఇది నిజమైన భౌగోళిక మ్యాప్ కాదు." else "This is a schematic preview, not a geographic map.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A6730),
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(14.dp)
                .pointerInput(points, editMode) {
                    detectTapGestures { offset ->
                        if (editMode == BoundaryMapEditMode.NONE || points.size < 3) return@detectTapGestures
                        val minLatitude = points.minOf { it.latitude }
                        val maxLatitude = points.maxOf { it.latitude }
                        val minLongitude = points.minOf { it.longitude }
                        val maxLongitude = points.maxOf { it.longitude }
                        val latitudeSpan = (maxLatitude - minLatitude).coerceAtLeast(0.00002)
                        val longitudeSpan = (maxLongitude - minLongitude).coerceAtLeast(0.00002)
                        val paddingRatio = 0.12
                        val paddedMinLatitude = minLatitude - latitudeSpan * paddingRatio
                        val paddedMaxLatitude = maxLatitude + latitudeSpan * paddingRatio
                        val paddedMinLongitude = minLongitude - longitudeSpan * paddingRatio
                        val paddedMaxLongitude = maxLongitude + longitudeSpan * paddingRatio
                        val normalizedX = (offset.x / size.width).coerceIn(0f, 1f).toDouble()
                        val normalizedY = (offset.y / size.height).coerceIn(0f, 1f).toDouble()
                        val longitude = paddedMinLongitude + normalizedX * (paddedMaxLongitude - paddedMinLongitude)
                        val latitude = paddedMaxLatitude - normalizedY * (paddedMaxLatitude - paddedMinLatitude)
                        latestOnMapPointSelected(latitude, longitude)
                    }
                },
        ) {
            if (points.isEmpty()) return@Canvas
            val minLatitude = points.minOf { it.latitude }
            val maxLatitude = points.maxOf { it.latitude }
            val minLongitude = points.minOf { it.longitude }
            val maxLongitude = points.maxOf { it.longitude }
            val latitudeSpan = (maxLatitude - minLatitude).coerceAtLeast(0.00002)
            val longitudeSpan = (maxLongitude - minLongitude).coerceAtLeast(0.00002)
            val latitudePadding = latitudeSpan * 0.12
            val longitudePadding = longitudeSpan * 0.12
            val projected = points.map {
                androidx.compose.ui.geometry.Offset(
                    ((it.longitude - (minLongitude - longitudePadding)) / (longitudeSpan + longitudePadding * 2)).toFloat() * size.width,
                    ((maxLatitude + latitudePadding - it.latitude) / (latitudeSpan + latitudePadding * 2)).toFloat() * size.height,
                )
            }
            val path = Path().apply {
                moveTo(projected.first().x, projected.first().y)
                projected.drop(1).forEach { lineTo(it.x, it.y) }
                if (closeBoundary && projected.size >= 3) close()
            }
            if (closeBoundary && projected.size >= 3) {
                drawPath(path, Color(0x443B8B4B), style = Fill)
            }
            drawPath(path, Color(0xFF245A3A), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            projected.forEachIndexed { index, position ->
                val selected = index == selectedPointIndex
                drawCircle(
                    if (selected) Color(0xFFE3A92C) else Color(0xFF245A3A),
                    radius = if (selected) 9.dp.toPx() else 6.dp.toPx(),
                    center = position,
                )
                drawCircle(Color.White, radius = 2.dp.toPx(), center = position)
            }
        }
    }
}

private const val BOUNDARY_SOURCE_ID = "farm-saathi-boundary-source"
private const val BOUNDARY_FILL_LAYER_ID = "farm-saathi-boundary-fill"
private const val BOUNDARY_LINE_LAYER_ID = "farm-saathi-boundary-line"
private const val BOUNDARY_POINTS_LAYER_ID = "farm-saathi-boundary-points"
private const val BOUNDARY_SELECTED_POINT_LAYER_ID = "farm-saathi-boundary-selected-point"
private const val STREET_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Esri World Imagery public basemap. Visible source attribution is required. */
private const val SATELLITE_STYLE_JSON = """
{
  "version": 8,
  "name": "Farm Saathi Satellite",
  "sources": {
    "farm-saathi-esri-imagery": {
      "type": "raster",
      "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
      "tileSize": 256,
      "attribution": "Source: Esri, Vantor, Earthstar Geographics, and the GIS User Community"
    }
  },
  "layers": [
    {
      "id": "farm-saathi-esri-imagery-layer",
      "type": "raster",
      "source": "farm-saathi-esri-imagery",
      "minzoom": 0,
      "maxzoom": 19,
      "paint": { "raster-opacity": 1.0 }
    }
  ]
}
"""
