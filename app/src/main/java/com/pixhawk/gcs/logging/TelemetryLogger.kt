package com.pixhawk.gcs.logging

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * TelemetryLogger for recording timestamped MAVLink frames to app-specific storage
 * Creates rolling log files with session-based naming
 */
class TelemetryLogger(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()
    
    private val _currentLogFile = MutableStateFlow<String?>(null)
    val currentLogFile: StateFlow<String?> = _currentLogFile.asStateFlow()
    
    private var logFile: File? = null
    private var logOutputStream: FileOutputStream? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    /**
     * Start logging MAVLink frames
     * @return true if logging started successfully, false otherwise
     */
    suspend fun startLogging(): Boolean {
        if (_isLogging.value) {
            return true // Already logging
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // Create logs directory if it doesn't exist
                val logsDir = File(context.filesDir, "logs")
                if (!logsDir.exists()) {
                    logsDir.mkdirs()
                }
                
                // Create new log file with timestamp
                val timestamp = dateFormat.format(Date())
                val fileName = "mav_${timestamp}.log"
                logFile = File(logsDir, fileName)
                
                // Open output stream
                logOutputStream = FileOutputStream(logFile)
                
                // Write log header
                val header = "# MAVLink Telemetry Log\n" +
                            "# Started: ${timestampFormat.format(Date())}\n" +
                            "# Format: [timestamp] [HEX] raw_data\n\n"
                logOutputStream?.write(header.toByteArray())
                logOutputStream?.flush()
                
                _isLogging.value = true
                _currentLogFile.value = fileName
                
                // Clean up old log files (keep last 10 files)
                cleanupOldLogs(logsDir)
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                _isLogging.value = false
                _currentLogFile.value = null
                false
            }
        }
    }
    
    /**
     * Stop logging MAVLink frames
     */
    suspend fun stopLogging() {
        if (!_isLogging.value) {
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                // Write log footer
                val footer = "\n# Ended: ${timestampFormat.format(Date())}\n"
                logOutputStream?.write(footer.toByteArray())
                logOutputStream?.flush()
                
                logOutputStream?.close()
                logOutputStream = null
                logFile = null
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            _isLogging.value = false
            _currentLogFile.value = null
        }
    }
    
    /**
     * Log MAVLink frame data
     * @param data Raw MAVLink frame data
     * @param length Length of valid data
     */
    fun logFrame(data: ByteArray, length: Int) {
        if (!_isLogging.value || logOutputStream == null) {
            return
        }
        
        scope.launch(Dispatchers.IO) {
            try {
                val timestamp = timestampFormat.format(Date())
                val hexData = data.take(length).joinToString(" ") { 
                    "%02X".format(it.toInt() and 0xFF) 
                }
                
                val logLine = "[$timestamp] [HEX] $hexData\n"
                logOutputStream?.write(logLine.toByteArray())
                logOutputStream?.flush()
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Stop logging on error
                _isLogging.value = false
                _currentLogFile.value = null
            }
        }
    }
    
    /**
     * Log text message (for debugging or annotations)
     * @param message Text message to log
     */
    fun logMessage(message: String) {
        if (!_isLogging.value || logOutputStream == null) {
            return
        }
        
        scope.launch(Dispatchers.IO) {
            try {
                val timestamp = timestampFormat.format(Date())
                val logLine = "[$timestamp] [MSG] $message\n"
                logOutputStream?.write(logLine.toByteArray())
                logOutputStream?.flush()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get list of existing log files
     */
    fun getLogFiles(): List<LogFileInfo> {
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists()) {
            return emptyList()
        }
        
        return try {
            logsDir.listFiles { file ->
                file.name.startsWith("mav_") && file.name.endsWith(".log")
            }?.map { file ->
                LogFileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = Date(file.lastModified())
                )
            }?.sortedByDescending { it.lastModified } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Delete all log files
     */
    suspend fun clearAllLogs(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Stop logging first if active
                if (_isLogging.value) {
                    stopLogging()
                }
                
                val logsDir = File(context.filesDir, "logs")
                if (logsDir.exists()) {
                    logsDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("mav_") && file.name.endsWith(".log")) {
                            file.delete()
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    /**
     * Clean up old log files, keeping only the most recent ones
     */
    private fun cleanupOldLogs(logsDir: File, keepCount: Int = 10) {
        try {
            val logFiles = logsDir.listFiles { file ->
                file.name.startsWith("mav_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }
            
            if (logFiles != null && logFiles.size > keepCount) {
                // Delete oldest files
                for (i in keepCount until logFiles.size) {
                    logFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Log file information
 */
data class LogFileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Date
)