package com.pixhawk.gcs.transport

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*

/**
 * UDP transport implementation for MAVLink communication
 * Supports both listen mode (UDP server) and client mode (UDP client)
 */
class UdpTransport(
    private val scope: CoroutineScope,
    override val transportType: TransportType
) : Transport {
    
    private val _connectionState = MutableStateFlow(TransportState.DISCONNECTED)
    override val connectionState: StateFlow<TransportState> = _connectionState.asStateFlow()
    
    private var socket: DatagramSocket? = null
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0
    private var receiveJob: Job? = null
    private var dataCallback: ((ByteArray, Int) -> Unit)? = null
    
    private var localHost: String = ""
    private var localPort: Int = 0
    
    override suspend fun connect(params: ConnectionParams): Boolean {
        if (params !is ConnectionParams.NetworkParams) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = TransportState.CONNECTING
                
                // Close existing socket
                disconnect()
                
                localHost = params.host
                localPort = params.port
                
                socket = when (transportType) {
                    TransportType.UDP_LISTEN -> {
                        // Listen-only mode - bind to all interfaces
                        DatagramSocket(params.port)
                    }
                    TransportType.UDP_CLIENT -> {
                        // Connect to specific host
                        val addr = InetAddress.getByName(params.host)
                        remoteAddress = addr
                        remotePort = params.port
                        DatagramSocket().apply {
                            connect(addr, params.port)
                        }
                    }
                    else -> {
                        _connectionState.value = TransportState.ERROR
                        return@withContext false
                    }
                }
                
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
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            socket = null
            remoteAddress = null
        }
        
        _connectionState.value = TransportState.DISCONNECTED
    }
    
    override suspend fun sendData(data: ByteArray): Boolean {
        val currentSocket = socket
        val currentRemoteAddress = remoteAddress
        val currentRemotePort = remotePort
        
        if (currentSocket?.isClosed != false || 
            (transportType == TransportType.UDP_CLIENT && currentRemoteAddress == null)) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val packet = if (transportType == TransportType.UDP_CLIENT && currentRemoteAddress != null) {
                    DatagramPacket(data, data.size, currentRemoteAddress, currentRemotePort)
                } else if (transportType == TransportType.UDP_LISTEN && currentRemoteAddress != null) {
                    // In listen mode, reply to the last sender
                    DatagramPacket(data, data.size, currentRemoteAddress, currentRemotePort)
                } else {
                    return@withContext false
                }
                
                currentSocket.send(packet)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    override fun setDataCallback(callback: (ByteArray, Int) -> Unit) {
        dataCallback = callback
    }
    
    override fun getConnectionInfo(): String {
        return when (transportType) {
            TransportType.UDP_LISTEN -> "UDP Listen on port $localPort"
            TransportType.UDP_CLIENT -> "UDP Client to $localHost:$localPort"
            else -> "UDP Transport"
        }
    }
    
    private suspend fun receiveMessages() {
        val buffer = ByteArray(1024)
        
        while (!Thread.currentThread().isInterrupted && socket?.isClosed == false) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet)
                
                // If in listen mode and we haven't learned the sender yet, learn it
                if (transportType == TransportType.UDP_LISTEN && remoteAddress == null) {
                    remoteAddress = packet.address
                    remotePort = packet.port
                }
                
                // Notify callback of received data
                dataCallback?.invoke(buffer, packet.length)
                
            } catch (e: Exception) {
                if (socket?.isClosed != true && connectionState.value == TransportState.CONNECTED) {
                    e.printStackTrace()
                    _connectionState.value = TransportState.ERROR
                }
                break
            }
        }
    }
}