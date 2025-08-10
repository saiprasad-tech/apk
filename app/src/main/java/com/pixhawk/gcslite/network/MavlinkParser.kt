package com.pixhawk.gcslite.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Internal MAVLink v1 parser/encoder implementation
 * Replaces the external dronefleet MAVLink dependency to eliminate build issues
 */
class MavlinkParser(private val scope: CoroutineScope) {
    
    // Vehicle state data class
    data class VehicleState(
        val connected: Boolean = false,
        val armed: Boolean = false,
        val mode: String = "UNKNOWN",
        val systemId: Int = 0,
        val componentId: Int = 0,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val altitude: Float = 0.0f,
        val heading: Float = 0.0f,
        val roll: Float = 0.0f,
        val pitch: Float = 0.0f,
        val yaw: Float = 0.0f,
        val groundSpeed: Float = 0.0f,
        val airSpeed: Float = 0.0f,
        val batteryVoltage: Float = 0.0f,
        val batteryCurrent: Float = 0.0f,
        val batteryRemaining: Int = 0,
        val gpsFixType: Int = 0,
        val gpsNumSatellites: Int = 0
    )
    
    // MAVLink v1 constants
    companion object {
        private const val MAVLINK_STX = 0xFE.toByte()
        private const val MAVLINK_HEADER_LEN = 6
        private const val MAVLINK_CRC_LEN = 2
        private const val MAVLINK_MAX_PAYLOAD_LEN = 255
        
        // Message IDs
        private const val MAVLINK_MSG_ID_HEARTBEAT = 0
        private const val MAVLINK_MSG_ID_SYS_STATUS = 1
        private const val MAVLINK_MSG_ID_ATTITUDE = 30
        private const val MAVLINK_MSG_ID_GLOBAL_POSITION_INT = 33
        private const val MAVLINK_MSG_ID_GPS_RAW_INT = 24
        private const val MAVLINK_MSG_ID_VFR_HUD = 74
        private const val MAVLINK_MSG_ID_COMMAND_LONG = 76
        
        // Command IDs for COMMAND_LONG
        private const val MAV_CMD_COMPONENT_ARM_DISARM = 400
        private const val MAV_CMD_NAV_TAKEOFF = 22
        private const val MAV_CMD_NAV_RETURN_TO_LAUNCH = 20
    }
    
    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()
    
    private var socket: DatagramSocket? = null
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0
    private var receiveJob: Job? = null
    private var currentSequence: Int = 0
    
    /**
     * Start UDP connection
     * @param host Host address (use "0.0.0.0" for listen-only mode)
     * @param port Port to bind to (default 14550)
     */
    fun startConnection(host: String, port: Int = 14550) {
        scope.launch {
            try {
                socket?.close()
                
                socket = if (host == "0.0.0.0") {
                    // Listen-only mode - bind to all interfaces
                    DatagramSocket(port)
                } else {
                    // Connect to specific host
                    val addr = InetAddress.getByName(host)
                    remoteAddress = addr
                    remotePort = port
                    DatagramSocket().apply {
                        connect(addr, port)
                    }
                }
                
                _vehicleState.value = _vehicleState.value.copy(connected = true)
                
                // Start receiving messages
                receiveJob = scope.launch {
                    receiveMessages()
                }
                
            } catch (e: Exception) {
                _vehicleState.value = _vehicleState.value.copy(connected = false)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Stop the connection
     */
    fun stopConnection() {
        receiveJob?.cancel()
        socket?.close()
        socket = null
        remoteAddress = null
        _vehicleState.value = _vehicleState.value.copy(connected = false)
    }
    
    /**
     * Send command to vehicle
     */
    fun sendCommand(command: Int, param1: Float = 0f, param2: Float = 0f, 
                   param3: Float = 0f, param4: Float = 0f, param5: Float = 0f, 
                   param6: Float = 0f, param7: Float = 0f) {
        scope.launch {
            val packet = createCommandLongPacket(command, param1, param2, param3, param4, param5, param6, param7)
            sendPacket(packet)
        }
    }
    
    /**
     * Arm/Disarm the vehicle
     */
    fun armDisarm(arm: Boolean) {
        sendCommand(MAV_CMD_COMPONENT_ARM_DISARM, if (arm) 1f else 0f)
    }
    
    /**
     * Return to Launch
     */
    fun returnToLaunch() {
        sendCommand(MAV_CMD_NAV_RETURN_TO_LAUNCH)
    }
    
    /**
     * Takeoff command
     */
    fun takeoff(altitude: Float = 10f) {
        sendCommand(MAV_CMD_NAV_TAKEOFF, 0f, 0f, 0f, 0f, 0f, 0f, altitude)
    }
    
    /**
     * Parse incoming UDP datagram data
     */
    fun parseDatagram(data: ByteArray, length: Int) {
        // Process the data byte by byte looking for MAVLink frames
        for (i in 0 until length) {
            processByte(data[i])
        }
    }
    
    // Message parsing state
    private var parseState = ParseState.WAITING_FOR_STX
    private var messageBuffer = ByteArray(MAVLINK_MAX_PAYLOAD_LEN + MAVLINK_HEADER_LEN + MAVLINK_CRC_LEN)
    private var bufferIndex = 0
    private var expectedLength = 0
    
    private enum class ParseState {
        WAITING_FOR_STX,
        READING_HEADER,
        READING_PAYLOAD,
        READING_CRC
    }
    
    private suspend fun receiveMessages() {
        val buffer = ByteArray(1024)
        
        while (!Thread.currentThread().isInterrupted && socket?.isClosed == false) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet)
                
                // If in listen mode and we haven't learned the sender yet, learn it
                if (remoteAddress == null) {
                    remoteAddress = packet.address
                    remotePort = packet.port
                }
                
                // Parse the received data
                parseDatagram(buffer, packet.length)
                
            } catch (e: Exception) {
                if (socket?.isClosed != true) {
                    e.printStackTrace()
                }
                break
            }
        }
    }
    
