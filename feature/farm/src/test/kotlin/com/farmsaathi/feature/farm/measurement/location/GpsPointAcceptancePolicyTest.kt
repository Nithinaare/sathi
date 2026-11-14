package com.farmsaathi.feature.farm.measurement.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsPointAcceptancePolicyTest {
    private val policy = GpsPointAcceptancePolicy()
    private val now = 1_700_000_100_000L

    @Test
    fun `fresh accurate first fix is accepted`() {
        val result = policy.evaluate(sample(17.0, 78.0, now), null, now)

        assertEquals(GpsFixDecision.Accept, result)
    }

    @Test
    fun `poor accuracy fix is rejected`() {
        val result = policy.evaluate(sample(17.0, 78.0, now, accuracy = 35.0), null, now)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.MISSING_OR_POOR_ACCURACY), result)
    }

    @Test
    fun `stale fix is rejected`() {
        val result = policy.evaluate(sample(17.0, 78.0, now - 60_000L), null, now)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.STALE_FIX), result)
    }

    @Test
    fun `future timestamp beyond allowed skew is rejected`() {
        val candidate = sample(17.0, 78.0, now + 10_000L)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.STALE_FIX), policy.evaluate(candidate, null, now))
    }

    @Test
    fun `invalid longitude is rejected`() {
        val candidate = sample(17.0, 181.0, now)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.INVALID_COORDINATE), policy.evaluate(candidate, null, now))
    }

    @Test
    fun `out of order timestamp is rejected`() {
        val previous = sample(17.0, 78.0, now - 5_000L)
        val candidate = sample(17.0001, 78.0001, now - 6_000L)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.OUT_OF_ORDER_FIX), policy.evaluate(candidate, previous, now))
    }

    @Test
    fun `point too close to previous accepted point is rejected`() {
        val previous = sample(17.0, 78.0, now - 5_000L)
        val candidate = sample(17.000001, 78.000001, now)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.TOO_CLOSE_TO_PREVIOUS_POINT), policy.evaluate(candidate, previous, now))
    }

    @Test
    fun `unrealistic location jump is rejected`() {
        val previous = sample(17.0, 78.0, now - 1_000L)
        val candidate = sample(17.001, 78.001, now)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.UNLIKELY_MOVEMENT), policy.evaluate(candidate, previous, now))
    }

    @Test
    fun `valid walking movement is accepted`() {
        val previous = sample(17.0, 78.0, now - 5_000L)
        val candidate = sample(17.00005, 78.0, now)

        assertTrue(policy.evaluate(candidate, previous, now) is GpsFixDecision.Accept)
    }

    @Test
    fun `coordinate outside legal latitude range is rejected`() {
        val candidate = sample(91.0, 78.0, now)

        assertEquals(GpsFixDecision.Reject(GpsFixRejection.INVALID_COORDINATE), policy.evaluate(candidate, null, now))
    }

    private fun sample(
        latitude: Double,
        longitude: Double,
        timestamp: Long,
        accuracy: Double = 4.0,
    ) = GpsLocationSample(latitude, longitude, accuracy, timestamp)
}
