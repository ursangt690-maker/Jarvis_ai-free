package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.MemoryFactEntity
import com.example.data.preferences.JarvisSettings
import com.example.service.InterceptedNotification
import com.example.ui.components.HudFramedCard
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisHudTheme
import com.example.ui.theme.JarvisOnlineGreen
import com.example.ui.theme.JarvisWarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndConfigScreen(
    settings: JarvisSettings,
    isApiKeyConfigured: Boolean,
    isForegroundActive: Boolean,
    memories: List<MemoryFactEntity>,
    interceptedNotifications: List<InterceptedNotification>,
    hasMicPermission: Boolean,
    hasCameraPermission: Boolean,
    hasContactsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSystemSetting: (String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveModel: (String) -> Unit,
    onSavePersonality: (String) -> Unit,
    onSaveLanguage: (String) -> Unit,
    onSaveVoiceParams: (rate: Float, pitch: Float, volume: Float) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleForegroundService: (Boolean) -> Unit,
    onToggleProactiveSpeech: (Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onToggleMemory: (Boolean) -> Unit,
    onToggleConfirmSensitive: (Boolean) -> Unit,
    onSelectHudTheme: (JarvisHudTheme) -> Unit,
    onSaveUserTitle: (String) -> Unit,
    onAddMemoryFact: (String) -> Unit,
    onDeleteMemoryFact: (Int) -> Unit,
    onClearAllHistoryAndMemory: () -> Unit,
    onSummarizeNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by rememberSaveable(settings.customApiKey) { mutableStateOf(settings.customApiKey) }
    var userTitleInput by rememberSaveable(settings.userTitle) { mutableStateOf(settings.userTitle) }
    var newMemoryInput by rememberSaveable { mutableStateOf("") }

    var speechRate by rememberSaveable(settings.speechRate) { mutableFloatStateOf(settings.speechRate) }
    var speechPitch by rememberSaveable(settings.speechPitch) { mutableFloatStateOf(settings.speechPitch) }
    var speechVolume by rememberSaveable(settings.speechVolume) { mutableFloatStateOf(settings.speechVolume) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM CONFIGURATION & SECURITY",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Gemini AI Configuration & Security Notice
        HudFramedCard(
            title = "GEMINI AI NEURAL LINK & CREDENTIALS",
            subtitle = if (isApiKeyConfigured) "ACTIVE" else "KEY NEEDED"
        ) {
            Surface(
                color = JarvisWarningAmber.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, JarvisWarningAmber.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Security Notice",
                        tint = JarvisWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.security_warning_prototype),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recommended: Set GEMINI_API_KEY in the AI Studio Secrets panel (injected via BuildConfig). Optional runtime override for development:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Runtime Gemini API Key (Optional Override)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_api_key_input")
                )
                Button(
                    onClick = { onSaveApiKey(apiKeyInput) },
                    modifier = Modifier.testTag("save_api_key_button")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("SELECT GEMINI MODEL:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val models = listOf(
                    "gemini-3.5-flash" to "Gemini 3.5 Flash (Fast)",
                    "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Deep Reasoning)",
                    "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash Lite"
                )
                models.forEach { (modelId, label) ->
                    FilterChip(
                        selected = settings.aiModel == modelId,
                        onClick = { onSaveModel(modelId) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("JARVIS PERSONALITY MATRIX:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val personas = listOf(
                    "WITTY_BUTLER" to "Classic Witty JARVIS",
                    "TACTICAL_COMBAT" to "Tactical Defense",
                    "CONCISE_ANALYST" to "Concise Analyst"
                )
                personas.forEach { (id, label) ->
                    FilterChip(
                        selected = settings.personalityStyle == id,
                        onClick = { onSavePersonality(id) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userTitleInput,
                    onValueChange = { userTitleInput = it },
                    label = { Text("Preferred Form of Address (e.g., Sir, Boss, Captain)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { onSaveUserTitle(userTitleInput) }) {
                    Text("UPDATE")
                }
            }
        }

        // 2. Voice, Language & Wake-Word Calibration
        HudFramedCard(
            title = "VOICE SYNTHESIS, LANGUAGE & WAKE-WORD",
            subtitle = settings.preferredLanguage
        ) {
            Text("VOICE & RESPONSE LANGUAGE:", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val langs = listOf(
                    "AUTO" to "Auto-Detect (EN / ने / हि)",
                    "en-US" to "English (US)",
                    "ne-NP" to "Nepali (नेपाली)",
                    "hi-IN" to "Hindi (हिन्दी)"
                )
                langs.forEach { (code, label) ->
                    FilterChip(
                        selected = settings.preferredLanguage == code,
                        onClick = { onSaveLanguage(code) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("Speaking Rate: %.2fx", speechRate),
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                onValueChangeFinished = { onSaveVoiceParams(speechRate, speechPitch, speechVolume) },
                valueRange = 0.6f..1.8f
            )

            Text(
                text = String.format("Voice Pitch: %.2f", speechPitch),
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = speechPitch,
                onValueChange = { speechPitch = it },
                onValueChangeFinished = { onSaveVoiceParams(speechRate, speechPitch, speechVolume) },
                valueRange = 0.6f..1.5f
            )

            Text(
                text = String.format("TTS Output Volume: %d%%", (speechVolume * 100).toInt()),
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = speechVolume,
                onValueChange = { speechVolume = it },
                onValueChangeFinished = { onSaveVoiceParams(speechRate, speechPitch, speechVolume) },
                valueRange = 0.2f..1.0f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Continuous Listening Mode", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.continuousListening,
                    onCheckedChange = onToggleContinuous
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("'Hey JARVIS' Wake-Word Filter", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.wakeWordEnabled,
                    onCheckedChange = onToggleWakeWord
                )
            }
        }

        // 3. HUD Visual Theme Customization
        HudFramedCard(
            title = "HUD VISUAL ARCHITECTURE THEME",
            subtitle = settings.hudTheme.displayName
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                JarvisHudTheme.entries.forEach { themeOption ->
                    FilterChip(
                        selected = settings.hudTheme == themeOption,
                        onClick = { onSelectHudTheme(themeOption) },
                        label = { Text(themeOption.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // 4. Background Operation, Notifications & Proactive Assistance
        HudFramedCard(
            title = "BACKGROUND SERVICE, NOTIFICATIONS & QUIET HOURS",
            subtitle = if (isForegroundActive) "SERVICE ACTIVE" else "STANDBY"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Foreground Assistant Service", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Displays persistent status notification for supported background operations.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.backgroundServiceEnabled,
                    onCheckedChange = onToggleForegroundService,
                    modifier = Modifier.testTag("foreground_service_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Proactive Spoken Alerts & Reminders", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.proactiveSpeechEnabled,
                    onCheckedChange = onToggleProactiveSpeech
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quiet Hours (22:00 – 07:00 Mute)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.quietHoursEnabled,
                    onCheckedChange = onToggleQuietHours
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onOpenSystemSetting(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NOTIFICATION ACCESS", style = MaterialTheme.typography.labelSmall)
                }

                if (interceptedNotifications.isNotEmpty()) {
                    Button(
                        onClick = onSummarizeNotifications,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SUMMARIZE (${interceptedNotifications.size})", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 5. Privacy, Conversation Memory & History Controls
        HudFramedCard(
            title = "PRIVACY, CONVERSATION MEMORY & HISTORY",
            subtitle = "${memories.size} STORED FACTS"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Context & Preference Memory", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.memoryEnabled,
                    onCheckedChange = onToggleMemory
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Confirm Before Sensitive Actions (Calls/SMS)", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.confirmSensitiveActions,
                    onCheckedChange = onToggleConfirmSensitive
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMemoryInput,
                    onValueChange = { newMemoryInput = it },
                    label = { Text("Add User Preference or Fact to Memory") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newMemoryInput.isNotBlank()) {
                            onAddMemoryFact(newMemoryInput)
                            newMemoryInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Save Memory")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            memories.forEach { mem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${mem.keyFact}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onDeleteMemoryFact(mem.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Fact", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClearAllHistoryAndMemory,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCriticalRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("purge_memory_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PURGE ALL CONVERSATION HISTORY & MEMORY")
            }
        }

        // 6. Android Permission Status & Troubleshooting Diagnostics
        HudFramedCard(
            title = "ANDROID PERMISSIONS & DIAGNOSTICS",
            subtitle = "VERSION ${BuildConfig.VERSION_NAME}"
        ) {
            PermissionStatusRow("Microphone (RECORD_AUDIO)", hasMicPermission)
            PermissionStatusRow("Camera Optical Sensor (CAMERA)", hasCameraPermission)
            PermissionStatusRow("Contacts Directory (READ_CONTACTS)", hasContactsPermission)
            PermissionStatusRow("Notifications (POST_NOTIFICATIONS)", hasNotificationPermission)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_permissions_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("REQUEST / VERIFY RUNTIME PERMISSIONS")
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "TROUBLESHOOTING & ANDROID OS LIMITATIONS:\n" +
                    "• Background Microphone: Android 14+ restricts continuous background microphone capture unless the Foreground Service is active and the user initiates it from a visible Activity.\n" +
                    "• Emulator SpeechRecognizer: Browser-streamed emulators without host microphone passthrough can execute all directives via the HUD Command Input bar or Quick Directive chips.\n" +
                    "• SMS & Phone Calls: Prepared via standard Android intents with user confirmation for Play Policy compliance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (granted) JarvisOnlineGreen else JarvisWarningAmber,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (granted) "GRANTED" else "NOT GRANTED",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) JarvisOnlineGreen else JarvisWarningAmber
            )
        }
    }
}
