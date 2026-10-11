package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.AndroidSystemController
import com.example.domain.VoiceAssistantEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS AI", appName)
    }

    @Test
    fun `language detection identifies English, Hindi, and Nepali`() {
        assertEquals("en-US", VoiceAssistantEngine.detectLanguageFromScript("Turn on the flashlight", "AUTO"))
        assertEquals("ne-NP", VoiceAssistantEngine.detectLanguageFromScript("नमस्ते जार्विस, आजको मौसम कस्तो छ?", "AUTO"))
        assertEquals("hi-IN", VoiceAssistantEngine.detectLanguageFromScript("नमस्ते जार्विस, आज का मौसम कैसा है?", "AUTO"))
    }

    @Test
    fun `system controller reads valid device telemetry`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = AndroidSystemController(context)
        val telemetry = controller.readTelemetry()
        assertTrue(telemetry.batteryPercent in 0..100)
        assertTrue(telemetry.deviceModel.isNotBlank())
    }
}
