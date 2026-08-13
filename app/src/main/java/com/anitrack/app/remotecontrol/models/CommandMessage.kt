package com.anitrack.app.remotecontrol.models

import kotlinx.serialization.Serializable

// ==================== COMMANDS (Server -> Device) ====================

@Serializable
data class CommandMessage(
    val type: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val payload: CommandPayload
)

@Serializable
data class CommandPayload(
    // Tap command
    val tap: TapCommand? = null,
    
    // Swipe command
    val swipe: SwipeCommand? = null,
    
    // Screenshot request
    val screenshot: ScreenshotRequest? = null,
    
    // Logcat command
    val logcat: LogcatCommand? = null,
    
    // Navigation commands
    val navigation: NavigationCommand? = null,
    
    // Text input
    val text: TextCommand? = null
)

@Serializable
data class TapCommand(
    val x: Float,
    val y: Float,
    val duration: Long = 100L
)

@Serializable
data class SwipeCommand(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val duration: Long = 300L
)

@Serializable
data class ScreenshotRequest(
    val quality: Int = 80,
    val format: String = "jpeg"
)

@Serializable
data class LogcatCommand(
    val action: String, // "start" | "stop"
    val filter: String? = null,
    val maxLines: Int = 100
)

@Serializable
data class NavigationCommand(
    val action: String, // "back" | "home" | "recent"
)

@Serializable
data class TextCommand(
    val text: String,
    val action: String = "input", // "input" | "paste"
)

// ==================== RESPONSES (Device -> Server) ====================

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
    // Screenshot response
    val screenshot: ScreenshotResponse? = null,
    
    // Logcat data
    val logcat: LogcatData? = null,
    
    // Device info
    val deviceInfo: DeviceInfo? = null,
    
    // UI hierarchy
    val uiHierarchy: UiHierarchy? = null,
    
    // Command acknowledgment
    val ack: AckResponse? = null
)

@Serializable
data class ScreenshotResponse(
    val image: String, // Base64 encoded image
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

// ==================== CONNECTION MESSAGES ====================

@Serializable
data class ConnectionMessage(
    val type: String, // "connect" | "disconnect" | "ping" | "pong"
    val deviceId: String? = null,
    val deviceInfo: DeviceInfo? = null,
    val timestamp: Long = System.currentTimeMillis()
)
