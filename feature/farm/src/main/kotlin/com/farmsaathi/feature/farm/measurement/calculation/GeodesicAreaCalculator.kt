package com.farmsaathi.feature.farm.measurement.calculation

import com.farmsaathi.core.model.AreaConverter
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.PolygonArea
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A validated latitude/longitude observation. Coordinates are WGS84 degrees. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    val capturedAtEpochMillis: Long,
)

data class AreaResult(
    val areaSquareMeters: Double,
    val areaMicroAcres: Long,
    val perimeterMeters: Double,
    val pointCount: Int,
)

sealed interface AreaCalculationResult {
    data class Success(val value: AreaResult) : AreaCalculationResult

    data class Invalid(val reason: AreaCalculationError) : AreaCalculationResult
}

enum class AreaCalculationError {
    TOO_FEW_POINTS,
    INVALID_COORDINATE,
    SELF_INTERSECTING_BOUNDARY,
    BOUNDARY_NOT_CLOSED,
    ZERO_OR_INVALID_AREA,
    TOO_MANY_POINTS,
}

/**
 * Calculates a polygon on the WGS84 ellipsoid using GeographicLib's geodesic planimeter.
 * A local equirectangular projection is used only to detect self-intersections, never for area.
 */
class GeodesicAreaCalculator {
    fun calculate(input: List<GeoPoint>): AreaCalculationResult {
        if (input.any { !it.latitude.isFinite() || !it.longitude.isFinite() || it.latitude !in -90.0..90.0 || it.longitude !in -180.0..180.0 }) {
            return AreaCalculationResult.Invalid(AreaCalculationError.INVALID_COORDINATE)
        }
        if (input.size > MAXIMUM_BOUNDARY_POINTS) {
            return AreaCalculationResult.Invalid(AreaCalculationError.TOO_MANY_POINTS)
        }
        val explicitlyClosed = input.size > 1 &&
            haversineMeters(
                input.first().latitude,
                input.first().longitude,
                input.last().latitude,
                input.last().longitude,
            ) < DUPLICATE_POINT_TOLERANCE_METERS
        val points = normalizeBoundary(input)
        if (points.size < MINIMUM_POLYGON_POINTS) {
            return AreaCalculationResult.Invalid(AreaCalculationError.TOO_FEW_POINTS)
        }
        val meanAccuracy = points.mapNotNull { it.accuracyMeters?.takeIf { it.isFinite() } }.average().takeIf { it.isFinite() } ?: 0.0
        val closingGap = if (explicitlyClosed) 0.0 else distanceMeters(points.last(), points.first())
        // Do not silently bridge a large gap. The tolerance scales with observed accuracy,
        // but is bounded so a poor GPS fix cannot justify closing a distant boundary.
        val maximumClosingGap = max(MINIMUM_CLOSING_GAP_METERS, (meanAccuracy * CLOSING_GAP_ACCURACY_MULTIPLIER).coerceAtMost(MAXIMUM_CLOSING_GAP_METERS))
        if (closingGap > maximumClosingGap) {
            return AreaCalculationResult.Invalid(AreaCalculationError.BOUNDARY_NOT_CLOSED)
        }
        if (hasSelfIntersection(points)) {
            return AreaCalculationResult.Invalid(AreaCalculationError.SELF_INTERSECTING_BOUNDARY)
        }

        return runCatching {
            val polygon = PolygonArea(Geodesic.WGS84, false)
            points.forEach { point -> polygon.AddPoint(point.latitude, point.longitude) }
            // sign=true returns the smaller signed area instead of the complementary earth area.
            val calculated = polygon.Compute(false, true)
            val area = abs(calculated.area)
            val perimeter = calculated.perimeter
            if (!area.isFinite() || !perimeter.isFinite() || area < MINIMUM_AREA_SQUARE_METERS || perimeter <= 0.0) {
                return AreaCalculationResult.Invalid(AreaCalculationError.ZERO_OR_INVALID_AREA)
            }
            val acres = BigDecimal.valueOf(area)
                .divide(SQUARE_METERS_PER_ACRE, 18, RoundingMode.HALF_UP)
            val microAcres = AreaConverter.fromDecimalAcres(acres)
            if (microAcres <= 0L) {
                AreaCalculationResult.Invalid(AreaCalculationError.ZERO_OR_INVALID_AREA)
            } else {
                AreaCalculationResult.Success(
                    AreaResult(
                        areaSquareMeters = area,
                        areaMicroAcres = microAcres,
                        perimeterMeters = perimeter,
                        pointCount = points.size,
                    ),
                )
            }
        }.getOrElse {
            AreaCalculationResult.Invalid(AreaCalculationError.ZERO_OR_INVALID_AREA)
        }
    }

    /** A rectangular manual estimate entered in feet. */
    fun calculateRectangleFromFeet(lengthFeet: Double, widthFeet: Double): AreaCalculationResult {
        if (!lengthFeet.isFinite() || !widthFeet.isFinite() || lengthFeet <= 0.0 || widthFeet <= 0.0) {
            return AreaCalculationResult.Invalid(AreaCalculationError.ZERO_OR_INVALID_AREA)
        }
        val areaSquareMeters = lengthFeet * widthFeet * SQUARE_METERS_PER_SQUARE_FOOT
        val perimeterMeters = (lengthFeet + widthFeet) * 2.0 * METERS_PER_FOOT
        if (!areaSquareMeters.isFinite() || !perimeterMeters.isFinite()) {
            return AreaCalculationResult.Invalid(AreaCalculationError.ZERO_OR_INVALID_AREA)
        }
        val acres = BigDecimal.valueOf(areaSquareMeters)
            .divide(SQUARE_METERS_PER_ACRE, 18, RoundingMode.HALF_UP)
        val microAcres = AreaConverter.fromDecimalAcres(acres)
        if (microAcres <= 0L) return AreaCalculationResult.Invalid(AreaCalculationError.ZERO_OR_INVALID_AREA)
        return AreaCalculationResult.Success(
            AreaResult(
                areaSquareMeters = areaSquareMeters,
                areaMicroAcres = microAcres,
                perimeterMeters = perimeterMeters,
                pointCount = 0,
            ),
        )
    }