    private fun processByte(byte: Byte) {
        when (parseState) {
            ParseState.WAITING_FOR_STX -> {
                if (byte == MAVLINK_STX) {
                    messageBuffer[0] = byte
                    bufferIndex = 1
                    parseState = ParseState.READING_HEADER
                }
            }
            
            ParseState.READING_HEADER -> {
                messageBuffer[bufferIndex++] = byte
                if (bufferIndex >= MAVLINK_HEADER_LEN) {
                    // Got full header, extract payload length
                    val payloadLength = messageBuffer[1].toInt() and 0xFF
                    expectedLength = MAVLINK_HEADER_LEN + payloadLength + MAVLINK_CRC_LEN
                    parseState = if (payloadLength > 0) ParseState.READING_PAYLOAD else ParseState.READING_CRC
                }
            }
            
            ParseState.READING_PAYLOAD -> {
                messageBuffer[bufferIndex++] = byte
                if (bufferIndex >= expectedLength - MAVLINK_CRC_LEN) {
                    parseState = ParseState.READING_CRC
                }
            }
            
            ParseState.READING_CRC -> {
                messageBuffer[bufferIndex++] = byte
                if (bufferIndex >= expectedLength) {
                    // Complete message received
                    processMessage(messageBuffer, expectedLength)
                    parseState = ParseState.WAITING_FOR_STX
                    bufferIndex = 0
                }
            }
        }
    }
    
    private fun processMessage(buffer: ByteArray, length: Int) {
        if (length < MAVLINK_HEADER_LEN + MAVLINK_CRC_LEN) return
        
        val payloadLength = buffer[1].toInt() and 0xFF
        val sequence = buffer[2].toInt() and 0xFF
        val systemId = buffer[3].toInt() and 0xFF
        val componentId = buffer[4].toInt() and 0xFF
        val messageId = buffer[5].toInt() and 0xFF
        
        // Extract payload
        val payload = ByteArray(payloadLength)
        System.arraycopy(buffer, MAVLINK_HEADER_LEN, payload, 0, payloadLength)
        
        // Update system/component ID
        val currentState = _vehicleState.value
        if (currentState.systemId == 0) {
            _vehicleState.value = currentState.copy(systemId = systemId, componentId = componentId)
        }
        
        // Process specific message types
        when (messageId) {
            MAVLINK_MSG_ID_HEARTBEAT -> processHeartbeat(payload)
            MAVLINK_MSG_ID_SYS_STATUS -> processSysStatus(payload)
            MAVLINK_MSG_ID_ATTITUDE -> processAttitude(payload)
            MAVLINK_MSG_ID_GLOBAL_POSITION_INT -> processGlobalPositionInt(payload)
            MAVLINK_MSG_ID_GPS_RAW_INT -> processGpsRawInt(payload)
            MAVLINK_MSG_ID_VFR_HUD -> processVfrHud(payload)
        }
    }
    
    private fun processHeartbeat(payload: ByteArray) {
        if (payload.size < 9) return
        
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val customMode = buffer.int
        val type = buffer.get().toInt() and 0xFF
        val autopilot = buffer.get().toInt() and 0xFF
        val baseMode = buffer.get().toInt() and 0xFF
        val systemStatus = buffer.get().toInt() and 0xFF
        val mavlinkVersion = buffer.get().toInt() and 0xFF
        
        val armed = (baseMode and 0x80) != 0
        val mode = getModeString(customMode, baseMode)
        
        _vehicleState.value = _vehicleState.value.copy(
            armed = armed,
            mode = mode
        )
    }
    
