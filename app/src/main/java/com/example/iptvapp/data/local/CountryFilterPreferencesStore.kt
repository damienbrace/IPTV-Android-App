package com.example.iptvapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.iptvapp.data.model.CountryGroupFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.countryFilterPreferencesDataStore by preferencesDataStore(
    name = "country_filter_preferences"
)

class CountryFilterPreferencesStore(private val context: Context) {
    val enabledFilters: Flow<List<CountryGroupFilter>> = context.countryFilterPreferencesDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            resolveCountryFilters(preferences[ENABLED_FILTERS])
        }

    suspend fun saveEnabledFilters(filters: List<CountryGroupFilter>) {
        val boundedFilters = filters.distinct().take(CountryGroupFilter.MaxEnabledFilters)
        if (boundedFilters.isEmpty()) return
        context.countryFilterPreferencesDataStore.edit { preferences ->
            preferences[ENABLED_FILTERS] = boundedFilters.mapTo(mutableSetOf()) { it.name }
        }
    }

    private companion object {
        val ENABLED_FILTERS = stringSetPreferencesKey("enabled_filters")
    }
}

internal fun resolveCountryFilters(storedNames: Set<String>?): List<CountryGroupFilter> {
    val resolved = CountryGroupFilter.entries
        .filter { it.name in storedNames.orEmpty() }
        .take(CountryGroupFilter.MaxEnabledFilters)
    return resolved.ifEmpty { CountryGroupFilter.DefaultFilters }
}
