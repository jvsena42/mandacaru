package com.github.jvsena42.mandacaru.domain.geoip

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * DB-IP publishes one file per month at a date-stamped URL, so "is there a newer database?"
 * reduces to "do we already hold this month's stamp?" — no HEAD request or ETag needed.
 */
object GeoIpUrl {

    private const val BASE_URL = "https://download.db-ip.com/free/dbip-country-lite-"
    private const val SUFFIX = ".mmdb.gz"
    private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)

    fun stamp(month: YearMonth): String = month.format(FORMATTER)

    fun forMonth(month: YearMonth): String = BASE_URL + stamp(month) + SUFFIX
}