    private fun processSysStatus(payload: ByteArray) {
        if (payload.size < 31) return
        
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(10) // Skip to voltage_battery
        val voltageMillivolts = buffer.short.toInt() and 0xFFFF
        val currentMilliamps = buffer.short.toInt() and 0xFFFF
        buffer.position(26) // Skip to battery_remaining
        val batteryRemaining = buffer.get().toInt() and 0xFF
        
        _vehicleState.value = _vehicleState.value.copy(
            batteryVoltage = voltageMillivolts / 1000f,
            batteryCurrent = currentMilliamps / 100f,
            batteryRemaining = batteryRemaining
        )
    }
    
    private fun processAttitude(payload: ByteArray) {
        if (payload.size < 28) return
        
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(4) // Skip time_boot_ms
        val roll = buffer.float
        val pitch = buffer.float
        val yaw = buffer.float
        
        _vehicleState.value = _vehicleState.value.copy(
            roll = Math.toDegrees(roll.toDouble()).toFloat(),
            pitch = Math.toDegrees(pitch.toDouble()).toFloat(),
            yaw = Math.toDegrees(yaw.toDouble()).toFloat()
        )
    }
    
    private fun processGlobalPositionInt(payload: ByteArray) {
        if (payload.size < 28) return
        
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(4) // Skip time_boot_ms
        val lat = buffer.int
        val lon = buffer.int
        val alt = buffer.int
        val relativeAlt = buffer.int
        
        _vehicleState.value = _vehicleState.value.copy(
            latitude = lat / 1e7,
            longitude = lon / 1e7,
            altitude = alt / 1000f
        )
    }
    
    private fun processGpsRawInt(payload: ByteArray) {
        if (payload.size < 30) return
        
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(12) // Skip to fix_type
        val fixType = buffer.get().toInt() and 0xFF
        val satellitesVisible = buffer.get().toInt() and 0xFF
        
        _vehicleState.value = _vehicleState.value.copy(
            gpsFixType = fixType,
            gpsNumSatellites = satellitesVisible
        )
    }
    
    private fun processVfrHud(payload: ByteArray) {
        if (payload.size < 20) return
        
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val airspeed = buffer.float
        val groundspeed = buffer.float
        val heading = buffer.short.toInt() and 0xFFFF
        
        _vehicleState.value = _vehicleState.value.copy(
            airSpeed = airspeed,
            groundSpeed = groundspeed,
            heading = heading.toFloat()
        )
    }
    
    private fun getModeString(customMode: Int, baseMode: Int): String {
        // Simplified mode mapping for ArduPilot
        return when {
            (baseMode and 0x01) != 0 -> "MANUAL"
            (baseMode and 0x02) != 0 -> "GUIDED" 
            (baseMode and 0x04) != 0 -> "AUTO"
            (baseMode and 0x10) != 0 -> "STABILIZE"
            else -> "UNKNOWN"
        }
    }
    
    private fun createCommandLongPacket(command: Int, param1: Float, param2: Float, 
                                      param3: Float, param4: Float, param5: Float, 
                                      param6: Float, param7: Float): ByteArray {
        val payload = ByteArray(33)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        
        buffer.putFloat(param1)
        buffer.putFloat(param2) 
        buffer.putFloat(param3)
        buffer.putFloat(param4)
        buffer.putFloat(param5)
        buffer.putFloat(param6)
        buffer.putFloat(param7)
        buffer.putShort(command.toShort())
        buffer.put((_vehicleState.value.systemId).toByte())
        buffer.put((_vehicleState.value.componentId).toByte())
        buffer.put(0) // confirmation
        
        return createMavlinkPacket(MAVLINK_MSG_ID_COMMAND_LONG, payload)
    }
    
    private fun createMavlinkPacket(messageId: Int, payload: ByteArray): ByteArray {
        val packet = ByteArray(MAVLINK_HEADER_LEN + payload.size + MAVLINK_CRC_LEN)
        val buffer = ByteBuffer.wrap(packet)
        
        buffer.put(MAVLINK_STX)
        buffer.put(payload.size.toByte())
        buffer.put((currentSequence++ and 0xFF).toByte())
        buffer.put(255.toByte()) // System ID (GCS)
        buffer.put(0.toByte())   // Component ID
        buffer.put(messageId.toByte())
        buffer.put(payload)
        
        // Simple CRC (placeholder - real implementation would calculate proper CRC)
        buffer.put(0.toByte())
        buffer.put(0.toByte())
        
        return packet
    }
    
    private fun sendPacket(data: ByteArray) {
        try {
            if (remoteAddress != null && socket != null) {
                val packet = DatagramPacket(data, data.size, remoteAddress, remotePort)
                socket?.send(packet)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}