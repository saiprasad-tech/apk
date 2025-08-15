package com.pixhawk.gcs.transport

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Transport manager coordinates a single active transport and provides
 * unified access to connection state and operations
 */
class TransportManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    private var currentTransport: Transport? = null
    
    // Combined flow that merges state from all possible transports
    private val _connectionState = MutableStateFlow(TransportState.DISCONNECTED)
    val connectionState: StateFlow<TransportState> = _connectionState.asStateFlow()
    
    private val _activeTransportType = MutableStateFlow<TransportType?>(null)
    val activeTransportType: StateFlow<TransportType?> = _activeTransportType.asStateFlow()
    
    private val _connectionInfo = MutableStateFlow("")
    val connectionInfo: StateFlow<String> = _connectionInfo.asStateFlow()
    
    private var dataCallback: ((ByteArray, Int) -> Unit)? = null
    private var stateObserverJob: Job? = null
    
    /**
     * Set callback for received data from any transport
     */
    fun setDataCallback(callback: (ByteArray, Int) -> Unit) {
        dataCallback = callback
        currentTransport?.setDataCallback(callback)
    }
    
    /**
     * Connect using the specified transport type and parameters
     */
    suspend fun connect(transportType: TransportType, params: ConnectionParams): Boolean {
        // Disconnect any existing transport
        disconnect()
        
        try {
            // Create new transport instance
            val transport = createTransport(transportType)
            currentTransport = transport
            _activeTransportType.value = transportType
            
            // Set up data callback
            dataCallback?.let { transport.setDataCallback(it) }
            
            // Observe transport state
            stateObserverJob = scope.launch {
                transport.connectionState.collect { state ->
                    _connectionState.value = state
                    _connectionInfo.value = if (state == TransportState.CONNECTED) {
                        transport.getConnectionInfo()
                    } else {
                        ""
                    }
                }
            }
            
            // Attempt connection
            return transport.connect(params)
            
        } catch (e: Exception) {
            _connectionState.value = TransportState.ERROR
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Disconnect current transport
     */
    suspend fun disconnect() {
        stateObserverJob?.cancel()
        stateObserverJob = null
        
        currentTransport?.disconnect()
        currentTransport = null
        
        _activeTransportType.value = null
        _connectionState.value = TransportState.DISCONNECTED
        _connectionInfo.value = ""
    }
    
    /**
     * Send data over current active transport
     */
    suspend fun sendData(data: ByteArray): Boolean {
        return currentTransport?.sendData(data) ?: false
    }
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return connectionState.value == TransportState.CONNECTED
    }
    
    /**
     * Get available transport types based on device capabilities and permissions
     */
    fun getAvailableTransports(): List<TransportType> {
        val available = mutableListOf<TransportType>()
        
        // Network transports are always available
        available.add(TransportType.UDP_LISTEN)
        available.add(TransportType.UDP_CLIENT)
        available.add(TransportType.TCP_CLIENT)
        
        // Bluetooth only if available and permissions granted
        if (BluetoothTransport.checkBluetoothPermissions(context)) {
            available.add(TransportType.BLUETOOTH_SPP)
        }
        
        return available
    }
    
    /**
     * Get paired Bluetooth devices for selection
     */
    fun getBluetoothDevices(): List<BluetoothDeviceInfo> {
        val bluetoothTransport = BluetoothTransport(context, scope)
        return bluetoothTransport.getPairedDevices()
    }
    
    /**
     * Create transport instance based on type
     */
    private fun createTransport(transportType: TransportType): Transport {
        return when (transportType) {
            TransportType.UDP_LISTEN -> UdpTransport(scope, TransportType.UDP_LISTEN)
            TransportType.UDP_CLIENT -> UdpTransport(scope, TransportType.UDP_CLIENT)
            TransportType.TCP_CLIENT -> TcpTransport(scope)
            TransportType.BLUETOOTH_SPP -> BluetoothTransport(context, scope)
        }
    }
    
    /**
     * Get default connection parameters for transport type
     */
    fun getDefaultParams(transportType: TransportType): ConnectionParams {
        return when (transportType) {
            TransportType.UDP_LISTEN -> ConnectionParams.NetworkParams("0.0.0.0", 14550)
            TransportType.UDP_CLIENT -> ConnectionParams.NetworkParams("192.168.1.100", 14550)
            TransportType.TCP_CLIENT -> ConnectionParams.NetworkParams("192.168.1.100", 5760)
            TransportType.BLUETOOTH_SPP -> ConnectionParams.BluetoothParams("", "")
        }
    }
    
    /**
     * Get user-friendly name for transport type
     */
    fun getTransportDisplayName(transportType: TransportType): String {
        return when (transportType) {
            TransportType.UDP_LISTEN -> "UDP Listen"
            TransportType.UDP_CLIENT -> "UDP Client"
            TransportType.TCP_CLIENT -> "TCP Client"
            TransportType.BLUETOOTH_SPP -> "Bluetooth SPP"
        }
    }
}