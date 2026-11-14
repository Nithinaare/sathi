package com.farmsaathi.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class LocalPhotoExifTest {
    @Test
    fun `parseExifDateTimeString parses standard EXIF timestamp and offset cleanly`() {
        val epoch = LocalPhotoStorage.parseExifDateTimeString("2026:11:12 06:42:15", "+05:30")
        assertNotNull(epoch)
        // Check exact UTC equivalent: 2026-11-12 01:12:15 UTC
        val cal =
            java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(2026, java.util.Calendar.NOVEMBER, 12, 1, 12, 15)
            }
        assertEquals(cal.timeInMillis, epoch)
    }

    @Test
    fun `parseExifDateTimeString returns null for blank or malformed strings`() {
        assertNull(LocalPhotoStorage.parseExifDateTimeString(""))
        assertNull(LocalPhotoStorage.parseExifDateTimeString("   "))
        assertNull(LocalPhotoStorage.parseExifDateTimeString("not-a-date"))
    }
}
