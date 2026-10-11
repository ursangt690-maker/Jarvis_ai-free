package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.ConversationTurnEntity
import com.example.data.preferences.JarvisSettings
import com.example.domain.DeviceTelemetry
import com.example.domain.JarvisVoiceState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.HudFramedCard
import com.example.ui.components.JarvisAnimatedOrb
import com.example.ui.components.TopTelemetryHudBar
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisOnlineGreen
import com.example.ui.theme.JarvisSpeakingCyan
import com.example.ui.theme.JarvisThinkingViolet
import com.example.ui.theme.JarvisWarningAmber
import com.example.viewmodel.PendingConfirmationAction

@Composable
fun HudCommandScreen(
    telemetry: DeviceTelemetry,
    settings: JarvisSettings,
    isApiKeyConfigured: Boolean,
    isForegroundActive: Boolean,
    voiceState: JarvisVoiceState,
    rmsLevel: Float,
    partialTranscript: String,
    hudSpokenText: String,
    hudDetailedCard: String,
    multiStepPlan: List<String>,
    proactiveSuggestion: String,
    statusBanner: String,
    pendingConfirmation: PendingConfirmationAction?,
    recentTurns: List<ConversationTurnEntity>,
    onOrbClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onRepeatSpeechClick: () -> Unit,
    onSubmitDirective: (String) -> Unit,
    onToggleContinuousListening: (Boolean) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onConfirmAction: () -> Unit,
    onDismissAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandText by rememberSaveable { mutableStateOf("") }
    var showAuditLog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopTelemetryHudBar(
            telemetry = telemetry,
            isApiKeyConfigured = isApiKeyConfigured,
            isForegroundActive = isForegroundActive
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Header Designation & Language Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "J.A.R.V.I.S. MARK VII",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "MODEL: ${settings.aiModel.uppercase()} • ${settings.personalityStyle.replace("_", " ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Language Pill (AUTO / EN / NE / HI)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val langs = listOf("AUTO" to "AUTO", "en-US" to "EN", "ne-NP" to "ने", "hi-IN" to "हि")
                    langs.forEach { (code, shortLabel) ->
                        val selected = settings.preferredLanguage == code
                        Text(
                            text = shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .testTag("lang_chip_$code")
                        )
                    }
                }
            }
        }

        // Security Confirmation Modal Banner for Sensitive Directives (Calls / SMS / Email)
        AnimatedVisibility(visible = pendingConfirmation != null) {
            pendingConfirmation?.let { pending ->
                HudFramedCard(
                    title = pending.title,
                    subtitle = "SECURITY AUTHORIZATION REQUIRED",
                    accentColor = JarvisWarningAmber,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text = pending.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onConfirmAction,
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisOnlineGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_directive_button")
                        ) {
                            Text("AUTHORIZE", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        OutlinedButton(
                            onClick = onDismissAction,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cancel_directive_button")
                        ) {
                            Text("ABORT")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Central Animated Arc-Reactor Voice Orb
        JarvisAnimatedOrb(
            voiceState = voiceState,
            rmsLevel = rmsLevel,
            onClick = onOrbClick
        )

        // State Status Badge
        val stateLabel = when (voiceState) {
            JarvisVoiceState.IDLE -> "STANDBY — TAP ORB TO SPEAK"
            JarvisVoiceState.LISTENING -> "VOICE CAPTURE ACTIVE — SPEAK NOW"
            JarvisVoiceState.THINKING -> "NEURAL MATRIX COMPUTING..."
            JarvisVoiceState.SPEAKING -> "VOCALIZING RESPONSE — TAP TO INTERRUPT"
        }
        val stateColor = when (voiceState) {
            JarvisVoiceState.IDLE -> MaterialTheme.colorScheme.primary
            JarvisVoiceState.LISTENING -> JarvisOnlineGreen
            JarvisVoiceState.THINKING -> JarvisThinkingViolet
            JarvisVoiceState.SPEAKING -> JarvisSpeakingCyan
        }

        Text(
            text = stateLabel,
            style = MaterialTheme.typography.labelLarge,
            color = stateColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        AudioWaveformVisualizer(
            voiceState = voiceState,
            rmsLevel = rmsLevel
        )

        if (partialTranscript.isNotBlank()) {
            Text(
                text = "“$partialTranscript”",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisOnlineGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (statusBanner.isNotBlank()) {
            Surface(
                color = JarvisWarningAmber.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, JarvisWarningAmber.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Diagnostic Notice",
                        tint = JarvisWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Vocal Output Readout Card
        HudFramedCard(
            title = "JARVIS VOCAL STREAM",
            subtitle = if (settings.wakeWordEnabled) "WAKE: '${settings.wakePhrase.uppercase()}'" else "MANUAL TRIGGER"
        ) {
            Text(
                text = hudSpokenText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("jarvis_spoken_output")
            )

            if (hudDetailedCard.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = hudDetailedCard,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (multiStepPlan.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MULTI-STEP EXECUTION SEQUENCE:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                multiStepPlan.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Step ${idx + 1}",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Voice Controls Row: Replay Speech, Stop/Interrupt, Continuous Listening Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRepeatSpeechClick,
                        modifier = Modifier.testTag("replay_speech_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak Again",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SPEAK", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onInterruptClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCriticalRed),
                        modifier = Modifier.testTag("interrupt_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Interrupt Audio",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CONT. MIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = settings.continuousListening,
                        onCheckedChange = onToggleContinuousListening,
                        modifier = Modifier.testTag("continuous_listening_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Proactive Quick Voice Directive Chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "PROACTIVE DIRECTIVES • $proactiveSuggestion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickDirectives = listOf(
                    "Morning briefing",
                    "Flashlight on",
                    "Flashlight off",
                    "Battery & storage status",
                    "Weather in Kathmandu",
                    "Convert 100 USD to NPR",
                    "Set alarm for 07:00",
                    "नमस्ते जार्विस, खबर के छ?",
                    "नमस्ते जार्विस, मौसम बताओ"
                )
                quickDirectives.forEach { directive ->
                    AssistChip(
                        onClick = { onSubmitDirective(directive) },
                        label = { Text(directive, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (directive.contains("Flashlight")) Icons.Default.FlashlightOn else Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct HUD Command Input Bar (For quiet environments or emulators without mic hardware)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                placeholder = {
                    Text(
                        "Issue voice or text directive (EN / नेपाली / हिन्दी)...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("hud_command_input"),
                leadingIcon = {
                    IconButton(onClick = onOrbClick) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Activate Voice Input",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                onSubmitDirective(commandText)
                                commandText = ""
                            }
                        },
                        modifier = Modifier.testTag("hud_send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Execute Directive",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Collapsible Neural Audit Log (keeps main screen voice-first, not a text-chat app)
        OutlinedButton(
            onClick = { showAuditLog = !showAuditLog },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("toggle_audit_log_button")
        ) {
            Text(
                text = if (showAuditLog) "HIDE NEURAL COMMAND LOG (${recentTurns.size})" else "VIEW NEURAL COMMAND LOG (${recentTurns.size})",
                style = MaterialTheme.typography.labelMedium
            )
        }

        AnimatedVisibility(visible = showAuditLog) {
            HudFramedCard(
                title = "RECENT DIRECTIVE AUDIT LOG",
                subtitle = "MEMORY ENABLED: ${settings.memoryEnabled}",
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (recentTurns.isEmpty()) {
                    Text(
                        text = "No directives recorded in this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentTurns.takeLast(6).reversed().forEach { turn ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "${turn.role.uppercase()} [${turn.languageCode}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (turn.role == "model") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = turn.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
