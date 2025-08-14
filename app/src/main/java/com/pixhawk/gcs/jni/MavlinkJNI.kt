package com.pixhawk.gcs.jni

/**
 * JNI bridge to the C++ MavlinkCore backend
 * 
 * This class provides a Kotlin interface to the C++ MAVLink implementation,
 * allowing the existing Android UI to use the hybrid C++ backend.
 */
class MavlinkJNI {
    
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
        val batteryRemaining: Int = 0,
        val gpsFixType: Int = 0,
        val gpsNumSatellites: Int = 0
    )
    
    companion object {
        init {
            try {
                System.loadLibrary("pixhawk_gcs_core")
            } catch (e: UnsatisfiedLinkError) {
                // Library not available, fallback to pure Kotlin implementation
                println("C++ library not available: ${e.message}")
            }
        }
    }
    
    private var nativePtr: Long = 0
    private var stateUpdateCallback: ((VehicleState) -> Unit)? = null
    
    init {
        nativePtr = nativeCreate()
    }
    
    fun destroy() {
        if (nativePtr != 0L) {
            nativeDestroy(nativePtr)
            nativePtr = 0
        }
    }
    
    fun startConnection(host: String, port: Int = 14550): Boolean {
        return if (nativePtr != 0L) {
            nativeStartConnection(nativePtr, host, port)
        } else {
            false
        }
    }
    
    fun stopConnection() {
        if (nativePtr != 0L) {
            nativeStopConnection(nativePtr)
        }
    }
    
    fun isConnected(): Boolean {
        return if (nativePtr != 0L) {
            nativeIsConnected(nativePtr)
        } else {
            false
        }
    }
    
    fun armDisarm(arm: Boolean) {
        if (nativePtr != 0L) {
            nativeArmDisarm(nativePtr, arm)
        }
    }
    
    fun returnToLaunch() {
        if (nativePtr != 0L) {
            nativeReturnToLaunch(nativePtr)
        }
    }
    
    fun takeoff(altitude: Float = 10.0f) {
        if (nativePtr != 0L) {
            nativeTakeoff(nativePtr, altitude)
        }
    }
    
    fun getVehicleState(): VehicleState {
        return if (nativePtr != 0L) {
            val state = nativeGetState(nativePtr)
            state ?: VehicleState()
        } else {
            VehicleState()
        }
    }
    
    fun setStateUpdateCallback(callback: (VehicleState) -> Unit) {
        stateUpdateCallback = callback
    }
    
    // Called from native code
    @Suppress("unused")
    private fun onStateUpdate(
        connected: Boolean, armed: Boolean, mode: String, systemId: Int, componentId: Int,
        latitude: Double, longitude: Double, altitude: Float, heading: Float,
        roll: Float, pitch: Float, yaw: Float, groundSpeed: Float, airSpeed: Float,
        batteryVoltage: Float, batteryRemaining: Int, gpsFixType: Int, gpsNumSatellites: Int
    ) {
        val state = VehicleState(
            connected, armed, mode, systemId, componentId, latitude, longitude,
            altitude, heading, roll, pitch, yaw, groundSpeed, airSpeed,
            batteryVoltage, batteryRemaining, gpsFixType, gpsNumSatellites
        )
        stateUpdateCallback?.invoke(state)
    }
    
    // Native methods
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(ptr: Long)
    private external fun nativeStartConnection(ptr: Long, host: String, port: Int): Boolean
    private external fun nativeStopConnection(ptr: Long)
    private external fun nativeIsConnected(ptr: Long): Boolean
    private external fun nativeArmDisarm(ptr: Long, arm: Boolean)
    private external fun nativeReturnToLaunch(ptr: Long)
    private external fun nativeTakeoff(ptr: Long, altitude: Float)
    private external fun nativeGetState(ptr: Long): VehicleState?
}