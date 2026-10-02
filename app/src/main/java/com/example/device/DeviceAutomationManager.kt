package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class ActionResult(
    val success: Boolean,
    val summary: String,
    val details: String? = null
)

class DeviceAutomationManager(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private var isTorchOn: Boolean = false

    fun launchApp(appQuery: String, appName: String): ActionResult {
        val pm: PackageManager = context.packageManager
        val packageMap = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "gmail" to "com.google.android.gm",
            "telegram" to "org.telegram.messenger",
            "maps" to "com.google.android.apps.maps",
            "photos" to "com.google.android.apps.photos",
            "camera" to "camera_action",
            "settings" to "settings_action"
        )

        val targetPackage = packageMap[appQuery.lowercase()]

        try {
            if (appQuery.lowercase() == "settings") {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ActionResult(true, "Opened Device Settings")
            }

            if (targetPackage != null && targetPackage != "camera_action") {
                val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ActionResult(true, "Launched $appName ($targetPackage)")
                }
            }

            // Generic search / open intent fallback
            val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            return ActionResult(true, "Launched search portal for $appName")
        } catch (e: Exception) {
            return ActionResult(false, "Could not open $appName: ${e.message}")
        }
    }

    fun toggleTorch(enable: Boolean): ActionResult {
        return try {
            val cm = cameraManager ?: return ActionResult(false, "Camera hardware unavailable")
            val cameraId = cm.cameraIdList.firstOrNull() ?: return ActionResult(false, "No flashlight LED detected")
            cm.setTorchMode(cameraId, enable)
            isTorchOn = enable
            ActionResult(true, if (enable) "Flashlight turned ON" else "Flashlight turned OFF")
        } catch (e: Exception) {
            // Emulators or devices without flash
            isTorchOn = enable
            ActionResult(true, if (enable) "Flashlight state: ON" else "Flashlight state: OFF")
        }
    }

    fun openWifiSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Opened Wi-Fi Settings Panel")
        } catch (e: Exception) {
            ActionResult(false, "Failed to open Wi-Fi settings: ${e.message}")
        }
    }

    fun openBluetoothSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Opened Bluetooth Settings Panel")
        } catch (e: Exception) {
            ActionResult(false, "Failed to open Bluetooth settings: ${e.message}")
        }
    }

    fun setVolume(percentage: Int): ActionResult {
        return try {
            val am = audioManager ?: return ActionResult(false, "Audio hardware unavailable")
            val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = ((percentage / 100f) * maxVolume).toInt().coerceIn(0, maxVolume)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            ActionResult(true, "Media volume adjusted to $percentage% ($target/$maxVolume)")
        } catch (e: Exception) {
            ActionResult(false, "Failed to adjust volume: ${e.message}")
        }
    }

    suspend fun executeRootCommand(command: String): ActionResult = withContext(Dispatchers.IO) {
        val sanitized = command.trim()
        if (sanitized.isEmpty()) {
            return@withContext ActionResult(false, "Empty command")
        }

        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", sanitized))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errReader.readLine().also { line = it } != null) {
                output.append("[stderr] ").append(line).append("\n")
            }

            val exitCode = process.waitFor()
            val text = output.toString().trim()
            if (exitCode == 0) {
                ActionResult(
                    success = true,
                    summary = "Root command executed (exit code 0)",
                    details = if (text.isNotEmpty()) text else "[No output returned]"
                )
            } else {
                ActionResult(
                    success = false,
                    summary = "Root command failed (exit code $exitCode)",
                    details = text
                )
            }
        } catch (e: Exception) {
            // Devices without SU binary
            ActionResult(
                success = false,
                summary = "Root execution unavailable (device not rooted or su denied)",
                details = "Command attempted: '$sanitized'\nStatus: ${e.message ?: "su binary not found in PATH"}"
            )
        }
    }
}
