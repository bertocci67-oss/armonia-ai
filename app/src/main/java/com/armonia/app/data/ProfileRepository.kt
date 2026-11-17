package com.armonia.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.armonia.app.domain.model.Instrument
import com.armonia.app.domain.model.Level
import com.armonia.app.domain.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.profileDataStore by preferencesDataStore(name = "armonia_profile")

class ProfileRepository(private val context: Context) {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val instrument = stringPreferencesKey("instrument")
        val level = stringPreferencesKey("level")
    }

    val profile: Flow<Profile?> = context.profileDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            if (preferences[Keys.onboarded] != true) {
                return@map null
            }

            val instrument = preferences[Keys.instrument]
                ?.let { saved -> Instrument.entries.firstOrNull { it.name == saved } }
            val level = preferences[Keys.level]
                ?.let { saved -> Level.entries.firstOrNull { it.name == saved } }

            if (instrument == null || level == null) {
                null
            } else {
                Profile(instrument = instrument, level = level)
            }
        }

    suspend fun save(profile: Profile) {
        context.profileDataStore.edit { preferences ->
            preferences[Keys.onboarded] = true
            preferences[Keys.instrument] = profile.instrument.name
            preferences[Keys.level] = profile.level.name
        }
    }
}
