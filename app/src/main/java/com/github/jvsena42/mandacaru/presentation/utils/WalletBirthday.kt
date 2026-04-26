package com.github.jvsena42.mandacaru.presentation.utils

import java.time.Year

object WalletBirthday {
    const val MIN_YEAR = 2009
    const val DEFAULT_YEARS_BACK = 3
    private const val BLOCKS_PER_YEAR = 52_560

    fun defaultYear(): Int = Year.now().value - DEFAULT_YEARS_BACK

    fun bitcoinHeightForYear(year: Int): Int {
        if (year <= MIN_YEAR) return 0
        BITCOIN_JAN_1_HEIGHTS[year]?.let { return it }
        val (lastYear, lastHeight) = BITCOIN_JAN_1_HEIGHTS.maxBy { it.key }
        val extrapolated = lastHeight + (year - lastYear) * BLOCKS_PER_YEAR
        return extrapolated.coerceAtLeast(0)
    }

    private val BITCOIN_JAN_1_HEIGHTS = mapOf(
        2009 to 1,
        2010 to 32_256,
        2011 to 100_388,
        2012 to 160_255,
        2013 to 214_935,
        2014 to 278_287,
        2015 to 336_860,
        2016 to 391_182,
        2017 to 446_033,
        2018 to 501_950,
        2019 to 556_445,
        2020 to 610_682,
        2021 to 663_913,
        2022 to 717_050,
        2023 to 769_800,
        2024 to 822_375,
        2025 to 877_130,
    )
}
