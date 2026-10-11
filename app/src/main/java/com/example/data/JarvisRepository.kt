package com.example.data

import android.graphics.Bitmap
import com.example.data.local.ConversationTurnEntity
import com.example.data.local.HabitEntity
import com.example.data.local.JarvisDao
import com.example.data.local.MemoryFactEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.data.preferences.JarvisSettings
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.InlineData
import com.example.data.remote.JarvisAiStructuredResponse
import com.example.data.remote.JarvisNetworkClient
import com.example.data.remote.Part
import com.example.domain.DeviceTelemetry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class JarvisRepository(private val dao: JarvisDao) {

    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val allReminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val allHabits: Flow<List<HabitEntity>> = dao.getAllHabits()
    val allMemories: Flow<List<MemoryFactEntity>> = dao.getAllMemories()
    val allConversationTurns: Flow<List<ConversationTurnEntity>> = dao.getAllConversationTurns()

    suspend fun insertNote(note: NoteEntity): Long = dao.insertNote(note)
    suspend fun deleteNote(id: Int) = dao.deleteNote(id)
    suspend fun searchNotes(query: String): List<NoteEntity> = dao.searchNotes(query)

    suspend fun insertTask(task: TaskEntity): Long = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(id: Int) = dao.deleteTask(id)

    suspend fun insertReminder(reminder: ReminderEntity): Long = dao.insertReminder(reminder)
    suspend fun deleteReminder(id: Int) = dao.deleteReminder(id)

    suspend fun insertHabit(habit: HabitEntity): Long = dao.insertHabit(habit)
    suspend fun updateHabit(habit: HabitEntity) = dao.updateHabit(habit)
    suspend fun deleteHabit(id: Int) = dao.deleteHabit(id)

    suspend fun insertMemory(memory: MemoryFactEntity): Long = dao.insertMemory(memory)
    suspend fun deleteMemory(id: Int) = dao.deleteMemory(id)
    suspend fun clearAllMemories() = dao.clearAllMemories()

    suspend fun insertConversationTurn(turn: ConversationTurnEntity): Long = dao.insertConversationTurn(turn)
    suspend fun clearConversationHistory() = dao.clearConversationHistory()

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val recentTurns = dao.getRecentConversationTurns(1)
        if (recentTurns.isEmpty()) {
            dao.insertMemory(
                MemoryFactEntity(
                    keyFact = "Operator prefers concise tactical briefings with subtle British wit.",
                    category = "PREFERENCE"
                )
            )
            dao.insertHabit(
                HabitEntity(
                    name = "Deep Work Engineering Sprint",
                    targetPerDay = 2,
                    currentCount = 1,
                    streakDays = 5
                )
            )
            dao.insertHabit(
                HabitEntity(
                    name = "Hydration & Mobility Protocol",
                    targetPerDay = 4,
                    currentCount = 2,
                    streakDays = 12
                )
            )
            dao.insertTask(
                TaskEntity(
                    title = "Review system security & API telemetry",
                    details = "Verify Gemini 3.5 Flash neural link and Android permissions.",
                    category = "TASK",
                    priority = "HIGH"
                )
            )
            dao.insertNote(
                NoteEntity(
                    title = "Mark VII Voice Directives Guide",
                    content = "Try saying:\n• 'Turn on the flashlight'\n• 'What is my battery and storage status?'\n• 'Weather in Kathmandu'\n• 'Convert 100 USD to NPR'\n• 'Set an alarm for 07:30'\n• 'Remind me to call Stark Industries'\n• 'नमस्ते जार्विस, आजको मौसम कस्तो छ?'",
                    category = "BRIEFING",
                    isPinned = true
                )
            )
        }
    }

    suspend fun processCommandWithGemini(
        userInput: String,
        detectedLanguage: String,
        settings: JarvisSettings,
        telemetry: DeviceTelemetry,
        attachedBitmap: Bitmap? = null,
        attachedDocumentText: String? = null
    ): Result<JarvisAiStructuredResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = JarvisNetworkClient.resolveApiKey(settings.customApiKey)
            if (apiKey.isBlank()) {
                throw IllegalStateException(
                    "Gemini API key is not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel or enter a key in the CONFIG tab."
                )
            }

            val memories = if (settings.memoryEnabled) {
                dao.getRecentMemoriesSync().joinToString("\n") { "- ${it.keyFact}" }
            } else {
                "Memory disabled by user."
            }

            val historyTurns = dao.getRecentConversationTurns(8).reversed()
            val contents = mutableListOf<Content>()

            for (turn in historyTurns) {
                val role = if (turn.role == "model") "model" else "user"
                contents.add(
                    Content(
                        role = role,
                        parts = listOf(Part(text = turn.message))
                    )
                )
            }

            val promptBuilder = StringBuilder()
            promptBuilder.appendLine("User Input: $userInput")
            promptBuilder.appendLine("Detected/Preferred Language: $detectedLanguage")
            if (!attachedDocumentText.isNullOrBlank()) {
                promptBuilder.appendLine("\n--- Attached Document / Extracted Content ---")
                promptBuilder.appendLine(attachedDocumentText.take(10000))
            }

            val currentParts = mutableListOf<Part>()
            currentParts.add(Part(text = promptBuilder.toString()))
            if (attachedBitmap != null) {
                currentParts.add(
                    Part(
                        inlineData = InlineData(
                            mimeType = "image/jpeg",
                            data = JarvisNetworkClient.bitmapToBase64(attachedBitmap)
                        )
                    )
                )
            }

            contents.add(Content(role = "user", parts = currentParts))

            val personalityInstruction = when (settings.personalityStyle) {
                "TACTICAL_COMBAT" -> "You are J.A.R.V.I.S. in Tactical Defense Protocol: crisp, high-precision, mission-focused, addressing the user as ${settings.userTitle}."
                "CONCISE_ANALYST" -> "You are J.A.R.V.I.S. in Analytical Mode: ultra-concise, data-driven, zero fluff, addressing the user as ${settings.userTitle}."
                else -> "You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), inspired by Iron Man's AI assistant: calm, hyper-intelligent, refined, helpful, confident, and subtly witty without being verbose. Address the user as ${settings.userTitle}."
            }

            val nowFormatted = SimpleDateFormat("EEEE, MMM d, yyyy HH:mm z", Locale.US).format(Date())
            val systemPrompt = """
                $personalityInstruction
                Current Local Time: $nowFormatted
                Live Android Telemetry:
                - Device: ${telemetry.deviceModel} (${telemetry.androidVersion})
                - Battery: ${telemetry.batteryPercent}% (Charging: ${telemetry.isCharging}, Temp: ${telemetry.batteryTempCelsius}°C)
                - Network: ${telemetry.networkType} (Online: ${telemetry.isOnline})
                - Storage Free: ${String.format(Locale.US, "%.1f", telemetry.storageFreeGb)} GB / ${String.format(Locale.US, "%.1f", telemetry.storageTotalGb)} GB
                - Media Volume: ${telemetry.mediaVolumePercent}%
                - Flashlight Active: ${telemetry.isFlashlightOn}

                Saved User Memory & Preferences:
                $memories

                Language Rules:
                - Default to English (en-US).
                - If the user speaks or writes in Nepali (नेपाली) or asks for Nepali, respond naturally in Nepali and set detectedLanguage = "ne-NP".
                - If the user speaks or writes in Hindi (हिन्दी) or asks for Hindi, respond naturally in Hindi and set detectedLanguage = "hi-IN".

                Action Mapping Rules:
                - If the user asks to perform a supported Android action, populate actionType, actionTarget, and actionPayload accurately:
                  * Flashlight on/off -> FLASHLIGHT_ON or FLASHLIGHT_OFF
                  * Open an app -> OPEN_APP, actionTarget = app name
                  * Open settings/Wi-Fi/Bluetooth/Location -> OPEN_SETTINGS, OPEN_WIFI, OPEN_BLUETOOTH, OPEN_LOCATION
                  * Volume up/down/set -> VOLUME_UP, VOLUME_DOWN, VOLUME_SET (actionTarget = percentage)
                  * Set alarm -> SET_ALARM (actionTarget = "HH:mm", actionPayload = label)
                  * Set timer -> SET_TIMER (actionTarget = seconds as integer string, actionPayload = label)
                  * Add reminder -> ADD_REMINDER (actionTarget = title, actionPayload = minutes from now or details)
                  * Add task/todo -> ADD_TASK (actionTarget = task title, actionPayload = priority HIGH/NORMAL)
                  * Add shopping item -> ADD_SHOPPING (actionTarget = item name)
                  * Save note -> ADD_NOTE (actionTarget = note title, actionPayload = note content)
                  * Call / dial -> DIAL_NUMBER (actionTarget = contact name or phone number, requiresConfirmation = true)
                  * Send SMS -> COMPOSE_SMS (actionTarget = contact name or number, actionPayload = message text, requiresConfirmation = true)
                  * Send Email -> COMPOSE_EMAIL (actionTarget = email address, actionPayload = body text, requiresConfirmation = true)
                  * Maps / navigation -> OPEN_MAPS (actionTarget = destination or place)
                  * Open camera -> OPEN_CAMERA
                  * Live weather -> WEATHER_LOOKUP (actionTarget = city name)
                  * Web search -> WEB_SEARCH (actionTarget = search query)
                  * Otherwise -> NONE
                - Keep spokenReply natural, concise (1-3 sentences), and suitable for Text-To-Speech. Put longer lists, code explanations, or multi-line breakdowns in detailedDisplay.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = contents,
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = JarvisNetworkClient.buildJarvisResponseSchema(),
                    temperature = 0.65f
                )
            )

            val modelName = settings.aiModel.ifBlank { "gemini-3.5-flash" }
            val response = JarvisNetworkClient.geminiService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: throw IllegalStateException("Empty response from Gemini model $modelName.")

            runCatching {
                JarvisNetworkClient.json.decodeFromString<JarvisAiStructuredResponse>(rawJson)
            }.getOrElse {
                JarvisAiStructuredResponse(
                    spokenReply = rawJson.replace(Regex("[{}\"]"), "").take(280),
                    detailedDisplay = rawJson,
                    detectedLanguage = detectedLanguage,
                    actionType = "NONE"
                )
            }
        }
    }
}
