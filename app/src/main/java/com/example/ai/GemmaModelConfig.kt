package com.example.ai

import java.io.File

enum class AccelerationDevice {
    NPU, // Neural Processing Unit (Qualcomm QNN / MediaTek APU / NNAPI)
    GPU, // OpenCL / Vulkan
    CPU  // Multi-threaded NEON CPU fallback
}

enum class ModelStatus {
    NOT_DOWNLOADED,
    UNLOADED,
    LOADING,
    READY,
    GENERATING,
    ERROR
}

data class GemmaModelConfig(
    val modelName: String = "Gemma 3 1B IT",
    val modelFileName: String = "gemma-3-1b-it.bin",
    val contextWindowTokens: Int = 4096,
    val maxOutputTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val preferredAcceleration: AccelerationDevice = AccelerationDevice.NPU,
    val idleUnloadTimeoutMs: Long = 90_000L // 90 seconds idle before unloading from RAM
)
