package com.example.iptvapp.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryGroupFilterTest {
    @Test
    fun matchesProviderCountryPrefixes() {
        assertTrue(CountryGroupFilter.USA.matchesCategory("USA | Sports"))
        assertTrue(CountryGroupFilter.UK.matchesCategory("UK | Sky Sports"))
        assertTrue(CountryGroupFilter.AUS.matchesCategory("AU | Kayo Events"))
        assertTrue(CountryGroupFilter.CAN.matchesCategory("CA | TSN+"))
    }

    @Test
    fun doesNotMatchCountriesWithSimilarStartingLetters() {
        assertFalse(CountryGroupFilter.UK.matchesCategory("Ukraine - Україна"))
        assertFalse(CountryGroupFilter.AUS.matchesCategory("Austria"))
        assertFalse(CountryGroupFilter.CAN.matchesCategory("Cambodia"))
    }

    @Test
    fun exposesOnlyCountriesPresentInProviderCategories() {
        val available = availableCountryGroupFilters(
            listOf("USA | Sports", "España | Dazn", "Live | Astro Sports", "Replay | Football")
        )

        assertTrue(CountryGroupFilter.USA in available)
        assertTrue(CountryGroupFilter.ESP in available)
        assertFalse(CountryGroupFilter.UK in available)
    }

    @Test
    fun combinesMultipleSelectedCountries() {
        val selected = setOf(CountryGroupFilter.UK, CountryGroupFilter.AUS)

        assertTrue(matchesSelectedCountryFilters("UK | Sky Sports", selected))
        assertTrue(matchesSelectedCountryFilters("AU | Kayo Events", selected))
        assertFalse(matchesSelectedCountryFilters("USA | Sports", selected))
    }

    @Test
    fun emptySelectionShowsEveryCountry() {
        assertTrue(matchesSelectedCountryFilters("France | Sports", emptySet()))
    }
}
