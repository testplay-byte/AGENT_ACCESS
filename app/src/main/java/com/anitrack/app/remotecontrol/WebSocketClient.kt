package com.anitrack.app.remotecontrol

import android.util.Log
import com.anitrack.app.remotecontrol.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

class WebSocketClient() {
    
    companion object {
        @Volatile
        private var instance: WebSocketClient? = null
        
        const val DEFAULT_WS_URL = "ws://localhost:8080/remote-control"
        
        fun getInstance(): WebSocketClient {
            return instance ?: synchronized(this) {
                instance ?: WebSocketClient().also { instance = it }
            }
        }
    }
    
    private val TAG = "WebSocketClient"
    
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient? = null
    private var reconnectJob: Job? = null
    
    private val _connectionState = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()
    
    private val _messageReceived = MutableSharedFlow<CommandMessage>()
    val messageReceived: SharedFlow<CommandMessage> = _messageReceived.asSharedFlow()
    
    private val _responseReceived = MutableSharedFlow<String>()
    val responseReceived: SharedFlow<String> = _responseReceived.asSharedFlow()
    
    private var serverUrl: String = ""
    private var autoReconnect: Boolean = true
    private var reconnectDelay: Long = 5000L // 5 seconds
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val webSocketListener = object : WebSocketListener() {
        
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            scope.launch {
                _connectionState.emit(ConnectionState.CONNECTED)
                
                // Send connection message with device info
                sendConnectionMessage()
            }
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Message received: ${text.take(100)}...")
            
            try {
                // Try to parse as command message
                val command = json.decodeFromString<CommandMessage>(text)
                scope.launch { _messageReceived.emit(command) }
            } catch (e: Exception) {
                // If not a command, emit as raw response
                scope.launch { _responseReceived.emit(text) }
            }
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            scope.launch {
                _connectionState.emit(ConnectionState.ERROR)
            }
            
            if (autoReconnect) {
                scheduleReconnect()
            }
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            scope.launch {
                _connectionState.emit(ConnectionState.DISCONNECTED)
            }
            
            if (autoReconnect) {
                scheduleReconnect()
            }
        }
    }

    fun connect(url: String) {
        this.serverUrl = url
        
        // Close existing connection if any
        disconnect()
        
        scope.launch {
            _connectionState.emit(ConnectionState.CONNECTING)
        }
        
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MINUTES) // No timeout for reads
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS) // Keep alive ping
            .build()
        
        val request = Request.Builder()
            .url(serverUrl)
            .build()
        
        webSocket = okHttpClient?.newWebSocket(request, webSocketListener)
    }
    
    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        
        scope.launch {
            _connectionState.emit(ConnectionState.DISCONNECTED)
        }
    }
    
    fun sendMessage(message: Any): Boolean {
        return try {
            val jsonString = when (message) {
                is String -> message
                else -> json.encodeToString(message)
            }
            
            val sent = webSocket?.send(jsonString) ?: false
            
            if (!sent) {
                Log.e(TAG, "Failed to send message")
            }
            
            sent
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            false
        }
    }
    
    fun sendResponse(response: ResponseMessage): Boolean {
        return sendMessage(response)
    }
    
    fun sendCommand(command: CommandMessage): Boolean {
        return sendMessage(command)
    }
    
    private fun sendConnectionMessage() {
        val deviceInfo = DeviceInfo(
            model = android.os.Build.MODEL,
            manufacturer = android.os.Build.MANUFACTURER,
            androidVersion = android.os.Build.VERSION.RELEASE,
            sdkVersion = android.os.Build.VERSION.SDK_INT,
            screenSize = "", // Will be filled by service
            density = 1.0f,
            appVersion = "1.0.0",
            packageName = "com.anitrack.app"
        )
        
        val connectionMessage = ConnectionMessage(
            type = "connect",
            deviceId = getDeviceId(),
            deviceInfo = deviceInfo
        )
        
        sendMessage(connectionMessage)
    }
    
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.emit(ConnectionState.RECONNECTING)
            
            while (isActive && autoReconnect) {
                delay(reconnectDelay)
                
                if (!autoReconnect) break
                
                Log.d(TAG, "Attempting to reconnect...")
                connect(serverUrl)
                
                // Wait for connection result
                delay(3000)
                
                val currentState = connectionState.replayCache.lastOrNull()
                if (currentState == ConnectionState.CONNECTED) {
                    break
                }
                
                // Exponential backoff
                reconnectDelay = minOf(reconnectDelay * 2, 60000L) // Max 1 minute
            }
        }
    }
    
    fun setAutoReconnect(enabled: Boolean) {
        this.autoReconnect = enabled
    }
    
    fun getCurrentState(): ConnectionState {
        return connectionState.replayCache.lastOrNull() ?: ConnectionState.DISCONNECTED
    }
    
    private fun getDeviceId(): String {
        // Use a hash of build fingerprint as fallback device ID
        // The actual ANDROID_ID will be set when context is available
        return java.util.UUID.nameUUIDFromBytes(
            "${android.os.Build.BRAND}${android.os.Build.MODEL}${android.os.Build.FINGERPRINT}".toByteArray()
        ).toString()
    }
    
    /**
     * Get the actual Android ID - should be called with application context
     */
    fun getAndroidId(context: android.content.Context): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: getDeviceId()
    }
    

}
