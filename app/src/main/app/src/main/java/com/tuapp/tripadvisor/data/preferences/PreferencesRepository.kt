package com.tuapp.tripadvisor.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.tuapp.tripadvisor.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "trip_advisor_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val MIN_PRICE_PER_KM = doublePreferencesKey("min_price_per_km")
        val MIN_EARNINGS_PER_HOUR = doublePreferencesKey("min_earnings_per_hour")
    }

    companion object {
        const val DEFAULT_MIN_PRICE_PER_KM = 1.5
        const val DEFAULT_MIN_EARNINGS_PER_HOUR = 25.0
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            minPricePerKm = prefs[Keys.MIN_PRICE_PER_KM] ?: DEFAULT_MIN_PRICE_PER_KM,
            minEarningsPerHour = prefs[Keys.MIN_EARNINGS_PER_HOUR] ?: DEFAULT_MIN_EARNINGS_PER_HOUR
        )
    }

    suspend fun savePreferences(prefs: UserPreferences) {
        context.dataStore.edit { settings ->
            settings[Keys.MIN_PRICE_PER_KM] = prefs.minPricePerKm
            settings[Keys.MIN_EARNINGS_PER_HOUR] = prefs.minEarningsPerHour
        }
    }
}
