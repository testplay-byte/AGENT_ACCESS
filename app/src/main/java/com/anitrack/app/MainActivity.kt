package com.anitrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.anitrack.app.ui.navigation.AniTrackNavHost
import com.anitrack.app.ui.theme.AniTrackTheme
import com.anitrack.app.updater.AppUpdater
import com.anitrack.app.updater.UpdateCheckResult
import com.anitrack.app.updater.UpdateDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    // State for update dialog
    private var showUpdateDialog by mutableStateOf(false)
    private var updateResult by mutableStateOf<UpdateCheckResult.UpdateAvailable?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check for updates after a short delay to not impact launch performance
        checkForUpdates()
        
        setContent {
            AniTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AniTrackNavHost()
                        
                        // Show update dialog if available
                        if (showUpdateDialog && updateResult != null) {
                            UpdateDialog(
                                release = updateResult!!.release,
                                apkDownloadUrl = updateResult!!.apkDownloadUrl,
                                onDismiss = { 
                                    showUpdateDialog = false 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check for available updates from GitHub Releases.
     * This is called on startup with a delay to avoid affecting app launch performance.
     */
    private fun checkForUpdates() {
        lifecycleScope.launch {
            // Delay to allow the app to fully load before checking for updates
            delay(3000) // 3 seconds delay
            
            try {
                val updater = AppUpdater(this@MainActivity)
                val result = updater.checkForUpdate()
                
                when (result) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        updateResult = result
                        showUpdateDialog = true
                    }
                    is UpdateCheckResult.NoUpdate -> {
                        // No update available, do nothing
                    }
                    is UpdateCheckResult.Error -> {
                        // Log error but don't show to user (non-intrusive)
                        android.util.Log.w("AppUpdater", "Update check failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                // Silently fail - update check should never crash the app
                android.util.Log.e("AppUpdater", "Update check exception", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up any pending downloads/receivers
        // The AppUpdater handles its own cleanup via DisposableEffect in the dialog
    }
}
