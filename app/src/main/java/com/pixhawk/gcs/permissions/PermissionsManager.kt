package com.pixhawk.gcs.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Permissions manager for handling runtime permissions required by the Ground Control Station
 */
class PermissionsManager(private val activity: ComponentActivity) {
    
    data class PermissionState(
        val bluetoothConnect: Boolean = false,
        val bluetoothScan: Boolean = false,
        val locationCoarse: Boolean = false,
        val locationFine: Boolean = false,
        val allGranted: Boolean = false
    )
    
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    
    companion object {
        /**
         * Get required permissions based on Android version
         */
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires new Bluetooth permissions
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                // Pre-Android 12 uses legacy permissions
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
        
        /**
         * Check if permission is granted
         */
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                context, 
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Initialize permissions manager - call from onCreate
     */
    fun initialize() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            updatePermissionState()
        }
        
        // Check initial permission state
        updatePermissionState()
    }
    
    /**
     * Request all required permissions
     */
    fun requestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter { permission ->
            !isPermissionGranted(activity, permission)
        }.toTypedArray()
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions)
        }
    }
    
    /**
     * Check if should show rationale for a permission
     */
    fun shouldShowRationale(permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }
    
    /**
     * Get permission rationale message
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN -> {
                "Bluetooth permissions are required to connect to MAVLink devices via Bluetooth. This allows the app to discover and communicate with your drone or ground station."
            }
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION -> {
                "Location permissions are required for Bluetooth scanning and network discovery features. Your location is not stored or transmitted."
            }
            else -> "This permission is required for the app to function properly."
        }
    }
    
    /**
     * Update internal permission state
     */
    private fun updatePermissionState() {
        val bluetoothConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isPermissionGranted(activity, Manifest.permission.BLUETOOTH_CONNECT)
        } else true // Not required on older versions
        
        val bluetoothScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isPermissionGranted(activity, Manifest.permission.BLUETOOTH_SCAN)
        } else true // Not required on older versions
        
        val locationCoarse = isPermissionGranted(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        val locationFine = isPermissionGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        
        val allGranted = bluetoothConnect && bluetoothScan && locationCoarse && locationFine
        
        _permissionState.value = PermissionState(
            bluetoothConnect = bluetoothConnect,
            bluetoothScan = bluetoothScan,
            locationCoarse = locationCoarse,
            locationFine = locationFine,
            allGranted = allGranted
        )
    }
    
    /**
     * Check if Bluetooth permissions are available for use
     */
    fun isBluetoothAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _permissionState.value.bluetoothConnect && _permissionState.value.bluetoothScan
        } else {
            true // Legacy permissions are install-time on older versions
        }
    }
    
    /**
     * Check if location permissions are available for use  
     */
    fun isLocationAvailable(): Boolean {
        return _permissionState.value.locationCoarse || _permissionState.value.locationFine
    }
}

/**
 * Composable for checking and requesting permissions
 */
@Composable
fun rememberPermissionsManager(activity: ComponentActivity): PermissionsManager {
    val permissionsManager = remember { PermissionsManager(activity) }
    
    LaunchedEffect(permissionsManager) {
        permissionsManager.initialize()
    }
    
    return permissionsManager
}