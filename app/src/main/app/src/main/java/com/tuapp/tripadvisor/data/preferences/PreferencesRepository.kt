package com.tuapp.tripadvisor.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val PRICE_PER_KM = doublePreferencesKey("price_per_km")
        val EARNINGS_PER_HOUR = doublePreferencesKey("earnings_per_hour")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val price = preferences[Keys.PRICE_PER_KM] ?: 0.0
        val earnings = preferences[Keys.EARNINGS_PER_HOUR] ?: 0.0
        UserPreferences(
            minPricePerKm = price,
            minEarningsPerHour = earnings
        )
    }

    suspend fun savePreferences(pricePerKm: Double, earningsPerHour: Double) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PRICE_PER_KM] = pricePerKm
            preferences[Keys.EARNINGS_PER_HOUR] = earningsPerHour
        }
    }
}
