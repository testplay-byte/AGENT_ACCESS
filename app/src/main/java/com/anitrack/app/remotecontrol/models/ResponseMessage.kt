package com.anitrack.app.remotecontrol.models

import kotlinx.serialization.Serializable

// This file contains additional message types for completeness
// The main models are in CommandMessage.kt

@Serializable
data class ResponseMessage(
    val type: String,
    val requestId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true,
    val payload: ResponsePayload? = null,
    val error: String? = null
)

@Serializable
data class ResponsePayload(
    val screenshot: ScreenshotResponse? = null,
    val logcat: LogcatData? = null,
    val deviceInfo: DeviceInfo? = null,
    val uiHierarchy: UiHierarchy? = null,
    val ack: AckResponse? = null
)

@Serializable
data class ScreenshotResponse(
    val image: String,
    val width: Int,
    val height: Int,
    val format: String = "jpeg",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LogcatData(
    val logs: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val screenSize: String,
    val density: Float,
    val appVersion: String,
    val packageName: String
)

@Serializable
data class UiHierarchy(
    val xml: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AckResponse(
    val commandType: String,
    val executedAt: Long = System.currentTimeMillis()
)
