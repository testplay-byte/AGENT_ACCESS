package com.anitrack.app.remotecontrol

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anitrack.app.MainActivity
import com.anitrack.app.R
import com.anitrack.app.remotecontrol.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RemoteControlService : Service() {
    
    private val TAG = "RemoteControlService"
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val webSocketClient: WebSocketClient by lazy { WebSocketClient.getInstance() }
    
    private val commandExecutor: CommandExecutor by lazy { CommandExecutor(this) }
    
    private val screenshotCapture: ScreenshotCapture by lazy { ScreenshotCapture(this) }
    
    private val logcatCollector: LogcatCollector by lazy { LogcatCollector() }
    
    private var overlay: RemoteControlOverlay? = null
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "remote_control_service"
        
        const val ACTION_START_SERVICE = "com.anitrack.app.remotecontrol.START"
        const val ACTION_STOP_SERVICE = "com.anitrack.app.remotecontrol.STOP"
        const val ACTION_TOGGLE_OVERLAY = "com.anitrack.app.remotecontrol.TOGGLE_OVERLAY"
        const val ACTION_CONNECT = "com.anitrack.app.remotecontrol.CONNECT"
        const val ACTION_DISCONNECT = "com.anitrack.app.remotecontrol.DISCONNECT"
        
        var isRunning: Boolean = false
            private set
        
        fun start(context: Context) {
            val intent = Intent(context, RemoteControlService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, RemoteControlService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
        
        fun toggleOverlay(context: Context) {
            val intent = Intent(context, RemoteControlService::class.java).apply {
                action = ACTION_TOGGLE_OVERLAY
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "RemoteControlService created")
        
        // Initialize components
        initializeComponents()
        
        // Start foreground notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Observe WebSocket connection state
        observeConnectionState()
        
        // Observe incoming messages
        observeMessages()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                connectToServer()
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            ACTION_TOGGLE_OVERLAY -> {
                toggleOverlay()
            }
            ACTION_CONNECT -> {
                connectToServer()
            }
            ACTION_DISCONNECT -> {
                disconnectFromServer()
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        
        cleanup()
        Log.d(TAG, "RemoteControlService destroyed")
    }
    
    private fun initializeComponents() {
        // Set up logcat collector with app package name
        logcatCollector.packageName = packageName
        logcatCollector.onNewLog = { log ->
            // Optionally send logs to server in real-time
            handleNewLog(log)
        }
        
        // Create overlay
        overlay = RemoteControlOverlay(
            context = this,
            onToggleVisibility = { /* Handle visibility change */ },
            onDismiss = { /* Handle dismiss */ }
        )
    }
    
    private fun connectToServer(url: String = WebSocketClient.DEFAULT_WS_URL) {
        serviceScope.launch {
            webSocketClient.connect(url)
        }
    }
    
    private fun disconnectFromServer() {
        webSocketClient.disconnect()
        updateNotification(ConnectionState.DISCONNECTED)
    }
    
    private fun showOverlay() {
        if (android.provider.Settings.canDrawOverlays(this)) {
            overlay?.show()
        } else {
            Log.w(TAG, "Cannot draw overlays - permission not granted")
        }
    }
    
    private fun hideOverlay() {
        overlay?.hide()
    }
    
    private fun toggleOverlay() {
        overlay?.toggleVisibility()
    }
    
    private fun observeConnectionState() {
        webSocketClient.connectionState
            .onEach { state ->
                Log.d(TAG, "Connection state changed: $state")
                
                // Update overlay status
                overlay?.updateConnectionState(state)
                
                // Update notification
                updateNotification(state)
                
                // Auto-start/stop logcat based on connection
                when (state) {
                    ConnectionState.CONNECTED -> {
                        showOverlay()
                    }
                    ConnectionState.DISCONNECTED,
                    ConnectionState.ERROR -> {
                        hideOverlay()
                    }
                    else -> {}
                }
            }
            .catch { e ->
                Log.e(TAG, "Error observing connection state", e)
            }
            .launchIn(serviceScope)
    }
    
    private fun observeMessages() {
        webSocketClient.messageReceived
            .onEach { command ->
                Log.d(TAG, "Processing command: ${command.type}")
                
                // Execute command and send response
                val response = commandExecutor.executeCommand(command)
                
                // Handle special cases that need async processing
                when (command.type) {
                    "screenshot" -> {
                        captureAndSendScreenshot(command.id, command.payload.screenshot)
                    }
                    "logcat" -> {
                        handleLogcatCommand(command.payload.logcat, command.id)
                    }
                    else -> {
                        webSocketClient.sendResponse(response)
                    }
                }
            }
            .catch { e ->
                Log.e(TAG, "Error observing messages", e)
            }
            .launchIn(serviceScope)
    }
    
    private suspend fun captureAndSendScreenshot(requestId: String?, request: ScreenshotRequest?) {
        try {
            val result = screenshotCapture.captureScreenshot(
                quality = request?.quality ?: ScreenshotCapture.DEFAULT_QUALITY
            )
            
            when (result) {
                is ScreenshotCapture.ScreenshotResult.Success -> {
                    val response = ResponseMessage(
                        type = "screenshot_response",
                        requestId = requestId,
                        success = true,
                        payload = com.anitrack.app.remotecontrol.models.ResponsePayload(
                            screenshot = ScreenshotResponse(
                                image = result.image,
                                width = result.width,
                                height = result.height,
                                format = result.format
                            )
                        )
                    )
                    webSocketClient.sendResponse(response)
                }
                is ScreenshotCapture.ScreenshotResult.Error -> {
                    val response = ResponseMessage(
                        type = "screenshot_response",
                        requestId = requestId,
                        success = false,
                        error = result.message
                    )
                    webSocketClient.sendResponse(response)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot", e)
            val response = ResponseMessage(
                type = "screenshot_response",
                requestId = requestId,
                success = false,
                error = e.message ?: "Screenshot failed"
            )
            webSocketClient.sendResponse(response)
        }
    }
    
    private fun handleLogcatCommand(command: LogcatCommand?, requestId: String?) {
        if (command == null) return
        
        when (command.action.lowercase()) {
            "start" -> {
                logcatCollector.start()
                val response = ResponseMessage(
                    type = "logcat_response",
                    requestId = requestId,
                    success = true,
                    payload = com.anitrack.app.remotecontrol.models.ResponsePayload(
                        ack = AckResponse("logcat_start")
                    )
                )
                webSocketClient.sendResponse(response)
            }
            "stop" -> {
                logcatCollector.stop()
                val logs = logcatCollector.getLogs()
                val response = ResponseMessage(
                    type = "logcat_response",
                    requestId = requestId,
                    success = true,
                    payload = com.anitrack.app.remotecontrol.models.ResponsePayload(
                        logcat = LogcatData(logs = logs.takeLast(100))
                    )
                )
                webSocketClient.sendResponse(response)
            }
            "get" -> {
                serviceScope.launch {
                    val logs = logcatCollector.captureRecentLogs(100)
                    val response = ResponseMessage(
                        type = "logcat_response",
                        requestId = requestId,
                        success = true,
                        payload = com.anitrack.app.remotecontrol.models.ResponsePayload(
                            logcat = LogcatData(logs = logs)
                        )
                    )
                    webSocketClient.sendResponse(response)
                }
            }
        }
    }
    
    private fun handleNewLog(log: String) {
        // Could implement real-time log streaming here
        // For now, just buffer it
    }
    
    @SuppressLint("NotificationPermission")
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, RemoteControlService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AniTrack Remote Control")
            .setContentText("Remote control service running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun updateNotification(connectionState: ConnectionState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val statusText = when (connectionState) {
            ConnectionState.CONNECTED -> "Connected to server"
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> "Connecting..."
            ConnectionState.DISCONNECTED -> "Disconnected"
            ConnectionState.ERROR -> "Connection error"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AniTrack Remote Control")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Remote Control Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for remote control service"
                setShowBadge(false)
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun cleanup() {
        serviceScope.launch {
            webSocketClient.disconnect()
            logcatCollector.stop()
            screenshotCapture.stopCapture()
            overlay?.hide()
        }
        
        serviceScope.cancel()
    }
}
