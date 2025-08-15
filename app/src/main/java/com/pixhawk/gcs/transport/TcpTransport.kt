package com.pixhawk.gcs.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * TCP transport implementation for MAVLink communication
 * Supports TCP client connections to remote MAVLink hosts
 */
class TcpTransport(
    private val scope: CoroutineScope
) : Transport {
    
    override val transportType: TransportType = TransportType.TCP_CLIENT
    
    private val _connectionState = MutableStateFlow(TransportState.DISCONNECTED)
    override val connectionState: StateFlow<TransportState> = _connectionState.asStateFlow()
    
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var receiveJob: Job? = null
    private var dataCallback: ((ByteArray, Int) -> Unit)? = null
    
    private var remoteHost: String = ""
    private var remotePort: Int = 0
    
    override suspend fun connect(params: ConnectionParams): Boolean {
        if (params !is ConnectionParams.NetworkParams) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = TransportState.CONNECTING
                
                // Close existing socket
                disconnect()
                
                remoteHost = params.host
                remotePort = params.port
                
                // Create TCP socket connection
                val newSocket = Socket(params.host, params.port)
                socket = newSocket
                inputStream = newSocket.getInputStream()
                outputStream = newSocket.getOutputStream()
                
                _connectionState.value = TransportState.CONNECTED
                
                // Start receiving messages
                receiveJob = scope.launch {
                    receiveMessages()
                }
                
                true
            } catch (e: Exception) {
                _connectionState.value = TransportState.ERROR
                e.printStackTrace()
                false
            }
        }
    }
    
    override suspend fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        
        withContext(Dispatchers.IO) {
            try {
                inputStream?.close()
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            inputStream = null
            outputStream = null
            socket = null
        }
        
        _connectionState.value = TransportState.DISCONNECTED
    }
    
    override suspend fun sendData(data: ByteArray): Boolean {
        val currentOutputStream = outputStream
        
        if (currentOutputStream == null || socket?.isConnected != true) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                currentOutputStream.write(data)
                currentOutputStream.flush()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = TransportState.ERROR
                false
            }
        }
    }
    
    override fun setDataCallback(callback: (ByteArray, Int) -> Unit) {
        dataCallback = callback
    }
    
    override fun getConnectionInfo(): String {
        return "TCP Client to $remoteHost:$remotePort"
    }
    
    private suspend fun receiveMessages() {
        val buffer = ByteArray(1024)
        val currentInputStream = inputStream
        
        if (currentInputStream == null) return
        
        while (!Thread.currentThread().isInterrupted && 
               socket?.isConnected == true && 
               connectionState.value == TransportState.CONNECTED) {
            try {
                val bytesRead = currentInputStream.read(buffer)
                if (bytesRead > 0) {
                    // Notify callback of received data
                    dataCallback?.invoke(buffer, bytesRead)
                } else if (bytesRead == -1) {
                    // End of stream - connection closed
                    break
                }
            } catch (e: Exception) {
                if (connectionState.value == TransportState.CONNECTED) {
                    e.printStackTrace()
                    _connectionState.value = TransportState.ERROR
                }
                break
            }
        }
        
        // Connection ended
        if (connectionState.value == TransportState.CONNECTED) {
            _connectionState.value = TransportState.DISCONNECTED
        }
    }
}