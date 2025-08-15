package com.pixhawk.gcs.transport

import kotlinx.coroutines.flow.StateFlow

/**
 * Transport types supported by the Ground Control Station
 */
enum class TransportType {
    UDP_LISTEN,     // UDP server listening for incoming connections
    UDP_CLIENT,     // UDP client connecting to remote host
    TCP_CLIENT,     // TCP client connecting to remote host
    BLUETOOTH_SPP   // Bluetooth Serial Port Profile
}

/**
 * Transport connection state
 */
enum class TransportState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Transport interface for MAVLink communication
 * All transport implementations must implement this interface
 */
interface Transport {
    
    /**
     * Current transport type
     */
    val transportType: TransportType
    
    /**
     * Connection state as StateFlow for UI observation
     */
    val connectionState: StateFlow<TransportState>
    
    /**
     * Connect to the transport with given parameters
     * @param params Connection parameters (varies by transport type)
     * @return true if connection initiation was successful, false otherwise
     */
    suspend fun connect(params: ConnectionParams): Boolean
    
    /**
     * Disconnect from the transport
     */
    suspend fun disconnect()
    
    /**
     * Send data over the transport
     * @param data Byte array to send
     * @return true if data was sent successfully, false otherwise
     */
    suspend fun sendData(data: ByteArray): Boolean
    
    /**
     * Set callback for received data
     * @param callback Function to call when data is received
     */
    fun setDataCallback(callback: (ByteArray, Int) -> Unit)
    
    /**
     * Get transport-specific information for display
     */
    fun getConnectionInfo(): String
}

/**
 * Base class for connection parameters
 */
sealed class ConnectionParams {
    
    /**
     * Network connection parameters for UDP/TCP
     */
    data class NetworkParams(
        val host: String,
        val port: Int
    ) : ConnectionParams()
    
    /**
     * Bluetooth connection parameters
     */
    data class BluetoothParams(
        val deviceAddress: String,
        val deviceName: String
    ) : ConnectionParams()
}