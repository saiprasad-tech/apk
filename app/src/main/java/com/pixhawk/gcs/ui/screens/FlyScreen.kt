package com.pixhawk.gcs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixhawk.gcs.network.MavlinkParser
import com.pixhawk.gcs.permissions.PermissionsManager
import com.pixhawk.gcs.transport.TransportState
import kotlinx.coroutines.launch

@Composable
fun FlyScreen(
    mavlinkParser: MavlinkParser,
    permissionsManager: PermissionsManager,
    modifier: Modifier = Modifier
) {
    var host by remember { mutableStateOf("0.0.0.0") }
    var port by remember { mutableStateOf("14550") }
    val vehicleState by mavlinkParser.vehicleState.collectAsState()
    val transportManager = mavlinkParser.getTransportManager()
    val telemetryLogger = mavlinkParser.getTelemetryLogger()
    val connectionState by transportManager.connectionState.collectAsState()
    val connectionInfo by transportManager.connectionInfo.collectAsState()
    val isLogging by telemetryLogger.isLogging.collectAsState()
    val currentLogFile by telemetryLogger.currentLogFile.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Flight Control",
            style = MaterialTheme.typography.headlineMedium
        )
        
        
        // Connection Status & Telemetry Logging Controls
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Connection & Logging",
                    style = MaterialTheme.typography.titleSmall
                )
                
                // Connection Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (connectionState) {
                                TransportState.CONNECTED -> "Connected"
                                TransportState.CONNECTING -> "Connecting..."
                                TransportState.DISCONNECTED -> "Disconnected"
                                TransportState.ERROR -> "Error"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (connectionState == TransportState.CONNECTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (connectionInfo.isNotEmpty()) {
                            Text(
                                text = connectionInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Telemetry Logging Toggle
                        Button(
                            onClick = {
                                scope.launch {
                                    if (isLogging) {
                                        telemetryLogger.stopLogging()
                                    } else {
                                        telemetryLogger.startLogging()
                                    }
                                }
                            },
                            colors = if (isLogging) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Icon(
                                imageVector = if (isLogging) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isLogging) "Stop Log" else "Start Log",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isLogging) "Stop Log" else "Start Log")
                        }
                        
                        // Quick Connect/Disconnect 
                        Button(
                            onClick = {
                                if (connectionState == TransportState.CONNECTED) {
                                    scope.launch {
                                        transportManager.disconnect()
                                    }
                                } else {
                                    val portInt = port.toIntOrNull() ?: 14550
                                    mavlinkParser.startConnection(host, portInt)
                                }
                            },
                            colors = if (connectionState == TransportState.CONNECTED) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                            enabled = connectionState != TransportState.CONNECTING
                        ) {
                            Text(if (connectionState == TransportState.CONNECTED) "Disc." else "Conn.")
                        }
                    }
                }
                
                // Logging Status
                if (isLogging) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Recording telemetry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        currentLogFile?.let { fileName ->
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
        
        // HUD/Status Display
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Vehicle Status",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Primary Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusIndicator(
                        label = "Mode",
                        value = vehicleState.mode,
                        color = if (vehicleState.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    StatusIndicator(
                        label = "Armed",
                        value = if (vehicleState.armed) "YES" else "NO",
                        color = if (vehicleState.armed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                    StatusIndicator(
                        label = "Battery",
                        value = "${vehicleState.batteryVoltage}V",
                        color = when {
                            vehicleState.batteryVoltage > 11.5f -> MaterialTheme.colorScheme.primary
                            vehicleState.batteryVoltage > 10.5f -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Attitude Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusIndicator(
                        label = "Roll",
                        value = "${String.format("%.1f", vehicleState.roll)}°",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusIndicator(
                        label = "Pitch", 
                        value = "${String.format("%.1f", vehicleState.pitch)}°",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusIndicator(
                        label = "Yaw",
                        value = "${String.format("%.1f", vehicleState.yaw)}°",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Speed and Position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusIndicator(
                        label = "G.Speed",
                        value = "${String.format("%.1f", vehicleState.groundSpeed)} m/s",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusIndicator(
                        label = "Altitude",
                        value = "${String.format("%.1f", vehicleState.altitude)} m",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusIndicator(
                        label = "GPS",
                        value = "${vehicleState.gpsNumSatellites} sats",
                        color = if (vehicleState.gpsFixType >= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        
        // Flight Control Actions
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Flight Actions",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Primary action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Arm/Disarm button
                    Button(
                        onClick = { mavlinkParser.armDisarm(!vehicleState.armed) },
                        modifier = Modifier.weight(1f),
                        enabled = vehicleState.connected,
                        colors = if (vehicleState.armed) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    ) {
                        Text(if (vehicleState.armed) "DISARM" else "ARM")
                    }
                    
                    // RTL button
                    Button(
                        onClick = { mavlinkParser.returnToLaunch() },
                        modifier = Modifier.weight(1f),
                        enabled = vehicleState.connected && vehicleState.armed,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("RTL")
                    }
                }
                
                // Takeoff button (full width)
                Button(
                    onClick = { mavlinkParser.takeoff(10f) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = vehicleState.connected && vehicleState.armed,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("TAKEOFF (10m)")
                }
            }
        }
        
        // Position Display
        if (vehicleState.latitude != 0.0 || vehicleState.longitude != 0.0) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Position",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Lat: ${String.format("%.6f", vehicleState.latitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Lon: ${String.format("%.6f", vehicleState.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Alt: ${String.format("%.1f", vehicleState.altitude)} m",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}