package com.github.jvsena42.mandacaru.domain.geoip

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class GeoIpUrlTest {

    @Test
    fun `stamps a month as yyyy-MM`() {
        assertEquals("2026-07", GeoIpUrl.stamp(YearMonth.of(2026, 7)))
    }

    @Test
    fun `pads single digit months`() {
        assertEquals("2026-01", GeoIpUrl.stamp(YearMonth.of(2026, 1)))
    }

    @Test
    fun `builds the download url for a month`() {
        assertEquals(
            "https://download.db-ip.com/free/dbip-country-lite-2026-07.mmdb.gz",
            GeoIpUrl.forMonth(YearMonth.of(2026, 7)),
        )
    }

    @Test
    fun `previous month fallback crosses the year boundary`() {
        assertEquals(
            "https://download.db-ip.com/free/dbip-country-lite-2025-12.mmdb.gz",
            GeoIpUrl.forMonth(YearMonth.of(2026, 1).minusMonths(1)),
        )
    }
}
