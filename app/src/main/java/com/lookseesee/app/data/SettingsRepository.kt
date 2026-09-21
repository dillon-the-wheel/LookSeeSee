package com.lookseesee.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lookseesee.app.util.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "look_see_see_settings")

data class SessionSettings(
    val selectedAlbumIds: Set<String> = emptySet(),
    val minutes: Int = 10,
    val pinHash: String? = null,
    val silenceNotifications: Boolean = false,
) {
    val isReadyToBegin: Boolean get() = selectedAlbumIds.isNotEmpty() && pinHash != null
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ALBUM_IDS = stringSetPreferencesKey("selected_album_ids")
        val MINUTES = intPreferencesKey("minutes")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val SILENCE_NOTIFICATIONS = booleanPreferencesKey("silence_notifications")
    }

    val settings: Flow<SessionSettings> = context.dataStore.data.map { prefs ->
        SessionSettings(
            selectedAlbumIds = prefs[Keys.ALBUM_IDS] ?: emptySet(),
            minutes = prefs[Keys.MINUTES] ?: 10,
            pinHash = prefs[Keys.PIN_HASH],
            silenceNotifications = prefs[Keys.SILENCE_NOTIFICATIONS] ?: false,
        )
    }

    suspend fun current(): SessionSettings = settings.first()

    suspend fun setSelectedAlbums(bucketIds: Set<String>) {
        context.dataStore.edit { it[Keys.ALBUM_IDS] = bucketIds }
    }

    suspend fun setMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.MINUTES] = minutes }
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = PinHasher.hash(pin) }
    }

    suspend fun setSilenceNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SILENCE_NOTIFICATIONS] = enabled }
    }

    suspend fun verifyPin(candidate: String): Boolean {
        val stored = current().pinHash ?: return false
        return PinHasher.matches(candidate, stored)
    }
}
