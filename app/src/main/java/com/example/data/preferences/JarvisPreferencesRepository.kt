package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.JarvisHudTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jarvis_preferences")

data class JarvisSettings(
    val customApiKey: String = "",
    val aiModel: String = "gemini-3.5-flash",
    val personalityStyle: String = "WITTY_BUTLER", // WITTY_BUTLER, TACTICAL_COMBAT, CONCISE_ANALYST
    val preferredLanguage: String = "AUTO", // AUTO, en-US, hi-IN, ne-NP
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 0.95f,
    val speechVolume: Float = 1.0f,
    val continuousListening: Boolean = false,
    val wakeWordEnabled: Boolean = true,
    val wakePhrase: String = "Hey JARVIS",
    val backgroundServiceEnabled: Boolean = false,
    val proactiveSpeechEnabled: Boolean = true,
    val notificationReadoutEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val memoryEnabled: Boolean = true,
    val confirmSensitiveActions: Boolean = true,
    val hudTheme: JarvisHudTheme = JarvisHudTheme.ARC_CYAN,
    val userTitle: String = "Sir"
)

class JarvisPreferencesRepository(private val context: Context) {

    private object Keys {
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val PERSONALITY_STYLE = stringPreferencesKey("personality_style")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val SPEECH_VOLUME = floatPreferencesKey("speech_volume")
        val CONTINUOUS_LISTENING = booleanPreferencesKey("continuous_listening")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
        val PROACTIVE_SPEECH_ENABLED = booleanPreferencesKey("proactive_speech_enabled")
        val NOTIFICATION_READOUT_ENABLED = booleanPreferencesKey("notification_readout_enabled")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val CONFIRM_SENSITIVE_ACTIONS = booleanPreferencesKey("confirm_sensitive_actions")
        val HUD_THEME = stringPreferencesKey("hud_theme")
        val USER_TITLE = stringPreferencesKey("user_title")
    }

    val settingsFlow: Flow<JarvisSettings> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.HUD_THEME] ?: JarvisHudTheme.ARC_CYAN.name
        val theme = runCatching { JarvisHudTheme.valueOf(themeName) }.getOrDefault(JarvisHudTheme.ARC_CYAN)

        JarvisSettings(
            customApiKey = prefs[Keys.CUSTOM_API_KEY] ?: "",
            aiModel = prefs[Keys.AI_MODEL] ?: "gemini-3.5-flash",
            personalityStyle = prefs[Keys.PERSONALITY_STYLE] ?: "WITTY_BUTLER",
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "AUTO",
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            speechPitch = prefs[Keys.SPEECH_PITCH] ?: 0.95f,
            speechVolume = prefs[Keys.SPEECH_VOLUME] ?: 1.0f,
            continuousListening = prefs[Keys.CONTINUOUS_LISTENING] ?: false,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
            wakePhrase = prefs[Keys.WAKE_PHRASE] ?: "Hey JARVIS",
            backgroundServiceEnabled = prefs[Keys.BACKGROUND_SERVICE_ENABLED] ?: false,
            proactiveSpeechEnabled = prefs[Keys.PROACTIVE_SPEECH_ENABLED] ?: true,
            notificationReadoutEnabled = prefs[Keys.NOTIFICATION_READOUT_ENABLED] ?: false,
            quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: false,
            quietStartHour = prefs[Keys.QUIET_START_HOUR] ?: 22,
            quietEndHour = prefs[Keys.QUIET_END_HOUR] ?: 7,
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            confirmSensitiveActions = prefs[Keys.CONFIRM_SENSITIVE_ACTIONS] ?: true,
            hudTheme = theme,
            userTitle = prefs[Keys.USER_TITLE] ?: "Sir"
        )
    }

    suspend fun updateCustomApiKey(key: String) {
        context.dataStore.edit { it[Keys.CUSTOM_API_KEY] = key.trim() }
    }

    suspend fun updateAiModel(model: String) {
        context.dataStore.edit { it[Keys.AI_MODEL] = model }
    }

    suspend fun updatePersonalityStyle(style: String) {
        context.dataStore.edit { it[Keys.PERSONALITY_STYLE] = style }
    }

    suspend fun updatePreferredLanguage(lang: String) {
        context.dataStore.edit { it[Keys.PREFERRED_LANGUAGE] = lang }
    }

    suspend fun updateVoiceSettings(rate: Float, pitch: Float, volume: Float) {
        context.dataStore.edit {
            it[Keys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f)
            it[Keys.SPEECH_PITCH] = pitch.coerceIn(0.5f, 1.8f)
            it[Keys.SPEECH_VOLUME] = volume.coerceIn(0.1f, 1.0f)
        }
    }

    suspend fun updateContinuousListening(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONTINUOUS_LISTENING] = enabled }
    }

    suspend fun updateWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun updateBackgroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_SERVICE_ENABLED] = enabled }
    }

    suspend fun updateProactiveSpeechEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PROACTIVE_SPEECH_ENABLED] = enabled }
    }

    suspend fun updateNotificationReadoutEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_READOUT_ENABLED] = enabled }
    }

    suspend fun updateQuietHours(enabled: Boolean, startHour: Int = 22, endHour: Int = 7) {
        context.dataStore.edit {
            it[Keys.QUIET_HOURS_ENABLED] = enabled
            it[Keys.QUIET_START_HOUR] = startHour
            it[Keys.QUIET_END_HOUR] = endHour
        }
    }

    suspend fun updateMemoryEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }
    }

    suspend fun updateConfirmSensitiveActions(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_SENSITIVE_ACTIONS] = enabled }
    }

    suspend fun updateHudTheme(theme: JarvisHudTheme) {
        context.dataStore.edit { it[Keys.HUD_THEME] = theme.name }
    }

    suspend fun updateUserTitle(title: String) {
        context.dataStore.edit { it[Keys.USER_TITLE] = title.ifBlank { "Sir" } }
    }
}
