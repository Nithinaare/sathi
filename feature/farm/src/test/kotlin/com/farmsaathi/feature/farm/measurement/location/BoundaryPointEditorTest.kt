package com.farmsaathi.feature.farm.measurement.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundaryPointEditorTest {
    @Test
    fun move_replacesSelectedCoordinateAndClearsGpsAccuracy() {
        val original = squarePoints()

        val result = BoundaryPointEditor.move(original, 1, 17.101, 78.101, 99L)

        assertTrue(result is BoundaryPointEditResult.Updated)
        val updated = (result as BoundaryPointEditResult.Updated).points
        assertEquals(17.101, updated[1].latitude, 0.0)
        assertEquals(78.101, updated[1].longitude, 0.0)
        assertNull(updated[1].accuracyMeters)
        assertEquals(99L, updated[1].capturedAtEpochMillis)
        assertEquals(3, updated.size)
    }

    @Test
    fun insertAfter_placesNewPointInBoundarySequence() {
        val original = squarePoints()

        val result = BoundaryPointEditor.insertAfter(original, 1, 17.105, 78.105, 120L)

        assertTrue(result is BoundaryPointEditResult.Updated)
        val updated = result as BoundaryPointEditResult.Updated
        assertEquals(2, updated.selectedIndex)
        assertEquals(4, updated.points.size)
        assertEquals(17.105, updated.points[2].latitude, 0.0)
        assertNull(updated.points[2].accuracyMeters)
    }

    @Test
    fun remove_preservesAtLeastThreePoints() {
        val original = squarePoints() + sample(17.11, 78.11)

        val result = BoundaryPointEditor.remove(original, 1)

        assertTrue(result is BoundaryPointEditResult.Updated)
        assertEquals(4, (result as BoundaryPointEditResult.Updated).points.size)
    }

    @Test
    fun remove_rejectsWhenOnlyThreePointsRemain() {
        val result = BoundaryPointEditor.remove(squarePoints(), 1)

        assertEquals(
            BoundaryPointEditResult.Rejected(BoundaryPointEditError.TOO_FEW_POINTS_TO_REMOVE),
            result,
        )
    }

    @Test
    fun move_rejectsInvalidLatitude() {
        val result = BoundaryPointEditor.move(squarePoints(), 0, 91.0, 78.0, 1L)

        assertEquals(BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_COORDINATE), result)
    }

    @Test
    fun insert_rejectsInvalidIndex() {
        val result = BoundaryPointEditor.insertAfter(squarePoints(), 9, 17.0, 78.0, 1L)

        assertEquals(BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_INDEX), result)
    }

    @Test
    fun move_rejectsNonFiniteLongitude() {
        val result = BoundaryPointEditor.move(squarePoints(), 0, 17.0, Double.NaN, 1L)

        assertEquals(BoundaryPointEditResult.Rejected(BoundaryPointEditError.INVALID_COORDINATE), result)
    }

    private fun squarePoints() = listOf(
        sample(17.100, 78.100),
        sample(17.100, 78.110),
        sample(17.110, 78.110),
    )

    private fun sample(latitude: Double, longitude: Double) = GpsLocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = 4.0,
        capturedAtEpochMillis = 10L,
    )
}
