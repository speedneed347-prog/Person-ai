package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GemmaEngine
import com.example.ai.GemmaModelConfig
import com.example.data.db.AppDatabase
import com.example.data.db.AegisRepository
import com.example.data.model.LocalMemory
import com.example.data.model.OwnerProfile
import com.example.memory.MemoryManager
import com.example.nlp.AssistantIntent
import com.example.nlp.LanguageEngine
import com.example.security.face.RealFaceVerificationManager
import com.example.security.voice.RealVoiceAuthenticator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    assertEquals("bn", LanguageEngine.detectLanguage("হোয়াটসঅ্যাপ খোলো"))
    assertEquals("hi", LanguageEngine.detectLanguage("व्हाट्सएप खोलो"))
    assertEquals("hi", LanguageEngine.detectLanguage("WhatsApp open karo"))
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
  fun `test real acoustic speaker embedding and cosine verification`() {
    val authenticator = RealVoiceAuthenticator()

    // Generate simulated owner PCM signal (440 Hz fundamental tone)
    val samplePcm = ShortArray(4096) { i ->
      (kotlin.math.sin(2.0 * Math.PI * 440.0 * i / 16000.0) * 15000.0).toInt().toShort()
    }
    val ownerEmbedding = authenticator.extractEmbedding(samplePcm)
    assertEquals(RealVoiceAuthenticator.EMBEDDING_DIM, ownerEmbedding.size)

    // Verification with matching audio
    val verifyResult = authenticator.verifySpeaker(samplePcm, ownerEmbedding, threshold = 0.70f)
    assertTrue(verifyResult.isOwnerVerified)
    assertTrue(verifyResult.similarityScore > 0.85f)

    // Verification with distinct acoustic tone (150 Hz vs 440 Hz)
    val distinctPcm = ShortArray(4096) { i ->
      (kotlin.math.sin(2.0 * Math.PI * 150.0 * i / 16000.0) * 15000.0).toInt().toShort()
    }
    val distinctEmbedding = authenticator.extractEmbedding(distinctPcm)
    val distinctSimilarity = authenticator.computeCosineSimilarity(distinctEmbedding, ownerEmbedding)
    assertTrue(distinctSimilarity < verifyResult.similarityScore)

    // Distant speaker rejected at owner threshold
    val distinctVerify = authenticator.verifySpeaker(distinctPcm, ownerEmbedding, threshold = 0.82f)
    assertFalse(distinctVerify.isOwnerVerified)
  }

  @Test
  fun `test real face optical geometry extraction and verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val faceManager = RealFaceVerificationManager(context)

    val template = faceManager.extractFaceTemplate(
      faceWidth = 140f,
      faceHeight = 180f,
      leftEyeX = 40f,
      leftEyeY = 60f,
      rightEyeX = 100f,
      rightEyeY = 60f,
      noseX = 70f,
      noseY = 95f,
      mouthCenterX = 70f,
      mouthCenterY = 135f
    )

    assertEquals(RealFaceVerificationManager.VECTOR_LENGTH, template.featureVector.size)

    // Self verification should match with high confidence
    val result = faceManager.verifyFaceGeometry(template, template.featureVector)
    assertTrue(result.success)
    assertTrue(result.confidence > 0.95f)
  }

  @Test
  fun `test gemma local ai engine configuration and prompt generation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val gemma = GemmaEngine(context)

    assertNotNull(gemma.getModelFile())
    val response = gemma.generateResponse(
      userPrompt = "Who are you?",
      conversationHistory = emptyList(),
      relevantMemories = listOf(
        LocalMemory(
          category = "PREFERENCE",
          title = "Focus Hours",
          detail = "Prefers silent notifications in the morning."
        )
      ),
      assistantName = "Aegis",
      ownerName = "Alex",
      personality = "GUARDIAN"
    )

    assertTrue(response.contains("Aegis"))
  }

  @Test
  fun `test advanced memory manager search and ranking`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dao = AppDatabase.getDatabase(context).aegisDao()
    val repo = AegisRepository(dao)
    val memoryManager = MemoryManager(repo)

    memoryManager.recordMemory("ROUTINE", "Morning Run", "Daily 7 AM running routine in park", "run, morning")
    memoryManager.recordMemory("CONTACT", "Sarah Mobile", "+1-555-0199 Sarah emergency contact", "sarah, call")

    val searchResults = memoryManager.searchMemories("Sarah")
    assertTrue(searchResults.isNotEmpty())
    assertEquals("Sarah Mobile", searchResults.first().title)
  }
}
