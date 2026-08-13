package com.anitrack.app.remotecontrol

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.anitrack.app.remotecontrol.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommandExecutor(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    
    private val TAG = "CommandExecutor"
    
    // Handler for delayed actions
    private val handler = Handler(Looper.getMainLooper())
    
    // Accessibility service reference (if available)
    var accessibilityService: AccessibilityService? = null
    
    /**
     * Execute a command received from the server
     */
    fun executeCommand(command: CommandMessage): ResponseMessage {
        return try {
            Log.d(TAG, "Executing command: ${command.type}")
            
            when (command.type) {
                "tap" -> executeTap(command.payload.tap)
                "swipe" -> executeSwipe(command.payload.swipe)
                "screenshot" -> handleScreenshotRequest(command.payload.screenshot)
                "logcat" -> handleLogcatCommand(command.payload.logcat)
                "navigation" -> executeNavigation(command.payload.navigation)
                "text" -> executeTextInput(command.payload.text)
                "get_ui_hierarchy" -> getUiHierarchy()
                else -> ResponseMessage(
                    type = "error",
                    requestId = command.id,
                    success = false,
                    error = "Unknown command type: ${command.type}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command", e)
            ResponseMessage(
                type = "error",
                requestId = command.id,
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Execute tap/click at coordinates
     */
    private fun executeTap(tap: TapCommand?): ResponseMessage {
        if (tap == null) {
            return createErrorResponse("tap", "Missing tap parameters")
        }
        
        return try {
            simulateTap(tap.x, tap.y, tap.duration)
            createSuccessResponse("tap", AckResponse("tap"))
        } catch (e: Exception) {
            createErrorResponse("tap", e.message ?: "Tap failed")
        }
    }
    
    /**
     * Simulate touch/tap using InputManager or AccessibilityService
     */
    fun simulateTap(x: Float, y: Float, duration: Long = 100L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Use accessibility service for injection
            accessibilityService?.let { service ->
                val path = android.graphics.Path().apply {
                    moveTo(x, y)
                    lineTo(x, y)
                }
                
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration
                    ))
                
                service.dispatchGesture(gestureBuilder.build(), null, null)
                return
            }
        }
        
        // Fallback: Use shell command (requires root)
        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "sh", "-c", "input tap $x $y"
            ))
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute tap via shell", e)
        }
    }
    
    /**
     * Execute swipe gesture
     */
    private fun executeSwipe(swipe: SwipeCommand?): ResponseMessage {
        if (swipe == null) {
            return createErrorResponse("swipe", "Missing swipe parameters")
        }
        
        return try {
            simulateSwipe(
                swipe.startX, swipe.startY,
                swipe.endX, swipe.endY,
                swipe.duration
            )
            createSuccessResponse("swipe", AckResponse("swipe"))
        } catch (e: Exception) {
            createErrorResponse("swipe", e.message ?: "Swipe failed")
        }
    }
    
    /**
     * Simulate swipe gesture
     */
    fun simulateSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            accessibilityService?.let { service ->
                val path = android.graphics.Path().apply {
                    moveTo(startX, startY)
                    lineTo(endX, endY)
                }
                
                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(
                        path,
                        0,
                        duration
                    ))
                
                service.dispatchGesture(gestureBuilder.build(), null, null)
                return
            }
        }
        
        // Fallback: Use shell command
        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "sh", "-c", "input swipe $startX $startY $endX $endY $duration"
            ))
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute swipe via shell", e)
        }
    }
    
    /**
     * Handle screenshot request - returns acknowledgment, actual capture is async
     */
    private fun handleScreenshotRequest(request: ScreenshotRequest?): ResponseMessage {
        if (request == null) {
            return createErrorResponse("screenshot", "Missing screenshot parameters")
        }
        
        // The actual screenshot will be captured asynchronously and sent as response
        return createSuccessResponse("screenshot", AckResponse("screenshot"))
    }
    
    /**
     * Handle logcat commands
     */
    private fun handleLogcatCommand(command: LogcatCommand?): ResponseMessage {
        if (command == null) {
            return createErrorResponse("logcat", "Missing logcat parameters")
        }
        
        return when (command.action.lowercase()) {
            "start" -> {
                // Start logcat collection
                createSuccessResponse("logcat", AckResponse("logcat_start"))
            }
            "stop" -> {
                // Stop logcat collection
                createSuccessResponse("logcat", AckResponse("logcat_stop"))
            }
            else -> createErrorResponse("logcat", "Unknown action: ${command.action}")
        }
    }
    
    /**
     * Execute navigation commands (back, home, recent apps)
     */
    private fun executeNavigation(navigation: NavigationCommand?): ResponseMessage {
        if (navigation == null) {
            return createErrorResponse("navigation", "Missing navigation parameters")
        }
        
        return try {
            when (navigation.action.lowercase()) {
                "back" -> performBackAction()
                "home" -> performHomeAction()
                "recent" -> performRecentAppsAction()
                else -> createErrorResponse("navigation", "Unknown action: ${navigation.action}")
            }
        } catch (e: Exception) {
            createErrorResponse("navigation", e.message ?: "Navigation failed")
        }
    }
    
    private fun performBackAction(): ResponseMessage {
        // Try accessibility service first
        accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            ?: run {
                // Fallback to key event
                val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
                handler.postDelayed({
                    context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                }, 100)
            }
        return createSuccessResponse("navigation", AckResponse("back"))
    }
    
    private fun performHomeAction(): ResponseMessage {
        accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            ?: run {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        return createSuccessResponse("navigation", AckResponse("home"))
    }
    
    private fun performRecentAppsAction(): ResponseMessage {
        accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        return createSuccessResponse("navigation", AckResponse("recent"))
    }
    
    /**
     * Execute text input
     */
    private fun executeTextInput(textCommand: TextCommand?): ResponseMessage {
        if (textCommand == null) {
            return createErrorResponse("text", "Missing text parameters")
        }
        
        return try {
            when (textCommand.action.lowercase()) {
                "input" -> inputText(textCommand.text)
                "paste" -> pasteText(textCommand.text)
                else -> createErrorResponse("text", "Unknown action: ${textCommand.action}")
            }
        } catch (e: Exception) {
            createErrorResponse("text", e.message ?: "Text input failed")
        }
    }
    
    private fun inputText(text: String): ResponseMessage {
        try {
            val escapedText = text.replace(" ", "%s").replace("'", "\\'")
            val process = Runtime.getRuntime().exec(arrayOf(
                "sh", "-c", "input text '$escapedText'"
            ))
            process.waitFor()
            return createSuccessResponse("text", AckResponse("text_input"))
        } catch (e: Exception) {
            // Fallback to clipboard method
            return pasteText(text)
        }
    }
    
    private fun pasteText(text: String): ResponseMessage {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("remote_control", text)
        clipboard.setPrimaryClip(clip)
        
        // Simulate paste (Ctrl+V or long press + paste)
        handler.postDelayed({
            try {
                val process = Runtime.getRuntime().exec(arrayOf(
                    "sh", "-c", "input keyevent KEYCODE_PASTE"
                ))
                process.waitFor()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to paste", e)
            }
        }, 100)
        
        return createSuccessResponse("text", AckResponse("text_paste"))
    }
    
    /**
     * Get UI hierarchy of current screen
     */
    private fun getUiHierarchy(): ResponseMessage {
        return try {
            val xml = accessibilityService?.rootInActiveWindow?.let { root ->
                buildXmlFromNode(root, 0)
            } ?: "<root>No accessibility service available</root>"
            
            createSuccessResponse("ui_hierarchy", UiHierarchy(xml))
        } catch (e: Exception) {
            createErrorResponse("ui_hierarchy", e.message ?: "Failed to get UI hierarchy")
        }
    }
    
    private fun buildXmlFromNode(node: AccessibilityNodeInfo?, depth: Int): String {
        if (node == null) return ""
        
        val indent = "  ".repeat(depth)
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val bounds = node.boundsInScreen
        
        val xml = StringBuilder()
        xml.appendLine("$indent<node class=\"$className\" text=\"$text\" desc=\"$contentDesc\" bounds=\"${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}\">")
        
        for (i in 0 until node.childCount) {
            xml.append(buildXmlFromNode(node.getChild(i), depth + 1))
        }
        
        xml.appendLine("$indent</node>")
        
        return xml.toString()
    }
    
    // Helper methods for creating responses
    private fun createSuccessResponse(type: String, payload: Any? = null): ResponseMessage {
        return ResponseMessage(
            type = "${type}_response",
            success = true,
            payload = payload as? com.anitrack.app.remotecontrol.models.ResponsePayload
        )
    }
    
    private fun createErrorResponse(type: String, errorMessage: String): ResponseMessage {
        return ResponseMessage(
            type = "${type}_response",
            success = false,
            error = errorMessage
        )
    }
}
