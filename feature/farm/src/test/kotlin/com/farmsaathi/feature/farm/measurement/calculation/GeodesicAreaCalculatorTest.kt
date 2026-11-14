package com.farmsaathi.feature.farm.measurement.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeodesicAreaCalculatorTest {
    private val calculator = GeodesicAreaCalculator()

    @Test
    fun `geodesic square at equator matches GeographicLib reference area`() {
        val result = calculator.calculate(squareAtEquator() + squareAtEquator().first())

        assertTrue(result is AreaCalculationResult.Success)
        val value = (result as AreaCalculationResult.Success).value
        // Reference generated independently with GeographicLib 2.1 Python for the same WGS84 vertices.
        assertEquals(12_309.072079001264, value.areaSquareMeters, 0.01)
        assertEquals(443.7875332131202, value.perimeterMeters, 0.01)
        assertEquals(4, value.pointCount)
        assertTrue(value.areaMicroAcres > 0L)
    }

    @Test
    fun `polygon orientation does not change unsigned field area`() {
        val clockwiseVertices = squareAtEquator().reversed()
        val clockwise = calculator.calculate(clockwiseVertices + clockwiseVertices.first()) as AreaCalculationResult.Success
        val counterClockwiseVertices = squareAtEquator()
        val counterClockwise = calculator.calculate(counterClockwiseVertices + counterClockwiseVertices.first()) as AreaCalculationResult.Success

        assertEquals(counterClockwise.value.areaSquareMeters, clockwise.value.areaSquareMeters, 0.001)
    }

    @Test
    fun `explicit repeated closing point is normalized`() {
        val points = squareAtEquator() + squareAtEquator().first()

        val result = calculator.calculate(points) as AreaCalculationResult.Success

        assertEquals(4, result.value.pointCount)
        assertEquals(12_309.072079001264, result.value.areaSquareMeters, 0.01)
    }

    @Test
    fun `exact repeated starting point closes the polygon`() {
        val vertices = listOf(
            point(0.0, 0.0),
            point(0.0, 0.001),
            point(0.001, 0.001),
            point(0.001, 0.0),
        )

        val result = calculator.calculate(vertices + vertices.first())

        assertTrue(result is AreaCalculationResult.Success)
        assertEquals(12_309.072079001264, (result as AreaCalculationResult.Success).value.areaSquareMeters, 0.01)
    }

    @Test
    fun `too few distinct points are rejected`() {
        val result = calculator.calculate(squareAtEquator().take(2))

        assertEquals(AreaCalculationError.TOO_FEW_POINTS, (result as AreaCalculationResult.Invalid).reason)
    }

    @Test
    fun `self intersecting bow tie is rejected`() {
        val bowTie = listOf(
            point(0.0, 0.0),
            point(0.001, 0.001),
            point(0.0, 0.001),
            point(0.001, 0.0),
        )

        val result = calculator.calculate(bowTie + bowTie.first())

        assertEquals(AreaCalculationError.SELF_INTERSECTING_BOUNDARY, (result as AreaCalculationResult.Invalid).reason)
    }

    @Test
    fun `invalid latitude is rejected`() {
        val points = squareAtEquator().toMutableList().apply { this[1] = point(91.0, 0.001) }

        val result = calculator.calculate(points)

        assertEquals(AreaCalculationError.INVALID_COORDINATE, (result as AreaCalculationResult.Invalid).reason)
    }

    @Test
    fun `boundary that ends far from the starting point is rejected`() {
        val openBoundary = squareAtEquator().dropLast(1)

        val result = calculator.calculate(openBoundary)

        assertEquals(AreaCalculationError.BOUNDARY_NOT_CLOSED, (result as AreaCalculationResult.Invalid).reason)
    }

    @Test
    fun `polygon with excessive point count is rejected before quadratic validation`() {
        val tooMany = List(5_001) { index -> point(0.0, index * 0.000001) }

        val result = calculator.calculate(tooMany)

        assertEquals(AreaCalculationError.TOO_MANY_POINTS, (result as AreaCalculationResult.Invalid).reason)
    }

    @Test
    fun `manual 100 by 100 feet rectangle converts to square metres`() {
        val result = calculator.calculateRectangleFromFeet(100.0, 100.0)

        assertTrue(result is AreaCalculationResult.Success)
        assertEquals(929.0304, (result as AreaCalculationResult.Success).value.areaSquareMeters, 0.000001)
    }

    @Test
    fun `660 by 66 feet converts to one acre`() {
        val result = calculator.calculateRectangleFromFeet(660.0, 66.0) as AreaCalculationResult.Success

        assertEquals(4_046.8564224, result.value.areaSquareMeters, 0.000001)
        assertEquals(1_000_000L, result.value.areaMicroAcres)
    }

    @Test
    fun `manual dimensions must be positive finite numbers`() {
        assertFalse(calculator.calculateRectangleFromFeet(0.0, 100.0) is AreaCalculationResult.Success)
        assertFalse(calculator.calculateRectangleFromFeet(-1.0, 100.0) is AreaCalculationResult.Success)
        assertFalse(calculator.calculateRectangleFromFeet(Double.NaN, 100.0) is AreaCalculationResult.Success)
        assertFalse(calculator.calculateRectangleFromFeet(Double.POSITIVE_INFINITY, 100.0) is AreaCalculationResult.Success)
    }

    private fun squareAtEquator() = listOf(
        point(0.0, 0.0),
        point(0.0, 0.001),
        point(0.001, 0.001),
        point(0.001, 0.0),
    )

    private fun point(latitude: Double, longitude: Double) =
        GeoPoint(latitude, longitude, accuracyMeters = 3.0, capturedAtEpochMillis = 1_700_000_000_000L)
}
