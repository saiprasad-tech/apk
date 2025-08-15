package com.pixhawk.gcs.transport

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Enhanced Bluetooth transport for MAVLink communication
 * Supports active SPP (Serial Port Profile) connections to paired devices
 */
class BluetoothTransport(
    private val context: Context,
    private val scope: CoroutineScope
) : Transport {
    
    override val transportType: TransportType = TransportType.BLUETOOTH_SPP
    
    private val _connectionState = MutableStateFlow(TransportState.DISCONNECTED)
    override val connectionState: StateFlow<TransportState> = _connectionState.asStateFlow()
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var receiveJob: Job? = null
    private var dataCallback: ((ByteArray, Int) -> Unit)? = null
    
    private var deviceAddress: String = ""
    private var deviceName: String = ""
    
    companion object {
        // Standard SPP UUID
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        /**
         * Check if Bluetooth permissions are granted
         */
        fun checkBluetoothPermissions(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Legacy permissions are install-time
            }
        }
        
        /**
         * Get list of paired Bluetooth devices that potentially support SPP
         */
        fun getPairedDevices(context: Context, bluetoothAdapter: BluetoothAdapter?): List<BluetoothDeviceInfo> {
            if (!checkBluetoothPermissions(context) || bluetoothAdapter?.isEnabled != true) {
                return emptyList()
            }
            
            return try {
                bluetoothAdapter.bondedDevices?.map { device ->
                    BluetoothDeviceInfo(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        isPaired = true
                    )
                } ?: emptyList()
            } catch (e: SecurityException) {
                emptyList()
            }
        }
    }
    
    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }
    
    override suspend fun connect(params: ConnectionParams): Boolean {
        if (params !is ConnectionParams.BluetoothParams) {
            return false
        }
        
        if (!checkBluetoothPermissions(context)) {
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = TransportState.CONNECTING
                
                // Close existing connection
                disconnect()
                
                deviceAddress = params.deviceAddress
                deviceName = params.deviceName
                
                val adapter = bluetoothAdapter
                if (adapter == null || !adapter.isEnabled) {
                    _connectionState.value = TransportState.ERROR
                    return@withContext false
                }
                
                // Get the Bluetooth device
                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                
                // Create SPP socket
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket = socket
                
                // Cancel discovery to improve connection performance
                adapter.cancelDiscovery()
                
                // Connect to the device
                socket.connect()
                
                // Get input/output streams
                inputStream = socket.inputStream
                outputStream = socket.outputStream
                
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
                bluetoothSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            inputStream = null
            outputStream = null
            bluetoothSocket = null
        }
        
        _connectionState.value = TransportState.DISCONNECTED
    }
    
    override suspend fun sendData(data: ByteArray): Boolean {
        val currentOutputStream = outputStream
        
        if (currentOutputStream == null || bluetoothSocket?.isConnected != true) {
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
        return "Bluetooth SPP to $deviceName ($deviceAddress)"
    }
    
    private suspend fun receiveMessages() {
        val buffer = ByteArray(1024)
        val currentInputStream = inputStream
        
        if (currentInputStream == null) return
        
        while (!Thread.currentThread().isInterrupted && 
               bluetoothSocket?.isConnected == true && 
               connectionState.value == TransportState.CONNECTED) {
            try {
                val bytesRead = currentInputStream.read(buffer)
                if (bytesRead > 0) {
                    // Notify callback of received data
                    dataCallback?.invoke(buffer, bytesRead)
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
    
    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true && checkBluetoothPermissions(context)
    }
    
    /**
     * Get paired devices that can be used for MAVLink communication
     */
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        return getPairedDevices(context, bluetoothAdapter)
    }
}

/**
 * Bluetooth device information
 */
data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean
)