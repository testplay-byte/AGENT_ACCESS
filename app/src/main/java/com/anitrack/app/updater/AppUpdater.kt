package com.anitrack.app.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.anitrack.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Configuration for the AppUpdater.
 * Update GITHUB_REPO to match your actual GitHub repository.
 */
object UpdaterConfig {
    // AniTrack GitHub Repository
    const val GITHUB_REPO = "testplay-byte/AGENT_ACCESS"
    
    // GitHub API endpoints
    const val GITHUB_API_BASE = "https://api.github.com"
    const val LATEST_RELEASE_ENDPOINT = "$GITHUB_API_BASE/repos/$GITHUB_REPO/releases/latest"
    
    // Download notification channel ID
    const val DOWNLOAD_CHANNEL_ID = "app_update_download"
    
    // FileProvider authority - must match AndroidManifest.xml
    // Note: APPLICATION_ID will be set at runtime via context
    const val FILE_PROVIDER_AUTHORITY_TEMPLATE = "%s.fileprovider"
}

/**
 * Data class representing a GitHub Release.
 */
@Serializable
data class GitHubRelease(
    val id: Long,
    val tag_name: String,
    val name: String?,
    val body: String?,
    val html_url: String,
    val assets: List<GitHubAsset> = emptyList(),
    val prerelease: Boolean = false,
    val published_at: String?
)

@Serializable
data class GitHubAsset(
    val id: Long,
    val name: String,
    val browser_download_url: String,
    val size: Long,
    val content_type: String
)

/**
 * Result of checking for updates.
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val release: GitHubRelease,
        val apkDownloadUrl: String?,
        val apkFileName: String?
    ) : UpdateCheckResult()
    
    object NoUpdate : UpdateCheckResult()
    
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Download state for tracking progress.
 */
sealed class DownloadState {
    object Idle : DownloadState()
    object Preparing : DownloadState()
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadState() {
        fun getProgressPercent(): Int {
            return if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
        }
        
        fun getProgressFormatted(): String {
            val percent = getProgressPercent()
            val downloaded = AppUpdater.formatFileSize(bytesDownloaded)
            val total = AppUpdater.formatFileSize(totalBytes)
            return "$downloaded / $total ($percent%)"
        }
    }
    
