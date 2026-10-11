package com.example.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.service.ReminderBroadcastReceiver
import java.util.Locale

data class DeviceTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = 28.0f,
    val isOnline: Boolean = true,
    val networkType: String = "WIFI",
    val storageFreeGb: Float = 32.0f,
    val storageTotalGb: Float = 64.0f,
    val mediaVolumePercent: Int = 70,
    val isFlashlightOn: Boolean = false,
    val deviceModel: String = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class ContactEntry(
    val name: String,
    val phoneNumber: String
)

class AndroidSystemController(private val context: Context) {

    private var torchEnabled: Boolean = false

    fun readTelemetry(): DeviceTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280

        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connMgr?.activeNetwork
        val caps = activeNet?.let { connMgr.getNetworkCapabilities(it) }
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netType = when {
            caps == null -> "OFFLINE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI-LINK"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL-5G/LTE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "ONLINE"
        }

        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalBytes = statFs?.totalBytes ?: (64L * 1024 * 1024 * 1024)
        val freeBytes = statFs?.availableBytes ?: (32L * 1024 * 1024 * 1024)
        val totalGb = totalBytes.toFloat() / (1024f * 1024f * 1024f)
        val freeGb = freeBytes.toFloat() / (1024f * 1024f * 1024f)

        val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val curVol = audioMgr?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val maxVol = (audioMgr?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val volPct = (curVol * 100) / maxVol

        return DeviceTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempCelsius = tempTenths / 10f,
            isOnline = isOnline,
            networkType = netType,
            storageFreeGb = freeGb,
            storageTotalGb = totalGb,
            mediaVolumePercent = volPct,
            isFlashlightOn = torchEnabled
        )
    }

    fun setFlashlight(enabled: Boolean): Result<String> = runCatching {
        val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = camMgr.cameraIdList.firstOrNull()
            ?: throw IllegalStateException("No camera hardware module detected on this unit.")
        camMgr.setTorchMode(cameraId, enabled)
        torchEnabled = enabled
        if (enabled) "Tactical flashlight engaged, Sir." else "Tactical flashlight disengaged."
    }

    fun adjustMediaVolume(directionOrPercent: String): String {
        val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return "Audio subsystem inaccessible."
        val maxVol = audioMgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        when (directionOrPercent.uppercase(Locale.ROOT)) {
            "UP" -> audioMgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "DOWN" -> audioMgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "MUTE" -> audioMgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            else -> {
                val pct = directionOrPercent.filter { it.isDigit() }.toIntOrNull()
                if (pct != null) {
                    val target = ((pct.coerceIn(0, 100) * maxVol) / 100)
                    audioMgr.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
                } else {
                    audioMgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
            }
        }
        val current = audioMgr.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentPct = (current * 100) / maxVol
        return "Audio output level calibrated to $currentPct percent."
    }

    fun openSystemSettings(settingAction: String = Settings.ACTION_SETTINGS): String {
        return runCatching {
            val intent = Intent(settingAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "System configuration panel opened."
        }.getOrElse {
            "Unable to launch system panel: ${it.localizedMessage}"
        }
    }

    fun listInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            val label = info.loadLabel(pm)?.toString() ?: pkg
            InstalledAppInfo(appName = label, packageName = pkg)
        }.distinctBy { it.packageName }.sortedBy { it.appName.lowercase(Locale.ROOT) }
    }

    fun openInstalledAppByName(queryName: String): String {
        val clean = queryName.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return "Please specify which application to launch."
        val apps = listInstalledApps()
        val match = apps.firstOrNull { it.appName.lowercase(Locale.ROOT) == clean }
            ?: apps.firstOrNull { it.appName.lowercase(Locale.ROOT).contains(clean) }
            ?: apps.firstOrNull { it.packageName.lowercase(Locale.ROOT).contains(clean) }

        if (match != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return "Launching ${match.appName}, Sir."
            }
        }
        return when {
            clean.contains("setting") -> openSystemSettings(Settings.ACTION_SETTINGS)
            clean.contains("wifi") || clean.contains("wi-fi") -> openSystemSettings(Settings.ACTION_WIFI_SETTINGS)
            clean.contains("bluetooth") -> openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            clean.contains("location") || clean.contains("gps") -> openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            clean.contains("camera") -> launchCameraIntent()
            clean.contains("map") -> openMapsNavigation("")
            clean.contains("phone") || clean.contains("dial") -> openDialer("")
            else -> "Application '$queryName' was not found among installed launcher packages."
        }
    }

    fun launchCameraIntent(): String = runCatching {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Optical camera interface launched."
    }.getOrElse {
        "Camera intent unavailable on this emulator or device."
    }

    fun openDialer(phoneNumber: String): String = runCatching {
        val uri = if (phoneNumber.isBlank()) Uri.parse("tel:") else Uri.parse("tel:${Uri.encode(phoneNumber)}")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        if (phoneNumber.isBlank()) "Communications dialer opened." else "Dialer prepared for $phoneNumber. Please tap call to confirm."
    }.getOrElse {
        "Unable to open phone dialer: ${it.localizedMessage}"
    }

    fun openEmergencyDialer(): String = runCatching {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Emergency communications interface prepared."
    }.getOrElse {
        "Unable to launch emergency dialer."
    }

    fun findContactsByName(nameQuery: String): List<ContactEntry> {
        val results = mutableListOf<ContactEntry>()
        runCatching {
            val resolver = context.contentResolver
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%${nameQuery.trim()}%")
            val cursor: Cursor? = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext() && results.size < 10) {
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "" else ""
                    val num = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
                    if (name.isNotBlank() && num.isNotBlank()) {
                        results.add(ContactEntry(name, num))
                    }
                }
            }
        }
        return results
    }

    fun composeSms(recipientNumberOrName: String, body: String): String = runCatching {
        var resolvedNumber = recipientNumberOrName
        if (recipientNumberOrName.any { it.isLetter() }) {
            val contact = findContactsByName(recipientNumberOrName).firstOrNull()
            if (contact != null) {
                resolvedNumber = contact.phoneNumber
            }
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(resolvedNumber)}")
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "SMS message drafted for $recipientNumberOrName. Please review and press Send in your messaging app."
    }.getOrElse {
        "Unable to open SMS composer: ${it.localizedMessage}"
    }

    fun composeEmail(recipient: String, subject: String, body: String): String = runCatching {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(recipient)}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Email draft prepared. Please verify and press send in your email client."
    }.getOrElse {
        "Unable to launch email client: ${it.localizedMessage}"
    }

    fun openMapsNavigation(queryOrAddress: String): String = runCatching {
        val uri = if (queryOrAddress.isBlank()) {
            Uri.parse("geo:0,0?q=nearby")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(queryOrAddress)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        if (queryOrAddress.isBlank()) "Tactical maps interface opened." else "Navigation coordinates plotted for $queryOrAddress."
    }.getOrElse {
        openWebSearch("maps $queryOrAddress")
    }

    fun openWebSearch(query: String): String = runCatching {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Opened web search for '$query'."
    }.getOrElse {
        "Unable to launch browser search."
    }

    fun setSystemAlarm(hour: Int, minute: Int, label: String): String = runCatching {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "JARVIS Alarm" })
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        String.format(Locale.US, "System alarm configured for %02d:%02d (%s).", hour, minute, label.ifBlank { "JARVIS" })
    }.getOrElse {
        "Unable to launch system clock alarm: ${it.localizedMessage}"
    }

    fun setSystemTimer(seconds: Int, label: String): String = runCatching {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(5))
            putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "JARVIS Timer" })
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Countdown timer set for $seconds seconds."
    }.getOrElse {
        "Unable to launch system timer: ${it.localizedMessage}"
    }

    fun createCalendarEvent(title: String, description: String, startMillis: Long): String = runCatching {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 3600_000L)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        "Calendar event '$title' prepared in system calendar."
    }.getOrElse {
        "Unable to launch calendar event creator."
    }

    fun scheduleLocalReminderNotification(reminderId: Int, title: String, message: String, triggerAtMillis: Long) {
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                putExtra("REMINDER_ID", reminderId)
                putExtra("REMINDER_TITLE", title)
                putExtra("REMINDER_MESSAGE", message)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
