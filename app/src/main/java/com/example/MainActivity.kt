package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.JarvisNetworkClient
import com.example.ui.screens.DailyOpsProductivityScreen
import com.example.ui.screens.HudCommandScreen
import com.example.ui.screens.SettingsAndConfigScreen
import com.example.ui.screens.SystemsAndCommsScreen
import com.example.ui.screens.VisionAndDocumentsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JarvisViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class JarvisDestination(val label: String, val icon: ImageVector, val tag: String) {
    HUD("HUD", Icons.Default.RadioButtonChecked, "nav_hud"),
    SYSTEMS("SYSTEMS", Icons.Default.Memory, "nav_systems"),
    DAILY_OPS("DAILY OPS", Icons.Default.Checklist, "nav_ops"),
    VISION_DOCS("VISION", Icons.Default.CenterFocusStrong, "nav_vision"),
    CONFIG("CONFIG", Icons.Default.Tune, "nav_config")
}

class MainActivity : ComponentActivity() {

    private val jarvisViewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by jarvisViewModel.settings.collectAsStateWithLifecycle()

            MyApplicationTheme(hudTheme = settings.hudTheme) {
                JarvisAppRoot(viewModel = jarvisViewModel)
            }
        }
    }
}

@Composable
fun JarvisAppRoot(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(JarvisDestination.HUD) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val foundContacts by viewModel.foundContacts.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val conversationTurns by viewModel.conversationTurns.collectAsStateWithLifecycle()
    val interceptedNotifications by viewModel.interceptedNotifications.collectAsStateWithLifecycle()
    val isForegroundActive by viewModel.isForegroundServiceActive.collectAsStateWithLifecycle()

    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val hudSpokenText by viewModel.hudSpokenText.collectAsStateWithLifecycle()
    val hudDetailedCard by viewModel.hudDetailedCard.collectAsStateWithLifecycle()
    val multiStepPlan by viewModel.multiStepPlan.collectAsStateWithLifecycle()
    val proactiveSuggestion by viewModel.proactiveSuggestion.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val loadedDocument by viewModel.loadedDocument.collectAsStateWithLifecycle()
    val liveToolOutput by viewModel.liveToolOutput.collectAsStateWithLifecycle()

    var hasMicPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasContactsPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by rememberSaveable {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantMap ->
        grantMap[Manifest.permission.RECORD_AUDIO]?.let { hasMicPermission = it }
        grantMap[Manifest.permission.CAMERA]?.let { hasCameraPermission = it }
        grantMap[Manifest.permission.READ_CONTACTS]?.let { hasContactsPermission = it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grantMap[Manifest.permission.POST_NOTIFICATIONS]?.let { hasNotificationPermission = it }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.onOrbTapped(hasRecordPermission = true, onRequestPermission = {})
        }
    }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onCameraBitmapCaptured(bitmap)
        }
    }

    val cameraPermissionForPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageOrDocumentSelected(uri)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageOrDocumentSelected(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshTelemetryAndApps()
    }

    if (currentDestination != JarvisDestination.HUD) {
        BackHandler {
            currentDestination = JarvisDestination.HUD
        }
    }

    val isApiKeyConfigured = JarvisNetworkClient.isApiKeyConfigured(settings.customApiKey)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                JarvisDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination == dest,
                        onClick = { currentDestination = dest },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.label
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag(dest.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                JarvisDestination.HUD -> {
                    HudCommandScreen(
                        telemetry = telemetry,
                        settings = settings,
                        isApiKeyConfigured = isApiKeyConfigured,
                        isForegroundActive = isForegroundActive,
                        voiceState = voiceState,
                        rmsLevel = rmsLevel,
                        partialTranscript = partialTranscript,
                        hudSpokenText = hudSpokenText,
                        hudDetailedCard = hudDetailedCard,
                        multiStepPlan = multiStepPlan,
                        proactiveSuggestion = proactiveSuggestion,
                        statusBanner = statusBanner,
                        pendingConfirmation = pendingConfirmation,
                        recentTurns = conversationTurns,
                        onOrbClick = {
                            viewModel.onOrbTapped(
                                hasRecordPermission = hasMicPermission,
                                onRequestPermission = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            )
                        },
                        onInterruptClick = { viewModel.interruptAudio() },
                        onRepeatSpeechClick = { viewModel.speakText(hudSpokenText, settings.preferredLanguage) },
                        onSubmitDirective = { cmd -> viewModel.submitCommand(cmd) },
                        onToggleContinuousListening = { enabled -> viewModel.saveContinuousListening(enabled) },
                        onSelectLanguage = { lang -> viewModel.savePreferredLanguage(lang) },
                        onConfirmAction = { viewModel.confirmPendingAction() },
                        onDismissAction = { viewModel.dismissPendingAction() }
                    )
                }

                JarvisDestination.SYSTEMS -> {
                    SystemsAndCommsScreen(
                        telemetry = telemetry,
                        installedApps = installedApps,
                        foundContacts = foundContacts,
                        liveToolOutput = liveToolOutput,
                        hasContactsPermission = hasContactsPermission,
                        onRequestContactsPermission = {
                            multiplePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                        },
                        onExecuteAction = { type, target, payload ->
                            CoroutineScope(Dispatchers.Main).launch {
                                val res = viewModel.executeSystemAction(type, target, payload)
                                if (res.isNotBlank()) {
                                    viewModel.speakText(res, "en-US")
                                }
                            }
                        },
                        onSearchContacts = { q -> viewModel.searchContacts(q) },
                        onFetchWeather = { city -> viewModel.fetchWeatherReport(city) },
                        onConvertCurrency = { base, target, amt -> viewModel.convertCurrencyLive(base, target, amt) },
                        onSummarizeUrl = { url -> viewModel.summarizeWebUrl(url) }
                    )
                }

                JarvisDestination.DAILY_OPS -> {
                    DailyOpsProductivityScreen(
                        notes = notes,
                        tasks = tasks,
                        reminders = reminders,
                        habits = habits,
                        onAddNote = { title, content, cat -> viewModel.addNote(title, content, cat) },
                        onDeleteNote = { id -> viewModel.deleteNote(id) },
                        onAddTask = { title, details, cat, prio -> viewModel.addTask(title, details, cat, prio) },
                        onToggleTask = { task -> viewModel.toggleTaskCompleted(task) },
                        onDeleteTask = { id -> viewModel.deleteTask(id) },
                        onAddReminder = { title, msg, mins, type -> viewModel.addReminder(title, msg, mins, type) },
                        onDeleteReminder = { id -> viewModel.deleteReminder(id) },
                        onIncrementHabit = { habit -> viewModel.incrementHabit(habit) },
                        onAddHabit = { name, target -> viewModel.addHabit(name, target) },
                        onSetSystemAlarm = { h, m, label ->
                            val r = viewModel.systemController.setSystemAlarm(h, m, label)
                            viewModel.speakText(r, "en-US")
                        },
                        onSetSystemTimer = { s, label ->
                            val r = viewModel.systemController.setSystemTimer(s, label)
                            viewModel.speakText(r, "en-US")
                        },
                        onCreateCalendarEvent = { title, desc ->
                            val r = viewModel.systemController.createCalendarEvent(title, desc, System.currentTimeMillis() + 3600_000L)
                            viewModel.speakText(r, "en-US")
                        },
                        onTriggerBriefing = { prompt ->
                            currentDestination = JarvisDestination.HUD
                            viewModel.submitCommand(prompt)
                        }
                    )
                }

                JarvisDestination.VISION_DOCS -> {
                    VisionAndDocumentsScreen(
                        loadedDocument = loadedDocument,
                        hudSpokenText = hudSpokenText,
                        hudDetailedCard = hudDetailedCard,
                        onCaptureCameraPhoto = {
                            if (hasCameraPermission) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermissionForPhotoLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onPickGalleryImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickDocumentOrCodeFile = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "text/*",
                                    "application/json",
                                    "application/xml",
                                    "image/*"
                                )
                            )
                        },
                        onAnalyzeLoadedTarget = { prompt -> viewModel.analyzeLoadedDocument(prompt) },
                        onClearLoadedTarget = { viewModel.clearLoadedDocument() },
                        onDirectCodeAnalysis = { code, instruction ->
                            viewModel.submitCommand("$instruction\n\n```\n$code\n```")
                        }
                    )
                }

                JarvisDestination.CONFIG -> {
                    SettingsAndConfigScreen(
                        settings = settings,
                        isApiKeyConfigured = isApiKeyConfigured,
                        isForegroundActive = isForegroundActive,
                        memories = memories,
                        interceptedNotifications = interceptedNotifications,
                        hasMicPermission = hasMicPermission,
                        hasCameraPermission = hasCameraPermission,
                        hasContactsPermission = hasContactsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermissions = {
                            val perms = mutableListOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA,
                                Manifest.permission.READ_CONTACTS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            multiplePermissionsLauncher.launch(perms.toTypedArray())
                        },
                        onOpenSystemSetting = { action -> viewModel.systemController.openSystemSettings(action) },
                        onSaveApiKey = { key -> viewModel.saveCustomApiKey(key) },
                        onSaveModel = { model -> viewModel.saveAiModel(model) },
                        onSavePersonality = { persona -> viewModel.savePersonalityStyle(persona) },
                        onSaveLanguage = { lang -> viewModel.savePreferredLanguage(lang) },
                        onSaveVoiceParams = { r, p, v -> viewModel.saveVoiceParams(r, p, v) },
                        onToggleContinuous = { enabled -> viewModel.saveContinuousListening(enabled) },
                        onToggleWakeWord = { enabled -> viewModel.saveWakeWordEnabled(enabled) },
                        onToggleForegroundService = { enabled -> viewModel.toggleForegroundService(enabled) },
                        onToggleProactiveSpeech = { enabled -> viewModel.saveProactiveSpeech(enabled) },
                        onToggleQuietHours = { enabled -> viewModel.saveQuietHours(enabled) },
                        onToggleMemory = { enabled -> viewModel.saveMemoryEnabled(enabled) },
                        onToggleConfirmSensitive = { enabled -> viewModel.saveConfirmSensitive(enabled) },
                        onSelectHudTheme = { theme -> viewModel.saveHudTheme(theme) },
                        onSaveUserTitle = { title -> viewModel.saveUserTitle(title) },
                        onAddMemoryFact = { fact -> viewModel.addMemoryFact(fact) },
                        onDeleteMemoryFact = { id -> viewModel.deleteMemoryFact(id) },
                        onClearAllHistoryAndMemory = { viewModel.clearAllMemoriesAndHistory() },
                        onSummarizeNotifications = {
                            val summaryInput = interceptedNotifications.joinToString("\n") {
                                "- [${it.packageName}] ${it.title}: ${it.text}"
                            }
                            currentDestination = JarvisDestination.HUD
                            viewModel.submitCommand("Summarize these recent Android notifications concisely:\n$summaryInput")
                        }
                    )
                }
            }
        }
    }
}
