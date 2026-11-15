package com.farmsaathi.feature.farm.measurement.offline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMapDownloadBudgetTest {
    @Test
    fun belowBothGuardrails_isAllowed() {
        assertFalse(OfflineMapDownloadBudget.isLimitReached(2_699, 40L * 1024L * 1024L - 1L))
    }

    @Test
    fun tileGuardrail_isReachedAtConfiguredCount() {
        assertTrue(OfflineMapDownloadBudget.isLimitReached(2_700, 1L))
    }

    @Test
    fun byteGuardrail_isReachedAtConfiguredSize() {
        assertTrue(OfflineMapDownloadBudget.isLimitReached(1L, 40L * 1024L * 1024L))
    }

    @Test
    fun eitherGuardrailStopsDownload() {
        assertTrue(OfflineMapDownloadBudget.isLimitReached(2_701, 0L))
        assertTrue(OfflineMapDownloadBudget.isLimitReached(0L, 40L * 1024L * 1024L + 1L))
    }
}
