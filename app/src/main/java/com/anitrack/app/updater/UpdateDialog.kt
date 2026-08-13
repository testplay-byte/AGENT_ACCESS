package com.anitrack.app.updater

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.anitrack.app.R
import com.anitrack.app.ui.theme.*

/**
 * Update Dialog composable that displays available update information
 * and allows users to download and install the new version.
 *
 * @param release The GitHub release information
 * @param apkDownloadUrl URL to download the APK (null if no direct APK found)
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    release: GitHubRelease,
    apkDownloadUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var showInstallButton by remember { mutableStateOf(false) }
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }
    
    val appUpdater = remember { AppUpdater(context) }
    
    // Handle download completion callback
    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Completed) {
            showInstallButton = true
            downloadedFilePath = (downloadState as DownloadState.Completed).filePath
        }
    }
    
    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            appUpdater.unregisterDownloadReceiver()
        }
    }
    
    Dialog(
        onDismissRequest = { 
            if (downloadState !is DownloadState.Progress && 
                downloadState !is DownloadState.Preparing) {
                onDismiss() 
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = downloadState !is DownloadState.Progress,
            dismissOnClickOutside = downloadState !is DownloadState.Progress
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header with gradient background
                UpdateDialogHeader(
                    versionName = release.tag_name,
                    isDownloading = downloadState is DownloadState.Progress || 
                                   downloadState is DownloadState.Preparing
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Release notes section
                ReleaseNotesSection(
                    body = release.body,
                    releaseName = release.name,
                    publishDate = release.published_at
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Download progress or action buttons
                when (val state = downloadState) {
                    is DownloadState.Idle, is DownloadState.Failed -> {
                        ActionButtons(
                            canDownload = apkDownloadUrl != null,
                            isFailed = state is DownloadState.Failed,
                            errorMessage = (state as? DownloadState.Failed)?.error,
                            onDownload = {
                                if (apkDownloadUrl != null) {
                                    startDownload(
                                        context = context,
                                        updater = appUpdater,
                                        url = apkDownloadUrl,
                                        fileName = "AniTrack-${release.tag_name}.apk",
                                        onStateChanged = { newState ->
                                            downloadState = newState
                                        }
                                    )
                                }
                            },
                            onOpenReleasePage = {
                                openReleasePage(context, release.html_url)
                            },
                            onLater = onDismiss
                        )
                    }
                    is DownloadState.Preparing -> {
                        PreparingIndicator()
                    }
                    is DownloadState.Progress -> {
                        DownloadProgressIndicator(
                            bytesDownloaded = state.bytesDownloaded,
                            totalBytes = state.totalBytes
                        )
                    }
                    is DownloadState.Completed -> {
                        CompletedActions(
                            onInstall = {
                                downloadedFilePath?.let { path ->
                                    appUpdater.installApk(path)
                                }
                                onDismiss()
                            },
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header with gradient background showing version info.
 */
