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

enum class MediaTypeFilter {
    PHOTOS,
    VIDEOS,
    BOTH,
}

data class SessionSettings(
    val selectedAlbumIds: Set<String> = emptySet(),
    val selectedMediaKeys: Set<String> = emptySet(),
    val minutes: Int = 10,
    val pinHash: String? = null,
    val silenceNotifications: Boolean = false,
    val mediaTypeFilter: MediaTypeFilter = MediaTypeFilter.BOTH,
) {
    val isReadyToBegin: Boolean
        get() = (selectedAlbumIds.isNotEmpty() || selectedMediaKeys.isNotEmpty()) && pinHash != null
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ALBUM_IDS = stringSetPreferencesKey("selected_album_ids")
        val MEDIA_KEYS = stringSetPreferencesKey("selected_media_keys")
        val MINUTES = intPreferencesKey("minutes")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val SILENCE_NOTIFICATIONS = booleanPreferencesKey("silence_notifications")
        val MEDIA_TYPE_FILTER = stringPreferencesKey("media_type_filter")
    }

    val settings: Flow<SessionSettings> = context.dataStore.data.map { prefs ->
        SessionSettings(
            selectedAlbumIds = prefs[Keys.ALBUM_IDS] ?: emptySet(),
            selectedMediaKeys = prefs[Keys.MEDIA_KEYS] ?: emptySet(),
            minutes = prefs[Keys.MINUTES] ?: 10,
            pinHash = prefs[Keys.PIN_HASH],
            silenceNotifications = prefs[Keys.SILENCE_NOTIFICATIONS] ?: false,
            mediaTypeFilter = prefs[Keys.MEDIA_TYPE_FILTER]?.let { name ->
                runCatching { MediaTypeFilter.valueOf(name) }.getOrDefault(MediaTypeFilter.BOTH)
            } ?: MediaTypeFilter.BOTH,
        )
    }

    suspend fun current(): SessionSettings = settings.first()

    suspend fun setSelectedAlbums(bucketIds: Set<String>) {
        context.dataStore.edit { it[Keys.ALBUM_IDS] = bucketIds }
    }

    suspend fun setSelectedMediaKeys(keys: Set<String>) {
        context.dataStore.edit { it[Keys.MEDIA_KEYS] = keys }
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

    suspend fun setMediaTypeFilter(filter: MediaTypeFilter) {
        context.dataStore.edit { it[Keys.MEDIA_TYPE_FILTER] = filter.name }
    }

    suspend fun verifyPin(candidate: String): Boolean {
        val stored = current().pinHash ?: return false
        return PinHasher.matches(candidate, stored)
    }
}