    data class Completed(val filePath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

/**
 * Main utility class for checking and downloading app updates from GitHub Releases.
 */
class AppUpdater(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var downloadId: Long? = null
    private var downloadReceiver: BroadcastReceiver? = null
    
    /**
     * Check for available updates by querying the GitHub API.
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UpdaterConfig.LATEST_RELEASE_ENDPOINT)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                when (response.code) {
                    404 -> return@withContext UpdateCheckResult.Error("Repository or releases not found. Please check GITHUB_REPO configuration.")
                    403 -> return@withContext UpdateCheckResult.Error("API rate limit exceeded. Try again later.")
                    else -> return@withContext UpdateCheckResult.Error("Failed to check for updates (HTTP ${response.code})")
                }
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext UpdateCheckResult.Error("Empty response from server")
            
            val release = json.decodeFromString<GitHubRelease>(responseBody)
            
            // Skip pre-releases unless configured otherwise
            if (release.prerelease) {
                return@withContext UpdateCheckResult.NoUpdate
            }
            
            // Compare versions
            val currentVersion = getCurrentVersion(context)
            val latestVersion = release.tag_name.removePrefix("v").removePrefix("V")
            
            if (!isNewerVersion(currentVersion, latestVersion)) {
                return@withContext UpdateCheckResult.NoUpdate
            }
            
            // Find APK asset
            val apkAsset = findApkAsset(release.assets)
            
            UpdateCheckResult.UpdateAvailable(
                release = release,
                apkDownloadUrl = apkAsset?.browser_download_url,
                apkFileName = apkAsset?.name
            )
            
        } catch (e: Exception) {
            UpdateCheckResult.Error("Network error: ${e.message}")
        }
    }
    
    /**
     * Find APK file from release assets.
     */
    private fun findApkAsset(assets: List<GitHubAsset>): GitHubAsset? {
        // Look for common APK naming patterns
        val apkPatterns = listOf(
            ".apk",
            "-release.apk",
            "-signed.apk",
            "anitrack",
            "AniTrack"
        )
        
        return assets.firstOrNull { asset ->
            apkPatterns.any { pattern -> 
                asset.name.contains(pattern, ignoreCase = true) 
            } && asset.content_type in listOf(
                "application/vnd.android.package-archive",
                "application/octet-stream",
                "application/apk"
            )
        } ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    }
    
    /**
     * Start downloading the update APK using DownloadManager.
     * Returns a suspend function that tracks progress and completion.
     */
    fun startDownload(apkUrl: String, fileName: String): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle(context.getString(R.string.update_downloading))
            setDescription(fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setMimeType("application/vnd.android.package-archive")
            
            // Show in downloads UI
            setVisibleInDownloadsUi(true)
            
            // Require network
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        }
        
        val id = downloadManager.enqueue(request)
        this.downloadId = id
        
        return id
    }
    
    /**
     * Register a broadcast receiver to listen for download completion.
     */
    fun registerDownloadCompletionCallback(onComplete: (Boolean, String?) -> Unit) {
        unregisterDownloadReceiver()
        
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val receivedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                
                if (receivedId == downloadId) {
                    val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    
                    if (downloadManager != null) {
                        val query = DownloadManager.Query().setFilterById(receivedId)
                        val cursor = downloadManager.query(query)
                        
                        if (cursor.moveToFirst()) {
                            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                            
                            val status = cursor.getInt(statusIndex)
                            val localUri = cursor.getString(uriIndex)
                            val title = cursor.getString(titleIndex)
                            
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    onComplete(true, localUri)
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    onComplete(false, "Download failed")
                                }
                                else -> {
                                    // Still in progress or other state
                                }
                            }
                        }
                        cursor.close()
                    }
                    
                    // Unregister after receiving result
                    unregisterDownloadReceiver()
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(downloadReceiver, filter)
        }
    }
    
    /**
     * Get current download progress.
     */
    fun getDownloadProgress(): DownloadState {
        val id = downloadId ?: return DownloadState.Idle
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query)
        
        if (!cursor.moveToFirst()) {
            cursor.close()
            return DownloadState.Idle
        }
        
        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
        
        val status = cursor.getInt(statusIndex)
        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
        val totalBytes = cursor.getLong(totalBytesIndex)
        val localUri = cursor.getString(localUriIndex)
        
        cursor.close()
        
        return when (status) {
            DownloadManager.STATUS_PENDING,
            DownloadManager.STATUS_RUNNING,
            DownloadManager.STATUS_PAUSED -> DownloadState.Progress(bytesDownloaded, totalBytes)
            DownloadManager.STATUS_SUCCESSFUL -> DownloadState.Completed(localUri ?: "")
            DownloadManager.STATUS_FAILED -> DownloadState.Failed("Download failed")
            else -> DownloadState.Idle
        }
    }
    
    /**
     * Cancel the current download.
     */
    fun cancelDownload() {
        downloadId?.let { id ->
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(id)
        }
        downloadId = null
        unregisterDownloadReceiver()
    }
    
    /**
     * Open/install the downloaded APK file.
     */
    fun installApk(filePath: String): Boolean {
        return try {
            val fileUri = Uri.parse(filePath)
            val path = fileUri?.path
            
            if (path.isNullOrEmpty()) {
                Log.w(TAG, "Invalid APK file path")
                return false
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                // For Android 7+, we need FileProvider
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val apkFile = java.io.File(path)
                    val authority = UpdaterConfig.FILE_PROVIDER_AUTHORITY_TEMPLATE.format(context.packageName)
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        authority,
                        apkFile
                    )
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get current app version name from package manager.
     */
    private fun getCurrentVersion(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Unregister the download receiver to prevent memory leaks.
     */
    fun unregisterDownloadReceiver() {
        try {
            downloadReceiver?.let {
                context.unregisterReceiver(it)
            }
        } catch (e: IllegalArgumentException) {
            // Receiver wasn't registered
        }
        downloadReceiver = null
    }
    
    companion object {
        /**
         * Compare version strings to determine if latest is newer than current.
         * Supports semantic versioning (1.2.3) and simple versions (1.2).
         */
        fun isNewerVersion(current: String, latest: String): Boolean {
            val currentParts = parseVersion(current)
            val latestParts = parseVersion(latest)
            
            val maxLength = maxOf(currentParts.size, latestParts.size)
            
            for (i in 0 until maxLength) {
                val currentPart = currentParts.getOrElse(i) { 0 }
                val latestPart = latestParts.getOrElse(i) { 0 }
                
                if (latestPart > currentPart) return true
                if (latestPart < currentPart) return false
            }
            
            return false // Versions are equal
        }
        
        /**
         * Parse version string into list of integers.
         */
        private fun parseVersion(version: String): List<Int> {
            // Remove 'v' prefix and extract numeric parts
            return version
                .removePrefix("v")
                .removePrefix("V")
                .split(Regex("[.\\-]"))
                .mapNotNull { part ->
                    part.toIntOrNull()
                }
        }
        
        /**
         * Format file size for display.
         */
        fun formatFileSize(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024))
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
