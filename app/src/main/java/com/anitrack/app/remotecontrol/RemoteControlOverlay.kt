package com.anitrack.app.remotecontrol

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.anitrack.app.R
import com.anitrack.app.remotecontrol.models.ConnectionState

class RemoteControlOverlay(
    private val context: Context,
    private val onToggleVisibility: () -> Unit = {},
    private val onDismiss: () -> Unit = {}
) {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isExpanded: Boolean = false
    private var isVisible: Boolean = true
    
    // Touch handling for dragging
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    
    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (overlayView != null) return
        
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
        
        overlayView = createOverlayView()
        
        // Set up touch listener for dragging
        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)
                    
                    // If it was a tap (not a drag), handle click
                    if (deltaX < 10 && deltaY < 10) {
                        handleClick(event)
                    }
                    true
                }
                else -> false
            }
        }
        
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun hide() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }
    
    fun updateConnectionState(state: ConnectionState) {
        overlayView?.let { view ->
            val statusIndicator = view.findViewById<ImageView>(R.id.status_indicator)
            val statusText = view.findViewById<TextView>(R.id.status_text)
            
            when (state) {
                ConnectionState.CONNECTED -> {
                    statusIndicator?.setImageResource(R.drawable.ic_status_connected)
                    statusIndicator?.setColorFilter(ContextCompat.getColor(context, R.color.remote_control_connected))
                    statusText?.text = context.getString(R.string.remote_control_connected)
                    statusText?.setTextColor(ContextCompat.getColor(context, R.color.remote_control_connected))
                }
                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
                    statusIndicator?.setImageResource(R.drawable.ic_status_connecting)
                    statusIndicator?.setColorFilter(ContextCompat.getColor(context, R.color.remote_control_connecting))
                    statusText?.text = context.getString(R.string.remote_control_connecting)
                    statusText?.setTextColor(ContextCompat.getColor(context, R.color.remote_control_connecting))
                }
                else -> {
                    statusIndicator?.setImageResource(R.drawable.ic_status_disconnected)
                    statusIndicator?.setColorFilter(ContextCompat.getColor(context, R.color.remote_control_disconnected))
                    statusText?.text = context.getString(R.string.remote_control_disconnected)
                    statusText?.setTextColor(ContextCompat.getColor(context, R.color.remote_control_disconnected))
                }
            }
        }
    }
    
    fun toggleExpand() {
        isExpanded = !isExpanded
        overlayView?.let { view ->
            val expandPanel = view.findViewById<LinearLayout>(R.id.expand_panel)
            expandPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
    }
    
    fun toggleVisibility() {
        isVisible = !isVisible
        if (isVisible) {
            show()
        } else {
            hide()
        }
        onToggleVisibility()
    }
    
    private fun createOverlayView(): View {
        return LayoutInflater.from(context).inflate(R.layout.overlay_remote_control, null).apply {
            // Initialize views
            findViewById<View>(R.id.fab_main)?.setOnClickListener {
                toggleExpand()
            }
            
            findViewById<View>(R.id.btn_close)?.setOnClickListener {
                hide()
                onDismiss()
            }
            
            findViewById<View>(R.id.btn_screenshot)?.setOnClickListener {
                // Trigger screenshot
            }
            
            // Initially hide expanded panel
            findViewById<LinearLayout>(R.id.expand_panel)?.visibility = View.GONE
        }
    }
    
    private fun handleClick(event: MotionEvent) {
        // Detect long press vs short press
        val clickTime = System.currentTimeMillis()
        
        // For now, just toggle expand on click
        toggleExpand()
    }
    
    companion object {
        const val TAG = "RemoteControlOverlay"
    }
}
