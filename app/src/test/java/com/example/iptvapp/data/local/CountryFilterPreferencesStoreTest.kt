package com.example.iptvapp.data.local

import com.example.iptvapp.data.model.CountryGroupFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryFilterPreferencesStoreTest {
    @Test
    fun usesDefaultFiltersWhenNoPreferenceExists() {
        assertEquals(CountryGroupFilter.DefaultFilters, resolveCountryFilters(null))
    }

    @Test
    fun resolvesSavedFiltersInStableCatalogOrder() {
        assertEquals(
            listOf(CountryGroupFilter.ESP, CountryGroupFilter.GER),
            resolveCountryFilters(setOf("GER", "ESP"))
        )
    }

    @Test
    fun ignoresUnknownValuesAndLimitsVisibleFilters() {
        assertEquals(
            listOf(
                CountryGroupFilter.USA,
                CountryGroupFilter.UK,
                CountryGroupFilter.AUS,
                CountryGroupFilter.CAN
            ),
            resolveCountryFilters(setOf("USA", "UK", "AUS", "CAN", "ESP", "UNKNOWN"))
        )
    }
}
