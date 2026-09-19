package com.github.jvsena42.mandacaru.domain.floresta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncDecimalsTest {

    @Test
    fun `filter progress is measured from genesis`() {
        assertEquals(
            0.5f,
            computeFilterSyncDecimal(filters = 250_000, height = 500_000)!!,
            TOLERANCE,
        )
    }

    @Test
    fun `filters at or past the tip are complete`() {
        assertEquals(
            1f,
            computeFilterSyncDecimal(filters = 500_000, height = 500_000)!!,
            TOLERANCE,
        )
        assertEquals(
            1f,
            computeFilterSyncDecimal(filters = 500_010, height = 500_000)!!,
            TOLERANCE,
        )
    }

    @Test
    fun `absent filters mean there is nothing to wait on`() {
        assertNull(computeFilterSyncDecimal(filters = null, height = 500_000))
    }

    @Test
    fun `zero height does not divide by zero`() {
        assertEquals(
            0f,
            computeFilterSyncDecimal(filters = 0, height = 0)!!,
            TOLERANCE,
        )
    }

    @Test
    fun `rescan progress is null until the daemon reports a block total`() {
        assertNull(computeRescanProgressDecimal(processed = 0, total = 0))
        assertNull(computeRescanProgressDecimal(processed = null, total = null))
    }

    @Test
    fun `rescan progress is the processed ratio`() {
        assertEquals(
            0.25f,
            computeRescanProgressDecimal(processed = 80, total = 320)!!,
            TOLERANCE,
        )
    }

    @Test
    fun `rescan progress clamps at one`() {
        assertEquals(
            1f,
            computeRescanProgressDecimal(processed = 400, total = 319)!!,
            TOLERANCE,
        )
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
