package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JarvisRepository
import com.example.data.local.ConversationTurnEntity
import com.example.data.local.HabitEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.MemoryFactEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.data.preferences.JarvisPreferencesRepository
import com.example.data.preferences.JarvisSettings
import com.example.data.remote.JarvisAiStructuredResponse
import com.example.data.remote.JarvisNetworkClient
import com.example.domain.AndroidSystemController
import com.example.domain.ContactEntry
import com.example.domain.DeviceTelemetry
import com.example.domain.DocumentAndVisionProcessor
import com.example.domain.InstalledAppInfo
import com.example.domain.JarvisVoiceState
import com.example.domain.LoadedDocumentInfo
import com.example.domain.VoiceAssistantEngine
import com.example.service.InterceptedNotification
import com.example.service.JarvisForegroundService
import com.example.service.JarvisServiceBus
import com.example.ui.theme.JarvisHudTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PendingConfirmationAction(
    val title: String,
    val description: String,
    val actionType: String,
    val target: String,
    val payload: String
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getInstance(application)
    private val repository = JarvisRepository(database.jarvisDao())
    private val preferencesRepo = JarvisPreferencesRepository(application)
    val systemController = AndroidSystemController(application)
    private val docProcessor = DocumentAndVisionProcessor(application)

    val settings: StateFlow<JarvisSettings> = preferencesRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JarvisSettings()
    )

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reminders: StateFlow<List<ReminderEntity>> = repository.allReminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitEntity>> = repository.allHabits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories: StateFlow<List<MemoryFactEntity>> = repository.allMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val conversationTurns: StateFlow<List<ConversationTurnEntity>> = repository.allConversationTurns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val interceptedNotifications: StateFlow<List<InterceptedNotification>> = JarvisServiceBus.recentNotifications
    val isForegroundServiceActive: StateFlow<Boolean> = JarvisServiceBus.isForegroundActive

    private val _telemetry = MutableStateFlow(systemController.readTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _foundContacts = MutableStateFlow<List<ContactEntry>>(emptyList())
    val foundContacts: StateFlow<List<ContactEntry>> = _foundContacts.asStateFlow()

    private val _hudSpokenText = MutableStateFlow(
        "J.A.R.V.I.S. Mark VII online. All primary Android subsystems calibrated. Tap the Arc Reactor orb or issue a directive, Sir."
    )
    val hudSpokenText: StateFlow<String> = _hudSpokenText.asStateFlow()

    private val _hudDetailedCard = MutableStateFlow("")
    val hudDetailedCard: StateFlow<String> = _hudDetailedCard.asStateFlow()

    private val _multiStepPlan = MutableStateFlow<List<String>>(emptyList())
    val multiStepPlan: StateFlow<List<String>> = _multiStepPlan.asStateFlow()

    private val _proactiveSuggestion = MutableStateFlow("Say 'Morning briefing', 'Weather in Kathmandu', or 'Turn on flashlight'")
    val proactiveSuggestion: StateFlow<String> = _proactiveSuggestion.asStateFlow()

    private val _statusBanner = MutableStateFlow("")
    val statusBanner: StateFlow<String> = _statusBanner.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmationAction?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmationAction?> = _pendingConfirmation.asStateFlow()

    private val _loadedDocument = MutableStateFlow<LoadedDocumentInfo?>(null)
    val loadedDocument: StateFlow<LoadedDocumentInfo?> = _loadedDocument.asStateFlow()

    private val _liveToolOutput = MutableStateFlow("")
    val liveToolOutput: StateFlow<String> = _liveToolOutput.asStateFlow()

    val voiceEngine = VoiceAssistantEngine(
        context = application,
        onTranscriptReady = { transcript, lang ->
            submitCommand(transcript, lang, fromVoice = true)
        },
        onStatusMessage = { msg ->
            _statusBanner.value = msg
        }
    )

    val voiceState: StateFlow<JarvisVoiceState> = voiceEngine.voiceState
    val rmsLevel: StateFlow<Float> = voiceEngine.rmsLevel
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            _installedApps.value = systemController.listInstalledApps()
        }

        // Keep VoiceEngine synced with DataStore settings
        viewModelScope.launch {
            settings.collect { cfg ->
                voiceEngine.updateConfiguration(
                    rate = cfg.speechRate,
                    pitch = cfg.speechPitch,
                    volume = cfg.speechVolume,
                    language = cfg.preferredLanguage,
                    continuous = cfg.continuousListening,
                    wakeWord = cfg.wakeWordEnabled,
                    phrase = cfg.wakePhrase
                )
            }
        }

        // Periodic telemetry refresh every 8 seconds
        viewModelScope.launch {
            while (isActive) {
                val currentTelemetry = systemController.readTelemetry()
                _telemetry.value = currentTelemetry
                if (currentTelemetry.batteryPercent in 1..15 && !currentTelemetry.isCharging) {
                    _proactiveSuggestion.value = "Warning: Arc Reactor reserve at ${currentTelemetry.batteryPercent}%. Recommend connecting power supply."
                }
                delay(8000L)
            }
        }

        // Listen for scheduled reminder broadcasts and speak them if allowed
        viewModelScope.launch {
            JarvisServiceBus.incomingReminderAlerts.collect { alert ->
                if (!alert.isNullOrBlank()) {
                    _hudSpokenText.value = alert
                    if (settings.value.proactiveSpeechEnabled && !isWithinQuietHours(settings.value)) {
                        voiceEngine.speak(alert, "en-US")
                    }
                    JarvisServiceBus.consumeReminderAlert()
                }
            }
        }
    }

    fun refreshTelemetryAndApps() {
        viewModelScope.launch {
            _telemetry.value = systemController.readTelemetry()
            _installedApps.value = systemController.listInstalledApps()
        }
    }

    fun onOrbTapped(hasRecordPermission: Boolean, onRequestPermission: () -> Unit) {
        when (voiceState.value) {
            JarvisVoiceState.SPEAKING, JarvisVoiceState.LISTENING, JarvisVoiceState.THINKING -> {
                voiceEngine.interruptAll()
                _statusBanner.value = "Audio & listening interrupted."
            }
            JarvisVoiceState.IDLE -> {
                if (!hasRecordPermission) {
                    onRequestPermission()
                } else {
                    _statusBanner.value = ""
                    voiceEngine.startListening(isContinuousAutoRestart = false)
                }
            }
        }
    }

    fun interruptAudio() {
        voiceEngine.interruptAll()
    }

    fun speakText(text: String, lang: String = "en-US") {
        if (!isWithinQuietHours(settings.value)) {
            voiceEngine.speak(text, lang)
        } else {
            _statusBanner.value = "Quiet Hours active — voice muted."
        }
    }

    /**
     * Primary entry point for both spoken commands and typed HUD directives.
     * Supports offline fallback commands as well as online Gemini AI + function execution.
     */
    fun submitCommand(
        rawCommand: String,
        detectedLang: String = "",
        fromVoice: Boolean = false
    ) {
        val clean = rawCommand.trim()
        if (clean.isBlank()) return

        val lang = detectedLang.ifBlank {
            VoiceAssistantEngine.detectLanguageFromScript(clean, settings.value.preferredLanguage)
        }

        viewModelScope.launch {
            _statusBanner.value = ""
            voiceEngine.setThinkingState()

            repository.insertConversationTurn(
                ConversationTurnEntity(
                    role = "user",
                    message = clean,
                    languageCode = lang
                )
            )

            val currentTelemetry = systemController.readTelemetry()
            _telemetry.value = currentTelemetry

            // Check fast deterministic offline / hardware commands first if device is offline or API key is missing
            val apiConfigured = JarvisNetworkClient.isApiKeyConfigured(settings.value.customApiKey)
            if (!currentTelemetry.isOnline || !apiConfigured) {
                val offlineResult = tryExecuteDeterministicCommand(clean, currentTelemetry)
                if (offlineResult != null) {
                    completeAssistantTurn(
                        spoken = offlineResult.first,
                        detailed = offlineResult.second,
                        lang = lang,
                        actionSummary = "OFFLINE_DIRECTIVE"
                    )
                    return@launch
                }
                if (!currentTelemetry.isOnline) {
                    completeAssistantTurn(
                        spoken = "Sir, network uplink is currently offline. Local phone controls, notes, tasks, and alarms remain operational, but cloud Gemini intelligence requires an active connection.",
                        detailed = "Device Status: OFFLINE\nAvailable Offline Commands:\n• 'Flashlight on / off'\n• 'Battery status'\n• 'Storage status'\n• 'Open Settings / Wi-Fi / Bluetooth'\n• 'Volume up / down'\n• 'Set alarm for 07:00'",
                        lang = "en-US",
                        actionSummary = "OFFLINE_NOTICE"
                    )
                    return@launch
                }
            }

            // Online Gemini processing (with optional attached vision bitmap / document text)
            val doc = _loadedDocument.value
            val aiResult = repository.processCommandWithGemini(
                userInput = clean,
                detectedLanguage = lang,
                settings = settings.value,
                telemetry = currentTelemetry,
                attachedBitmap = doc?.previewBitmap,
                attachedDocumentText = doc?.extractedText
            )

            aiResult.onSuccess { structured ->
                handleStructuredAiResponse(structured)
            }.onFailure { err ->
                // Attempt deterministic local command fallback before reporting error
                val fallback = tryExecuteDeterministicCommand(clean, currentTelemetry)
                if (fallback != null) {
                    completeAssistantTurn(
                        spoken = fallback.first,
                        detailed = fallback.second,
                        lang = lang,
                        actionSummary = "LOCAL_FALLBACK"
                    )
                } else {
                    val errMsg = err.localizedMessage ?: "Unknown neural link error"
                    completeAssistantTurn(
                        spoken = "Sir, I encountered an issue communicating with the Gemini service: $errMsg",
                        detailed = "Diagnostic Details:\n$errMsg\n\nVerify your GEMINI_API_KEY in the AI Studio Secrets panel or the CONFIG tab.",
                        lang = "en-US",
                        actionSummary = "ERROR"
                    )
                }
            }
        }
    }

    private suspend fun handleStructuredAiResponse(res: JarvisAiStructuredResponse) {
        if (res.memoryFactToSave.isNotBlank() && settings.value.memoryEnabled) {
            repository.insertMemory(
                MemoryFactEntity(
                    keyFact = res.memoryFactToSave.trim(),
                    category = "USER_FACT"
                )
            )
        }

        if (res.multiStepPlan.isNotEmpty()) {
            _multiStepPlan.value = res.multiStepPlan
        }

        if (res.proactiveSuggestion.isNotBlank()) {
            _proactiveSuggestion.value = res.proactiveSuggestion
        }

        val actionType = res.actionType.uppercase(Locale.ROOT)
        var actionExecutionNote = ""

        if (actionType != "NONE") {
            val isSensitive = actionType in listOf("DIAL_NUMBER", "COMPOSE_SMS", "COMPOSE_EMAIL")
            if ((isSensitive || res.requiresConfirmation) && settings.value.confirmSensitiveActions) {
                _pendingConfirmation.value = PendingConfirmationAction(
                    title = "Confirm Directive: $actionType",
                    description = "Target: ${res.actionTarget}\nDetails: ${res.actionPayload}",
                    actionType = actionType,
                    target = res.actionTarget,
                    payload = res.actionPayload
                )
                actionExecutionNote = "Awaiting user confirmation for $actionType."
            } else {
                actionExecutionNote = executeSystemAction(actionType, res.actionTarget, res.actionPayload)
            }
        }

        val combinedDetails = buildString {
            if (res.detailedDisplay.isNotBlank()) {
                appendLine(res.detailedDisplay)
            }
            if (actionExecutionNote.isNotBlank()) {
                if (isNotEmpty()) appendLine()
                append("[SYSTEM EXECUTION]: $actionExecutionNote")
            }
        }

        val finalSpoken = if (actionExecutionNote.isNotBlank() && res.spokenReply.isBlank()) {
            actionExecutionNote
        } else {
            res.spokenReply
        }

        completeAssistantTurn(
            spoken = finalSpoken,
            detailed = combinedDetails,
            lang = res.detectedLanguage.ifBlank { "en-US" },
            actionSummary = actionType
        )
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            val result = executeSystemAction(pending.actionType, pending.target, pending.payload)
            _hudSpokenText.value = result
            speakText(result, "en-US")
        }
    }

    fun dismissPendingAction() {
        _pendingConfirmation.value = null
        _statusBanner.value = "Directive cancelled by operator."
    }

    suspend fun executeSystemAction(actionType: String, target: String, payload: String): String {
        return when (actionType.uppercase(Locale.ROOT)) {
            "FLASHLIGHT_ON" -> {
                val res = systemController.setFlashlight(true).getOrElse { it.localizedMessage ?: "Flashlight failed" }
                _telemetry.value = systemController.readTelemetry()
                res
            }
            "FLASHLIGHT_OFF" -> {
                val res = systemController.setFlashlight(false).getOrElse { it.localizedMessage ?: "Flashlight failed" }
                _telemetry.value = systemController.readTelemetry()
                res
            }
            "OPEN_APP" -> systemController.openInstalledAppByName(target)
            "OPEN_SETTINGS" -> systemController.openSystemSettings(Settings.ACTION_SETTINGS)
            "OPEN_WIFI" -> systemController.openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
            "OPEN_BLUETOOTH" -> systemController.openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            "OPEN_LOCATION" -> systemController.openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "VOLUME_UP" -> {
                val r = systemController.adjustMediaVolume("UP")
                _telemetry.value = systemController.readTelemetry()
                r
            }
            "VOLUME_DOWN" -> {
                val r = systemController.adjustMediaVolume("DOWN")
                _telemetry.value = systemController.readTelemetry()
                r
            }
            "VOLUME_SET" -> {
                val r = systemController.adjustMediaVolume(target)
                _telemetry.value = systemController.readTelemetry()
                r
            }
            "SET_ALARM" -> {
                val parts = target.split(":")
                val hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 7
                val min = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                systemController.setSystemAlarm(hour, min, payload.ifBlank { "JARVIS Alarm" })
            }
            "SET_TIMER" -> {
                val secs = target.filter { it.isDigit() }.toIntOrNull() ?: 60
                systemController.setSystemTimer(secs, payload.ifBlank { "JARVIS Timer" })
            }
            "ADD_REMINDER" -> {
                val mins = payload.filter { it.isDigit() }.toIntOrNull() ?: 15
                val triggerAt = System.currentTimeMillis() + (mins * 60_000L)
                val id = repository.insertReminder(
                    ReminderEntity(
                        title = target.ifBlank { "Scheduled Directive" },
                        message = payload.ifBlank { "Scheduled via JARVIS voice command" },
                        triggerTimeMillis = triggerAt
                    )
                ).toInt()
                systemController.scheduleLocalReminderNotification(
                    reminderId = id,
                    title = target.ifBlank { "Scheduled Directive" },
                    message = payload.ifBlank { "Reminder triggered" },
                    triggerAtMillis = triggerAt
                )
                "Reminder '${target.ifBlank { "Directive" }}' scheduled for $mins minutes from now."
            }
            "ADD_TASK" -> {
                repository.insertTask(
                    TaskEntity(
                        title = target.ifBlank { "New Tactical Task" },
                        details = payload,
                        category = "TASK",
                        priority = "HIGH"
                    )
                )
                "Added task '$target' to Daily Operations."
            }
            "ADD_SHOPPING" -> {
                repository.insertTask(
                    TaskEntity(
                        title = target.ifBlank { "Supply Item" },
                        details = payload,
                        category = "SHOPPING",
                        priority = "NORMAL"
                    )
                )
                "Added '$target' to your shopping list."
            }
            "ADD_NOTE" -> {
                repository.insertNote(
                    NoteEntity(
                        title = target.ifBlank { "Voice Note" },
                        content = payload.ifBlank { target },
                        category = "GENERAL"
                    )
                )
                "Saved note '$target' to local memory."
            }
            "DIAL_NUMBER" -> {
                var number = target
                if (target.any { it.isLetter() }) {
                    val match = systemController.findContactsByName(target).firstOrNull()
                    if (match != null) number = match.phoneNumber
                }
                systemController.openDialer(number)
            }
            "COMPOSE_SMS" -> systemController.composeSms(target, payload)
            "COMPOSE_EMAIL" -> systemController.composeEmail(target, "Message via JARVIS", payload)
            "OPEN_MAPS" -> systemController.openMapsNavigation(target)
            "OPEN_CAMERA" -> systemController.launchCameraIntent()
            "WEB_SEARCH" -> systemController.openWebSearch(target.ifBlank { payload })
            "WEATHER_LOOKUP" -> {
                val weatherReport = JarvisNetworkClient.fetchLiveWeather(target.ifBlank { "New York" })
                    .getOrElse { "Unable to fetch live weather: ${it.localizedMessage}" }
                _liveToolOutput.value = weatherReport
                weatherReport
            }
            else -> ""
        }
    }

    private suspend fun tryExecuteDeterministicCommand(
        input: String,
        telemetry: DeviceTelemetry
    ): Pair<String, String>? {
        val lower = input.lowercase(Locale.ROOT)
        return when {
            lower.contains("flashlight on") || lower.contains("torch on") -> {
                val msg = executeSystemAction("FLASHLIGHT_ON", "", "")
                msg to "Hardware Action: Torch Enabled"
            }
            lower.contains("flashlight off") || lower.contains("torch off") -> {
                val msg = executeSystemAction("FLASHLIGHT_OFF", "", "")
                msg to "Hardware Action: Torch Disabled"
            }
            lower.contains("battery") -> {
                val chargingStr = if (telemetry.isCharging) "currently charging" else "discharging"
                val reply = "Power reserves are at ${telemetry.batteryPercent} percent, $chargingStr, with core temperature at ${telemetry.batteryTempCelsius} degrees Celsius."
                reply to "Battery: ${telemetry.batteryPercent}% | Charging: ${telemetry.isCharging} | Temp: ${telemetry.batteryTempCelsius}°C"
            }
            lower.contains("storage") || lower.contains("disk space") -> {
                val free = String.format(Locale.US, "%.1f", telemetry.storageFreeGb)
                val total = String.format(Locale.US, "%.1f", telemetry.storageTotalGb)
                val reply = "Internal storage has $free gigabytes available out of $total gigabytes total."
                reply to "Free Storage: $free GB / $total GB"
            }
            lower.contains("volume up") || lower.contains("increase volume") -> {
                val msg = executeSystemAction("VOLUME_UP", "", "")
                msg to msg
            }
            lower.contains("volume down") || lower.contains("decrease volume") -> {
                val msg = executeSystemAction("VOLUME_DOWN", "", "")
                msg to msg
            }
            lower.contains("open wifi") || lower.contains("wi-fi settings") -> {
                val msg = executeSystemAction("OPEN_WIFI", "", "")
                msg to msg
            }
            lower.contains("open bluetooth") -> {
                val msg = executeSystemAction("OPEN_BLUETOOTH", "", "")
                msg to msg
            }
            lower.startsWith("open ") -> {
                val appName = input.substringAfter("open ", "").trim()
                val msg = executeSystemAction("OPEN_APP", appName, "")
                msg to msg
            }
            lower.contains("time") || lower.contains("date") -> {
                val now = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' hh:mm a", Locale.US).format(Date())
                "The current local time is $now, Sir." to "System Clock: $now"
            }
            lower.contains("morning briefing") || lower.contains("daily briefing") -> {
                val activeTasks = tasks.value.count { !it.isCompleted }
                val noteCount = notes.value.size
                val summary = "Good day, ${settings.value.userTitle}. Power reserves stand at ${telemetry.batteryPercent}%. Network link is ${telemetry.networkType}. You have $activeTasks active tactical tasks and $noteCount saved notes in local storage."
                summary to "BRIEFING TELEMETRY:\n• Battery: ${telemetry.batteryPercent}%\n• Network: ${telemetry.networkType}\n• Pending Tasks: $activeTasks\n• Saved Notes: $noteCount"
            }
            else -> null
        }
    }

    private suspend fun completeAssistantTurn(
        spoken: String,
        detailed: String,
        lang: String,
        actionSummary: String
    ) {
        _hudSpokenText.value = spoken
        _hudDetailedCard.value = detailed

        repository.insertConversationTurn(
            ConversationTurnEntity(
                role = "model",
                message = if (detailed.isNotBlank()) "$spoken\n$detailed" else spoken,
                languageCode = lang,
                actionTaken = actionSummary
            )
        )

        if (!isWithinQuietHours(settings.value)) {
            voiceEngine.speak(spoken, lang)
        } else {
            voiceEngine.setIdleState()
            _statusBanner.value = "Quiet Hours active — spoken output suppressed."
        }
    }

    private fun isWithinQuietHours(cfg: JarvisSettings): Boolean {
        if (!cfg.quietHoursEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (cfg.quietStartHour <= cfg.quietEndHour) {
            hour in cfg.quietStartHour until cfg.quietEndHour
        } else {
            hour >= cfg.quietStartHour || hour < cfg.quietEndHour
        }
    }

    // --- Productivity CRUD Helpers ---

    fun addNote(title: String, content: String, category: String = "GENERAL") {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = title.ifBlank { "Tactical Note" },
                    content = content,
                    category = category
                )
            )
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    fun addTask(title: String, details: String, category: String = "TASK", priority: String = "HIGH") {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = title,
                    details = details,
                    category = category,
                    priority = priority
                )
            )
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch { repository.deleteTask(id) }
    }

    fun addReminder(title: String, message: String, minutesFromNow: Int, type: String = "REMINDER") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val triggerMillis = System.currentTimeMillis() + (minutesFromNow.coerceAtLeast(1) * 60_000L)
            val id = repository.insertReminder(
                ReminderEntity(
                    title = title,
                    message = message.ifBlank { title },
                    triggerTimeMillis = triggerMillis,
                    type = type
                )
            ).toInt()
            systemController.scheduleLocalReminderNotification(
                reminderId = id,
                title = title,
                message = message.ifBlank { title },
                triggerAtMillis = triggerMillis
            )
            _statusBanner.value = "Scheduled '$title' in $minutesFromNow min."
        }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }

    fun incrementHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val newCount = habit.currentCount + 1
            val newStreak = if (newCount == habit.targetPerDay) habit.streakDays + 1 else habit.streakDays
            repository.updateHabit(
                habit.copy(
                    currentCount = newCount,
                    streakDays = newStreak
                )
            )
        }
    }

    fun addHabit(name: String, targetPerDay: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertHabit(
                HabitEntity(
                    name = name.trim(),
                    targetPerDay = targetPerDay.coerceAtLeast(1),
                    currentCount = 0,
                    streakDays = 1
                )
            )
        }
    }

    fun addMemoryFact(fact: String, category: String = "PREFERENCE") {
        if (fact.isBlank()) return
        viewModelScope.launch {
            repository.insertMemory(MemoryFactEntity(keyFact = fact.trim(), category = category))
        }
    }

    fun deleteMemoryFact(id: Int) {
        viewModelScope.launch { repository.deleteMemory(id) }
    }

    fun clearAllMemoriesAndHistory() {
        viewModelScope.launch {
            repository.clearAllMemories()
            repository.clearConversationHistory()
            _hudDetailedCard.value = ""
            _multiStepPlan.value = emptyList()
            _statusBanner.value = "All conversation history and memory facts purged."
        }
    }

    // --- Contacts Search ---
    fun searchContacts(query: String) {
        viewModelScope.launch {
            _foundContacts.value = systemController.findContactsByName(query)
        }
    }

    // --- Internet & Information Live Tools ---
    fun fetchWeatherReport(city: String) {
        viewModelScope.launch {
            _liveToolOutput.value = "Scanning atmospheric satellites for $city..."
            val res = JarvisNetworkClient.fetchLiveWeather(city)
                .getOrElse { "Weather service error: ${it.localizedMessage}" }
            _liveToolOutput.value = res
            _hudSpokenText.value = res
            speakText(res, "en-US")
        }
    }

    fun convertCurrencyLive(base: String, target: String, amount: Double) {
        viewModelScope.launch {
            _liveToolOutput.value = "Querying global exchange rates ($base -> $target)..."
            val res = JarvisNetworkClient.fetchLiveCurrencyRate(base, target, amount)
                .getOrElse { "Currency exchange error: ${it.localizedMessage}" }
            _liveToolOutput.value = res
            _hudSpokenText.value = res
            speakText(res, "en-US")
        }
    }

    fun summarizeWebUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _liveToolOutput.value = "Extracting content from $url..."
            JarvisNetworkClient.fetchWebPageSnippet(url).onSuccess { snippet ->
                submitCommand(
                    rawCommand = "Summarize the key points from this webpage ($url):\n\n$snippet",
                    detectedLang = "en-US"
                )
            }.onFailure { err ->
                _liveToolOutput.value = "Could not fetch URL: ${err.localizedMessage}"
            }
        }
    }

    // --- Vision AI & Document Analysis ---
    fun onImageOrDocumentSelected(uri: Uri) {
        viewModelScope.launch {
            _statusBanner.value = "Analyzing file via JARVIS Optical & Document Processor..."
            docProcessor.loadUriDocumentOrImage(uri).onSuccess { info ->
                _loadedDocument.value = info
                val qrNote = if (info.qrOrBarcodeValue != null) {
                    "QR/Barcode decoded: ${info.qrOrBarcodeValue}"
                } else {
                    "Loaded ${info.fileName} (${info.mimeType})"
                }
                _statusBanner.value = qrNote
            }.onFailure { err ->
                _statusBanner.value = "Document load failed: ${err.localizedMessage}"
            }
        }
    }

    fun onCameraBitmapCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            val scaled = docProcessor.scaleBitmapIfNeeded(bitmap, 1024)
            val qr = docProcessor.decodeBarcodeOrQr(scaled)
            _loadedDocument.value = LoadedDocumentInfo(
                fileName = "Camera_Capture_${System.currentTimeMillis()}.jpg",
                mimeType = "image/jpeg",
                sizeBytes = (scaled.byteCount).toLong(),
                extractedText = if (qr != null) "Decoded QR/Barcode: $qr" else "Live Camera Frame Ready for Vision AI",
                previewBitmap = scaled,
                qrOrBarcodeValue = qr
            )
            _statusBanner.value = if (qr != null) "Decoded QR/Barcode: $qr" else "Camera frame captured for Vision AI."
        }
    }

    fun clearLoadedDocument() {
        _loadedDocument.value = null
    }

    fun analyzeLoadedDocument(prompt: String) {
        val doc = _loadedDocument.value ?: return
        submitCommand(
            rawCommand = "$prompt (Target File: ${doc.fileName})",
            detectedLang = "en-US"
        )
    }

    // --- Foreground Service Control ---
    fun toggleForegroundService(enable: Boolean) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            preferencesRepo.updateBackgroundServiceEnabled(enable)
            runCatching {
                val intent = Intent(app, JarvisForegroundService::class.java)
                if (enable) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        app.startForegroundService(intent)
                    } else {
                        app.startService(intent)
                    }
                } else {
                    intent.action = JarvisForegroundService.ACTION_STOP
                    app.startService(intent)
                }
            }.onFailure { err ->
                _statusBanner.value = "Foreground service restricted by OS: ${err.localizedMessage}"
            }
        }
    }

    // --- Settings Updates ---
    fun saveCustomApiKey(key: String) = viewModelScope.launch { preferencesRepo.updateCustomApiKey(key) }
    fun saveAiModel(model: String) = viewModelScope.launch { preferencesRepo.updateAiModel(model) }
    fun savePersonalityStyle(style: String) = viewModelScope.launch { preferencesRepo.updatePersonalityStyle(style) }
    fun savePreferredLanguage(lang: String) = viewModelScope.launch { preferencesRepo.updatePreferredLanguage(lang) }
    fun saveVoiceParams(rate: Float, pitch: Float, volume: Float) = viewModelScope.launch {
        preferencesRepo.updateVoiceSettings(rate, pitch, volume)
    }
    fun saveContinuousListening(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateContinuousListening(enabled) }
    fun saveWakeWordEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateWakeWordEnabled(enabled) }
    fun saveProactiveSpeech(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateProactiveSpeechEnabled(enabled) }
    fun saveQuietHours(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateQuietHours(enabled) }
    fun saveMemoryEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateMemoryEnabled(enabled) }
    fun saveConfirmSensitive(enabled: Boolean) = viewModelScope.launch { preferencesRepo.updateConfirmSensitiveActions(enabled) }
    fun saveHudTheme(theme: JarvisHudTheme) = viewModelScope.launch { preferencesRepo.updateHudTheme(theme) }
    fun saveUserTitle(title: String) = viewModelScope.launch { preferencesRepo.updateUserTitle(title) }

    override fun onCleared() {
        voiceEngine.shutdown()
        super.onCleared()
    }
}
