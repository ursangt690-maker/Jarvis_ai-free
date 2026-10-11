package com.example.ui.screens

import android.provider.Settings
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import com.example.domain.ContactEntry
import com.example.domain.DeviceTelemetry
import com.example.domain.InstalledAppInfo
import com.example.ui.components.HudFramedCard
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisOnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemsAndCommsScreen(
    telemetry: DeviceTelemetry,
    installedApps: List<InstalledAppInfo>,
    foundContacts: List<ContactEntry>,
    liveToolOutput: String,
    hasContactsPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onExecuteAction: (actionType: String, target: String, payload: String) -> Unit,
    onSearchContacts: (String) -> Unit,
    onFetchWeather: (String) -> Unit,
    onConvertCurrency: (base: String, target: String, amount: Double) -> Unit,
    onSummarizeUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var appFilterQuery by rememberSaveable { mutableStateOf("") }
    var contactQuery by rememberSaveable { mutableStateOf("") }
    var smsRecipient by rememberSaveable { mutableStateOf("") }
    var smsMessage by rememberSaveable { mutableStateOf("") }
    var emailRecipient by rememberSaveable { mutableStateOf("") }
    var emailBody by rememberSaveable { mutableStateOf("") }
    var weatherCity by rememberSaveable { mutableStateOf("Kathmandu") }
    var currencyAmount by rememberSaveable { mutableStateOf("100") }
    var currencyBase by rememberSaveable { mutableStateOf("USD") }
    var currencyTarget by rememberSaveable { mutableStateOf("NPR") }
    var webUrlOrQuery by rememberSaveable { mutableStateOf("") }
    var volumeSlider by rememberSaveable(telemetry.mediaVolumePercent) {
        mutableFloatStateOf(telemetry.mediaVolumePercent.toFloat())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ANDROID SYSTEMS, COMMS & INTEL",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Hardware Telemetry & Quick Toggles
        HudFramedCard(
            title = "HARDWARE TELEMETRY & CONTROLS",
            subtitle = telemetry.deviceModel
        ) {
            Text(
                text = "OS: ${telemetry.androidVersion} • Network: ${telemetry.networkType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val usedStorage = (telemetry.storageTotalGb - telemetry.storageFreeGb).coerceAtLeast(0f)
            val storageFraction = (usedStorage / telemetry.storageTotalGb.coerceAtLeast(1f)).coerceIn(0f, 1f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Power Core: ${telemetry.batteryPercent}% (${telemetry.batteryTempCelsius}°C)",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = String.format(Locale.US, "Storage Free: %.1f / %.1f GB", telemetry.storageFreeGb, telemetry.storageTotalGb),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { storageFraction },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Media Volume Control Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Media Volume",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Volume: ${volumeSlider.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = volumeSlider,
                    onValueChange = { volumeSlider = it },
                    onValueChangeFinished = {
                        onExecuteAction("VOLUME_SET", volumeSlider.toInt().toString(), "")
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("media_volume_slider")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cmd = if (telemetry.isFlashlightOn) "FLASHLIGHT_OFF" else "FLASHLIGHT_ON"
                        onExecuteAction(cmd, "", "")
                    },
                    modifier = Modifier.testTag("toggle_flashlight_button")
                ) {
                    Icon(
                        imageVector = if (telemetry.isFlashlightOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (telemetry.isFlashlightOn) "TORCH OFF" else "TORCH ON")
                }

                OutlinedButton(onClick = { onExecuteAction("OPEN_WIFI", "", "") }) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WI-FI")
                }

                OutlinedButton(onClick = { onExecuteAction("OPEN_BLUETOOTH", "", "") }) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BLUETOOTH")
                }

                OutlinedButton(onClick = { onExecuteAction("OPEN_LOCATION", "", "") }) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GPS")
                }

                OutlinedButton(onClick = { onExecuteAction("OPEN_SETTINGS", Settings.ACTION_DISPLAY_SETTINGS, "") }) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DISPLAY")
                }

                OutlinedButton(onClick = { onExecuteAction("OPEN_CAMERA", "", "") }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CAMERA")
                }

                OutlinedButton(onClick = { onExecuteAction("OPEN_MAPS", "", "") }) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MAPS")
                }
            }
        }

        // 2. Calling, Contacts & Messaging Hub
        HudFramedCard(
            title = "COMMUNICATIONS & CONTACTS",
            subtitle = "PERMISSION-VERIFIED"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = contactQuery,
                    onValueChange = { contactQuery = it },
                    label = { Text("Search Contact or Dial Number") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_search_input")
                )
                Button(
                    onClick = {
                        if (!hasContactsPermission && contactQuery.any { it.isLetter() }) {
                            onRequestContactsPermission()
                        } else {
                            onSearchContacts(contactQuery)
                        }
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Find Contact")
                }
            }

            if (foundContacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                foundContacts.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, style = MaterialTheme.typography.bodyMedium)
                            Text(entry.phoneNumber, style = MaterialTheme.typography.labelSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onExecuteAction("DIAL_NUMBER", entry.phoneNumber, "") }) {
                                Text("DIAL")
                            }
                            OutlinedButton(onClick = { smsRecipient = entry.phoneNumber }) {
                                Text("SMS")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onExecuteAction("DIAL_NUMBER", contactQuery, "") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN DIALER")
                }
                Button(
                    onClick = { onExecuteAction("DIAL_NUMBER", "112", "") },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCriticalRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EMERGENCY")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SMS Composer
            OutlinedTextField(
                value = smsRecipient,
                onValueChange = { smsRecipient = it },
                label = { Text("SMS Recipient (Name or Number)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = smsMessage,
                    onValueChange = { smsMessage = it },
                    label = { Text("SMS Message Content") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (smsRecipient.isNotBlank()) {
                            onExecuteAction("COMPOSE_SMS", smsRecipient, smsMessage)
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Prepare SMS")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Email Composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = emailRecipient,
                    onValueChange = { emailRecipient = it },
                    label = { Text("Email Recipient") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = emailBody,
                    onValueChange = { emailBody = it },
                    label = { Text("Email Draft") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onExecuteAction("COMPOSE_EMAIL", emailRecipient, emailBody)
                    }
                ) {
                    Icon(Icons.Default.Email, contentDescription = "Compose Email")
                }
            }
        }

        // 3. Live Internet & Global Information Hub
        HudFramedCard(
            title = "GLOBAL INTEL, WEATHER & CURRENCY",
            subtitle = "LIVE REST SERVICES"
        ) {
            if (liveToolOutput.isNotBlank()) {
                Text(
                    text = liveToolOutput,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisOnlineGreen,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Live Weather
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = weatherCity,
                    onValueChange = { weatherCity = it },
                    label = { Text("City for Live Weather") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weather_city_input")
                )
                Button(
                    onClick = { onFetchWeather(weatherCity) },
                    modifier = Modifier.testTag("fetch_weather_button")
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = "Fetch Weather")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WEATHER")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Currency Conversion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currencyAmount,
                    onValueChange = { currencyAmount = it },
                    label = { Text("Amt") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currencyBase,
                    onValueChange = { currencyBase = it },
                    label = { Text("From") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currencyTarget,
                    onValueChange = { currencyTarget = it },
                    label = { Text("To") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val amt = currencyAmount.toDoubleOrNull() ?: 1.0
                        onConvertCurrency(currencyBase, currencyTarget, amt)
                    },
                    modifier = Modifier.testTag("convert_currency_button")
                ) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = "Convert Currency")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Web Search & Online Content Summarizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = webUrlOrQuery,
                    onValueChange = { webUrlOrQuery = it },
                    label = { Text("Web Search Query or URL to Summarize") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        if (webUrlOrQuery.isNotBlank()) {
                            onExecuteAction("WEB_SEARCH", webUrlOrQuery, "")
                        }
                    }
                ) {
                    Icon(Icons.Default.Public, contentDescription = "Search Web")
                }
                Button(
                    onClick = {
                        if (webUrlOrQuery.isNotBlank()) {
                            onSummarizeUrl(webUrlOrQuery)
                        }
                    }
                ) {
                    Text("SUMMARIZE")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // World Clock Readout (UTC, New York, London, New Delhi, Kathmandu, Tokyo)
            Text(
                text = "WORLD CLOCK TELEMETRY:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val zones = listOf(
                "UTC" to "UTC",
                "NYC" to "America/New_York",
                "LON" to "Europe/London",
                "DEL" to "Asia/Kolkata",
                "KTM" to "Asia/Kathmandu",
                "TYO" to "Asia/Tokyo"
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val now = Date()
                zones.forEach { (code, tzId) ->
                    val fmt = SimpleDateFormat("HH:mm", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone(tzId)
                    }
                    AssistChip(
                        onClick = {},
                        label = { Text("$code ${fmt.format(now)}", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // 4. Installed Application Launcher
        HudFramedCard(
            title = "INSTALLED APPLICATION LAUNCHER (${installedApps.size})",
            subtitle = "VOICE OR TAP"
        ) {
            OutlinedTextField(
                value = appFilterQuery,
                onValueChange = { appFilterQuery = it },
                label = { Text("Filter Installed Apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            val filtered = installedApps.filter {
                appFilterQuery.isBlank() || it.appName.contains(appFilterQuery, ignoreCase = true)
            }.take(18)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filtered.forEach { app ->
                    AssistChip(
                        onClick = { onExecuteAction("OPEN_APP", app.appName, "") },
                        label = { Text(app.appName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}