    private fun normalizeBoundary(input: List<GeoPoint>): List<GeoPoint> {
        if (input.isEmpty()) return emptyList()
        val cleaned = ArrayList<GeoPoint>(input.size)
        input.forEach { point ->
            val previous = cleaned.lastOrNull()
            if (previous == null || haversineMeters(previous.latitude, previous.longitude, point.latitude, point.longitude) >= DUPLICATE_POINT_TOLERANCE_METERS) {
                cleaned += point
            }
        }
        if (cleaned.size > 1) {
            val first = cleaned.first()
            val last = cleaned.last()
            if (haversineMeters(first.latitude, first.longitude, last.latitude, last.longitude) < DUPLICATE_POINT_TOLERANCE_METERS) {
                cleaned.removeAt(cleaned.lastIndex)
            }
        }
        return cleaned
    }

    private fun hasSelfIntersection(points: List<GeoPoint>): Boolean {
        val meanLatitudeRadians = Math.toRadians(points.map { it.latitude }.average())
        val originLongitude = points.first().longitude
        val projected = points.map { point ->
            val deltaLongitude = normalizeLongitudeDelta(point.longitude - originLongitude)
            Point2D(
                x = Math.toRadians(deltaLongitude) * cos(meanLatitudeRadians),
                y = Math.toRadians(point.latitude),
            )
        }
        val count = projected.size
        for (firstEdge in 0 until count) {
            val firstNext = (firstEdge + 1) % count
            for (secondEdge in firstEdge + 1 until count) {
                val secondNext = (secondEdge + 1) % count
                val edgesAreAdjacent =
                    firstEdge == secondEdge || firstNext == secondEdge || secondNext == firstEdge
                if (edgesAreAdjacent) continue
                if (segmentsIntersect(projected[firstEdge], projected[firstNext], projected[secondEdge], projected[secondNext])) {
                    return true
                }
            }
        }
        return false
    }

    private fun segmentsIntersect(a: Point2D, b: Point2D, c: Point2D, d: Point2D): Boolean {
        val o1 = orientation(a, b, c)
        val o2 = orientation(a, b, d)
        val o3 = orientation(c, d, a)
        val o4 = orientation(c, d, b)
        if (((o1 > EPSILON && o2 < -EPSILON) || (o1 < -EPSILON && o2 > EPSILON)) &&
            ((o3 > EPSILON && o4 < -EPSILON) || (o3 < -EPSILON && o4 > EPSILON))
        ) {
            return true
        }
        return (abs(o1) <= EPSILON && onSegment(a, b, c)) ||
            (abs(o2) <= EPSILON && onSegment(a, b, d)) ||
            (abs(o3) <= EPSILON && onSegment(c, d, a)) ||
            (abs(o4) <= EPSILON && onSegment(c, d, b))
    }

    private fun orientation(a: Point2D, b: Point2D, c: Point2D): Double =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    private fun onSegment(a: Point2D, b: Point2D, p: Point2D): Boolean =
        p.x >= min(a.x, b.x) - EPSILON && p.x <= max(a.x, b.x) + EPSILON &&
            p.y >= min(a.y, b.y) - EPSILON && p.y <= max(a.y, b.y) + EPSILON

    private fun distanceMeters(first: GeoPoint, second: GeoPoint): Double =
        haversineMeters(first.latitude, first.longitude, second.latitude, second.longitude)

    private fun normalizeLongitudeDelta(value: Double): Double {
        var result = value
        while (result > 180.0) result -= 360.0
        while (result < -180.0) result += 360.0
        return result
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val latDelta = Math.toRadians(lat2 - lat1)
        val lonDelta = Math.toRadians(normalizeLongitudeDelta(lon2 - lon1))
        val a = sin(latDelta / 2.0) * sin(latDelta / 2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(lonDelta / 2.0) * sin(lonDelta / 2.0)
        return 2.0 * EARTH_MEAN_RADIUS_METERS * kotlin.math.asin(kotlin.math.sqrt(a.coerceIn(0.0, 1.0)))
    }

    private data class Point2D(val x: Double, val y: Double)

    private companion object {
        const val MINIMUM_POLYGON_POINTS = 3
        const val MAXIMUM_BOUNDARY_POINTS = 5_000
        const val MINIMUM_AREA_SQUARE_METERS = 1.0
        const val DUPLICATE_POINT_TOLERANCE_METERS = 0.25
        const val MINIMUM_CLOSING_GAP_METERS = 5.0
        const val CLOSING_GAP_ACCURACY_MULTIPLIER = 1.5
        const val MAXIMUM_CLOSING_GAP_METERS = 10.0
        const val METERS_PER_FOOT = 0.3048
        const val SQUARE_METERS_PER_SQUARE_FOOT = 0.09290304
        const val EARTH_MEAN_RADIUS_METERS = 6_371_008.8
        const val EPSILON = 1e-12
        val SQUARE_METERS_PER_ACRE: BigDecimal = BigDecimal("4046.8564224")
    }
}
