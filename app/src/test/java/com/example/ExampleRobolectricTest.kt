package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OwnerProfile
import com.example.nlp.AssistantIntent
import com.example.nlp.LanguageEngine
import com.example.security.BiometricVerificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aegis AI Assistant", appName)
  }

  @Test
  fun `test multilingual language detection`() {
    // Bengali script
    assertEquals("bn", LanguageEngine.detectLanguage("হোয়াটসঅ্যাপ খোলো"))
    // Hindi script
    assertEquals("hi", LanguageEngine.detectLanguage("व्हाट्सएप खोलो"))
    // Hinglish
    assertEquals("hi", LanguageEngine.detectLanguage("WhatsApp open karo"))
    // English
    assertEquals("en", LanguageEngine.detectLanguage("Open WhatsApp and turn on torch"))
  }

  @Test
  fun `test intent parsing for app open and device controls`() {
    val result1 = LanguageEngine.parseInput(
      rawText = "Hey Mano, open WhatsApp",
      wakeWord = "Hey Mano",
      assistantName = "Mano AI",
      configuredPrimaryLang = "en"
    )
    assertTrue(result1.isWakeWordDetected)
    assertTrue(result1.intent is AssistantIntent.OpenApp)
    assertEquals("WhatsApp", (result1.intent as AssistantIntent.OpenApp).appName)

    val result2 = LanguageEngine.parseInput(
      rawText = "Torch on",
      wakeWord = "Hey Aegis",
      assistantName = "Aegis",
      configuredPrimaryLang = "en"
    )
    assertTrue(result2.intent is AssistantIntent.ToggleTorch)
    assertTrue((result2.intent as AssistantIntent.ToggleTorch).enable)
  }

  @Test
  fun `test voice signature biometric protection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = BiometricVerificationManager(context)

    val profile = OwnerProfile(
      ownerName = "Alex",
      voiceVerificationEnabled = true,
      voicePitchMean = 165f
    )

    // Owner test
    val ownerResult = manager.verifyVoiceSignature(profile, isGuestSimulation = false, samplePitch = 166f)
    assertTrue(ownerResult.isOwnerVerified)
    assertTrue(ownerResult.confidence > 0.8f)

    // Guest simulator test
    val guestResult = manager.verifyVoiceSignature(profile, isGuestSimulation = true)
    assertFalse(guestResult.isOwnerVerified)
    assertTrue(guestResult.confidence < 0.5f)
  }
}