@Composable
private fun UpdateDialogHeader(
    versionName: String,
    isDownloading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(SkyBlue500, Aqua400)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isDownloading) 
                    androidx.compose.material.icons.Icons.Default.Download else 
                    androidx.compose.material.icons.Icons.Default.Update,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            
            Column {
                Text(
                    text = if (isDownloading) "Downloading Update" else "Update Available",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            // Animated badge for new version
            if (!isDownloading) {
                Box(
                    modifier = Modifier
                        .background(
                            color = CoralAccent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Section displaying release notes/changelog.
 */
@Composable
private fun ReleaseNotesSection(
    body: String?,
    releaseName: String?,
    publishDate: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = releaseName ?: "What's New",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                publishDate?.let { date ->
                    Text(
                        text = formatDate(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Release notes content
            if (!body.isNullOrBlank()) {
                Text(
                    text = formatReleaseNotes(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
                )
            } else {
                Text(
                    text = "No release notes available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Action buttons when no download in progress.
 */
@Composable
private fun ActionButtons(
    canDownload: Boolean,
    isFailed: Boolean,
    errorMessage: String?,
    onDownload: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onLater: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Error message if download failed
        AnimatedVisibility(visible = isFailed) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMessage ?: "Download failed. Please try again.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Primary action - Download or Open Page
        Button(
            onClick = if (canDownload) onDownload else onOpenReleasePage,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SkyBlue600,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            Icon(
                imageVector = if (canDownload) 
                    androidx.compose.material.icons.Icons.Default.Download else 
                    androidx.compose.material.icons.Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (canDownload) "Download Update" else "View on GitHub",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        // Secondary actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Open on GitHub button (if download available)
            if (canDownload) {
                OutlinedButton(
                    onClick = onOpenReleasePage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GitHub")
                }
            }
            
            // Later/Dismiss button
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.weight(if (canDownload) 1f else 1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Later")
            }
        }
    }
}

/**
 * Preparing download indicator.
 */
@Composable
private fun PreparingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SkyBlue50
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = SkyBlue600,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Preparing download...",
                style = MaterialTheme.typography.bodyMedium,
                color = SkyBlue700
            )
        }
    }
}

/**
 * Download progress indicator with percentage.
 */
@Composable
private fun DownloadProgressIndicator(
    bytesDownloaded: Long,
    totalBytes: Long
) {
    val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toFloat() / 100f else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SkyBlue50
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = SkyBlue600,
                trackColor = SkyBlue200
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = AppUpdater.formatFileSize(bytesDownloaded),
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyBlue700
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlue600
                )
                Text(
                    text = AppUpdater.formatFileSize(totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = SkyBlue700
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Downloading... Please wait",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Actions shown after successful download.
 */
@Composable
private fun CompletedActions(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StatusFinished.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                contentDescription = null,
                tint = StatusFinished,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Download Complete!",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StatusFinished
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusFinished,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Install")
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Start a download and track its progress.
 */
private fun startDownload(
    context: Context,
    updater: AppUpdater,
    url: String,
    fileName: String,
    onStateChanged: (DownloadState) -> Unit
) {
    // Set preparing state
    onStateChanged(DownloadState.Preparing)
    
    // Start download
    val downloadId = updater.startDownload(url, fileName)
    
    // Register completion callback
    updater.registerDownloadCompletionCallback { success, filePath ->
        if (success && filePath != null) {
            onStateChanged(DownloadState.Completed(filePath))
        } else {
            onStateChanged(DownloadState.Failed(filePath ?: "Unknown error"))
        }
    }
    
    // Poll for progress updates
    kotlinx.coroutines.GlobalScope.launch {
        while (true) {
            kotlinx.coroutines.delay(500) // Poll every 500ms
            val state = updater.getDownloadProgress()
            if (state is DownloadState.Completed || state is DownloadState.Failed) {
                break
            }
            if (state is DownloadState.Progress) {
                onStateChanged(state)
            }
        }
    }
}

/**
 * Format ISO date string to readable format.
 */
private fun formatDate(isoDate: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoDate)
        
        val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        outputFormat.format(date ?: return isoDate)
    } catch (e: Exception) {
        isoDate.take(10) // Fallback to just the date part
    }
}

/**
 * Format release notes from markdown-like syntax.
 */
private fun formatReleaseNotes(body: String): String {
    return body
        .replace(Regex("###\\s*"), "") // Remove h3 headers
        .replace(Regex("##\\s*"), "\n") // Convert h2 to newline
        .replace(Regex("#\\s*"), "\n") // Convert h1 to newline
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold markers
        .replace(Regex("\\*(.*?)\\*"), "$1") // Remove italic markers
        .replace(Regex("-\\s*"), "• ") // Convert list items
        .replace(Regex("`([^`]*)`"), "$1") // Remove code markers
        .trim()
}

/**
 * Open release page in browser.
 */
private fun openReleasePage(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Browser not available
    }
}
