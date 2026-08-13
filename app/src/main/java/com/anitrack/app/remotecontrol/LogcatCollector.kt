package com.anitrack.app.remotecontrol

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue

class LogcatCollector(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    
    private val TAG = "LogcatCollector"
    
    private var isCollecting: Boolean = false
    private var collectJob: Job? = null
    
    // Buffer for collected logs
    private val logBuffer: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()
    
    // Maximum buffer size to prevent OOM
    private val maxBufferSize: Int = 1000
    
    // Filter for app-specific logs
    var packageName: String? = null
        set(value) {
            field = value
            if (isCollecting) {
                restartCollection()
            }
        }
    
    // Additional filter tags
    var filterTags: List<String> = emptyList()
    
    // Callback for new logs
    var onNewLog: ((String) -> Unit)? = null
    
    /**
     * Start collecting logcat output
     */
    fun start() {
        if (isCollecting) return
        
        isCollecting = true
        logBuffer.clear()
        
        collectJob = scope.launch {
            try {
                val processBuilder = ProcessBuilder().apply {
                    command(buildLogcatCommand())
                    redirectErrorStream(true)
                }
                
                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                
                Log.d(TAG, "Started logcat collection")
                
                reader.use { r ->
                    while (isActive && isCollecting) {
                        val line = r.readLine() ?: continue
                        
                        // Add to buffer
                        if (logBuffer.size >= maxBufferSize) {
                            logBuffer.poll()
                        }
                        logBuffer.offer(line)
                        
                        // Notify callback
                        onNewLog?.invoke(line)
                    }
                }
                
                process.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error in logcat collection", e)
            } finally {
                isCollecting = false
            }
        }
    }
    
    /**
     * Stop collecting logcat output
     */
    fun stop() {
        isCollecting = false
        collectJob?.cancel()
        collectJob = null
        Log.d(TAG, "Stopped logcat collection")
    }
    
    /**
     * Restart collection with new filters
     */
    fun restartCollection() {
        stop()
        start()
    }
    
    /**
     * Get all buffered logs
     */
    fun getLogs(): List<String> {
        return logBuffer.toList()
    }
    
    /**
     * Get last N lines of logs
     */
    fun getLastLogs(count: Int): List<String> {
        return logBuffer.toList().takeLast(count)
    }
    
    /**
     * Clear the log buffer
     */
    fun clearBuffer() {
        logBuffer.clear()
    }
    
    /**
     * Check if currently collecting
     */
    fun isActive(): Boolean = isCollecting
    
    /**
     * Build the logcat command based on current settings
     */
    private fun buildLogcatCommand(): List<String> {
        val command = mutableListOf("logcat", "-v", "time") // Use time format
        
        // Add package filter if specified
        packageName?.let { pkg ->
            command.add("--pid=${getProcessId(pkg)}")
        }
        
        // Add tag filters if specified
        if (filterTags.isNotEmpty()) {
            command.add("-s")
            command.addAll(filterTags)
        }
        
        return command
    }
    
    /**
     * Get process ID for a package name
     */
    private fun getProcessId(packageName: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "sh", "-c", "pidof $packageName"
            ))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val pid = reader.readLine() ?: ""
            process.waitFor()
            reader.close()
            pid.ifEmpty { android.os.Process.myPid().toString() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get PID for $packageName", e)
            android.os.Process.myPid().toString()
        }
    }
    
    /**
     * Get recent logs without starting continuous collection
     */
    suspend fun captureRecentLogs(maxLines: Int = 100): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val command = buildList {
                    add("logcat")
                    add("-d") // Dump mode - don't stream
                    add("-t")
                    add("$maxLines")
                    
                    packageName?.let { pkg ->
                        add("--pid=${getProcessId(pkg)}")
                    }
                }
                
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val logs = mutableListOf<String>()
                
                reader.use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        logs.add(line!!)
                    }
                }
                
                process.waitFor()
                logs
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture recent logs", e)
                emptyList()
            }
        }
    }
    
    /**
     * Search logs by keyword
     */
    fun searchLogs(keyword: String): List<String> {
        return logBuffer.filter { 
            it.contains(keyword, ignoreCase = true) 
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stop()
        scope.cancel()
        onNewLog = null
    }
}
