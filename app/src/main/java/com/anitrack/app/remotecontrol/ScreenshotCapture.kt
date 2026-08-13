package com.anitrack.app.remotecontrol

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenshotCapture(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    
    private val TAG = "ScreenshotCapture"
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private var mediaProjectionManager: MediaProjectionManager? = null
    
    private var resultCode: Int = 0
    private var resultData: Intent? = null
    
    private var isCapturing: Boolean = false
    
    companion object {
        const val SCREENSHOT_REQUEST_CODE = 1001
        const val DEFAULT_QUALITY = 80
        const val DEFAULT_FORMAT = Bitmap.CompressFormat.JPEG
    }

    init {
        mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    
    fun requestPermission(activity: Activity) {
        mediaProjectionManager?.createScreenCaptureIntent()?.let { intent ->
            activity.startActivityForResult(intent, SCREENSHOT_REQUEST_CODE)
        }
    }
    
    fun setPermissionResult(resultCode: Int, data: Intent?) {
        this.resultCode = resultCode
        this.resultData = data
    }
    
    suspend fun captureScreenshot(
        quality: Int = DEFAULT_QUALITY,
        format: Bitmap.CompressFormat = DEFAULT_FORMAT
    ): ScreenshotResult {
        
        return withContext(Dispatchers.IO) {
            if (mediaProjection == null) {
                setupMediaProjection()
            }
            
            try {
                val bitmap = captureScreen()
                
                if (bitmap != null) {
                    // Compress to byte array
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(format, quality, outputStream)
                    val bytes = outputStream.toByteArray()
                    
                    // Encode to base64
                    val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    
                    ScreenshotResult.Success(
                        image = base64Image,
                        width = bitmap.width,
                        height = bitmap.height,
                        format = when (format) {
                            Bitmap.CompressFormat.PNG -> "png"
                            Bitmap.CompressFormat.WEBP -> "webp"
                            else -> "jpeg"
                        },
                        sizeBytes = bytes.size
                    )
                } else {
                    ScreenshotResult.Error("Failed to capture screen")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing screenshot", e)
                ScreenshotResult.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun setupMediaProjection() {
        if (resultCode == 0 || resultData == null) {
            throw IllegalStateException("Permission not granted for screen capture")
        }
        
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics.bounds
            val width = metrics.width()
            val height = metrics.height()
            val density = context.resources.configuration.densityDpi
            
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData!!)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AniTrack-ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            
            imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
            
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData!!)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "AniTrack-ScreenCapture",
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
        }
    }
    
    private fun captureScreen(): Bitmap? {
        val reader = imageReader ?: return null
        
        try {
            val image = reader.acquireLatestImage() ?: return null
            
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer as ByteBuffer?)
            image.close()
            
            return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring image", e)
            return null
        }
    }
    
    fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        virtualDisplay = null
        mediaProjection?.stop()
        mediaProjection = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader = null
    }
    
    fun cleanup() {
        stopCapture()
        scope.cancel()
    }
    
    sealed class ScreenshotResult {
        data class Success(
            val image: String, // Base64 encoded
            val width: Int,
            val height: Int,
            val format: String,
            val sizeBytes: Int
        ) : ScreenshotResult()
        
        data class Error(val message: String) : ScreenshotResult()
    }
}
