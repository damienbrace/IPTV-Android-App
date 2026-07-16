package com.example.iptvapp.data.model

enum class CountryGroupFilter(
    val label: String,
    val displayName: String,
    private val providerCodes: Set<String>
) {
    USA("USA", "United States", setOf("US", "USA")),
    UK("UK", "United Kingdom", setOf("UK")),
    AUS("AUS", "Australia", setOf("AU", "AUS", "AUSTRALIA")),
    CAN("CAN", "Canada", setOf("CA", "CAN", "CANADA")),
    ARG("ARG", "Argentina", setOf("AR", "ARG", "ARGENTINA")),
    ESP("ESP", "Spain", setOf("ES", "ESP", "ESPAÑA", "SPAIN")),
    ITA("ITA", "Italy", setOf("IT", "ITA", "ITALY")),
    AFR("AFR", "Africa", setOf("AFRICA")),
    FRA("FRA", "France", setOf("FR", "FRA", "FRANCE")),
    SWE("SWE", "Sweden", setOf("SE", "SWE", "SWEDEN")),
    CAR("CAR", "Caribbean", setOf("CARIBBEAN")),
    NED("NED", "Netherlands", setOf("NL", "NED", "NETHERLANDS")),
    NOR("NOR", "Norway", setOf("NO", "NOR", "NORWAY")),
    BEL("BEL", "Belgium", setOf("BE", "BEL", "BELGIUM")),
    GER("GER", "Germany", setOf("DE", "GER", "GERMANY")),
    DEN("DEN", "Denmark", setOf("DK", "DEN", "DENMARK")),
    IND("IND", "India / Pakistan", setOf("INDIA/PAKISTAN")),
    MEX("MEX", "Mexico", setOf("MX", "MEX", "MEXICO")),
    POL("POL", "Poland", setOf("PL", "POL", "POLAND"));

    fun matchesCategory(category: String): Boolean {
        val providerCode = category.substringBefore('|').trim().uppercase()
        return providerCode in providerCodes
    }

    companion object {
        val DefaultFilters = listOf(USA, UK, AUS, CAN)
        const val MaxEnabledFilters = 4
    }
}

internal fun availableCountryGroupFilters(categories: List<String>): List<CountryGroupFilter> {
    return CountryGroupFilter.entries.filter { filter ->
        categories.any(filter::matchesCategory)
    }
}

internal fun matchesSelectedCountryFilters(
    category: String,
    selectedFilters: Set<CountryGroupFilter>
): Boolean {
    return selectedFilters.isEmpty() || selectedFilters.any { it.matchesCategory(category) }
}
