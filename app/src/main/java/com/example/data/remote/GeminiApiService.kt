package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = 0.7f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class JarvisAiStructuredResponse(
    val spokenReply: String,
    val detailedDisplay: String = "",
    val detectedLanguage: String = "en-US", // en-US, hi-IN, ne-NP
    val actionType: String = "NONE",
    val actionTarget: String = "",
    val actionPayload: String = "",
    val requiresConfirmation: Boolean = false,
    val memoryFactToSave: String = "",
    val multiStepPlan: List<String> = emptyList(),
    val proactiveSuggestion: String = ""
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object JarvisNetworkClient {
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun resolveApiKey(customKey: String): String {
        val trimmedCustom = customKey.trim()
        if (trimmedCustom.isNotEmpty() && trimmedCustom != "MY_GEMINI_API_KEY") {
            return trimmedCustom
        }
        val buildKey = runCatching { BuildConfig.GEMINI_API_KEY.trim() }.getOrDefault("")
        return if (buildKey == "MY_GEMINI_API_KEY") "" else buildKey
    }

    fun isApiKeyConfigured(customKey: String): Boolean {
        return resolveApiKey(customKey).isNotBlank()
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Fetches live weather from Open-Meteo geocoding + weather API (no API key required).
     */
    suspend fun fetchLiveWeather(cityOrQuery: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanCity = cityOrQuery.trim().ifEmpty { "New York" }
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(cleanCity, "UTF-8")}&count=1&language=en&format=json"
            val geoReq = Request.Builder().url(geoUrl).get().build()
            val geoBody = okHttpClient.newCall(geoReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Geocoding HTTP ${resp.code}")
                resp.body?.string() ?: throw IllegalStateException("Empty geocoding response")
            }

            val geoJson = json.parseToJsonElement(geoBody).jsonObject
            val firstResult = geoJson["results"]?.jsonArray?.firstOrNull()?.jsonObject
                ?: return@runCatching "Could not locate coordinates for '$cleanCity'."

            val name = firstResult["name"]?.jsonPrimitive?.content ?: cleanCity
            val country = firstResult["country"]?.jsonPrimitive?.content ?: ""
            val lat = firstResult["latitude"]?.jsonPrimitive?.content ?: "0.0"
            val lon = firstResult["longitude"]?.jsonPrimitive?.content ?: "0.0"

            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code&timezone=auto"
            val weatherReq = Request.Builder().url(weatherUrl).get().build()
            val weatherBody = okHttpClient.newCall(weatherReq).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Weather HTTP ${resp.code}")
                resp.body?.string() ?: throw IllegalStateException("Empty weather response")
            }

            val wJson = json.parseToJsonElement(weatherBody).jsonObject
            val current = wJson["current"]?.jsonObject
            val temp = current?.get("temperature_2m")?.jsonPrimitive?.content ?: "--"
            val feelsLike = current?.get("apparent_temperature")?.jsonPrimitive?.content ?: "--"
            val humidity = current?.get("relative_humidity_2m")?.jsonPrimitive?.content ?: "--"
            val wind = current?.get("wind_speed_10m")?.jsonPrimitive?.content ?: "--"
            val code = current?.get("weather_code")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val condition = weatherCodeToDescription(code)

            "Live Telemetry for $name, $country: $condition, Temperature ${temp}°C (Feels like ${feelsLike}°C), Humidity ${humidity}%, Wind Speed ${wind} km/h."
        }
    }

    /**
     * Fetches live currency exchange rate from open.er-api.com (no API key required).
     */
    suspend fun fetchLiveCurrencyRate(base: String, target: String, amount: Double): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUpper = base.trim().uppercase()
            val targetUpper = target.trim().uppercase()
            val url = "https://open.er-api.com/v6/latest/$baseUpper"
            val req = Request.Builder().url(url).get().build()
            val body = okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Currency API HTTP ${resp.code}")
                resp.body?.string() ?: throw IllegalStateException("Empty currency response")
            }
            val parsed = json.parseToJsonElement(body).jsonObject
            val rates = parsed["rates"]?.jsonObject ?: throw IllegalStateException("Rates unavailable")
            val rate = rates[targetUpper]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: throw IllegalStateException("Currency code $targetUpper not found")
            val converted = amount * rate
            String.format(java.util.Locale.US, "%.2f %s = %.2f %s (Live Rate: 1 %s = %.4f %s)", amount, baseUpper, converted, targetUpper, baseUpper, rate, targetUpper)
        }
    }

    /**
     * Fetches plain text content from a webpage URL for online content summarization.
     */
    suspend fun fetchWebPageSnippet(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val req = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; JARVIS-Assistant)")
                .get()
                .build()
            val html = okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                resp.body?.string() ?: ""
            }
            val stripped = html
                .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(6000)
            if (stripped.isBlank()) throw IllegalStateException("No readable text extracted from $normalizedUrl")
            stripped
        }
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Fog / Atmospheric Haze"
        51, 53, 55 -> "Light Drizzle"
        61, 63, 65 -> "Rain Showers"
        71, 73, 75 -> "Snowfall"
        80, 81, 82 -> "Heavy Rain Showers"
        95, 96, 99 -> "Thunderstorm Activity"
        else -> "Standard Atmospheric Conditions"
    }

    fun buildJarvisResponseSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("spokenReply") {
                put("type", "STRING")
                put("description", "Natural, concise spoken reply in the user's language (English, Hindi, or Nepali) suitable for TTS.")
            }
            putJsonObject("detailedDisplay") {
                put("type", "STRING")
                put("description", "Optional structured HUD card body, code analysis, summary, or multi-line details.")
            }
            putJsonObject("detectedLanguage") {
                put("type", "STRING")
                put("description", "BCP-47 language tag of the reply: en-US, hi-IN, or ne-NP.")
            }
            putJsonObject("actionType") {
                put("type", "STRING")
                put(
                    "description",
                    "Optional Android action to execute: NONE, FLASHLIGHT_ON, FLASHLIGHT_OFF, OPEN_APP, OPEN_SETTINGS, OPEN_WIFI, OPEN_BLUETOOTH, OPEN_LOCATION, VOLUME_UP, VOLUME_DOWN, VOLUME_SET, SET_ALARM, SET_TIMER, ADD_REMINDER, ADD_TASK, ADD_NOTE, ADD_SHOPPING, DIAL_NUMBER, COMPOSE_SMS, COMPOSE_EMAIL, OPEN_MAPS, OPEN_CAMERA, WEB_SEARCH, WEATHER_LOOKUP"
                )
            }
            putJsonObject("actionTarget") {
                put("type", "STRING")
                put("description", "Target app name, phone number, contact name, city, alarm time (HH:mm), timer seconds, or note/task title.")
            }
            putJsonObject("actionPayload") {
                put("type", "STRING")
                put("description", "Secondary payload such as SMS message body, note content, email text, or search query.")
            }
            putJsonObject("requiresConfirmation") {
                put("type", "BOOLEAN")
                put("description", "True if this is a sensitive action (like calling or messaging) that should ask user confirmation first.")
            }
            putJsonObject("memoryFactToSave") {
                put("type", "STRING")
                put("description", "If the user shared a personal preference, fact, or routine to remember, extract a concise fact string here; otherwise empty string.")
            }
            putJsonObject("multiStepPlan") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "If the request involves multi-step planning, list the sequential steps.")
            }
            putJsonObject("proactiveSuggestion") {
                put("type", "STRING")
                put("description", "A relevant follow-up suggestion or proactive tip.")
            }
        }
        putJsonArray("required") {
            add(kotlinx.serialization.json.JsonPrimitive("spokenReply"))
            add(kotlinx.serialization.json.JsonPrimitive("detectedLanguage"))
            add(kotlinx.serialization.json.JsonPrimitive("actionType"))
        }
    }
}
