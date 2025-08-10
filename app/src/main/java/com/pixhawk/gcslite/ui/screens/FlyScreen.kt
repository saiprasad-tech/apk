package com.pixhawk.gcslite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixhawk.gcslite.network.MavlinkParser

@Composable
fun FlyScreen(
    mavlinkParser: MavlinkParser,
    modifier: Modifier = Modifier
) {
    var host by remember { mutableStateOf("0.0.0.0") }
    var port by remember { mutableStateOf("14550") }
    val vehicleState by mavlinkParser.vehicleState.collectAsState()
    
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
        
        // Connection Controls - Always visible in Fly mode as per requirements
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Connection",
                    style = MaterialTheme.typography.titleSmall
                )
                
                // Host/Port layout with proper sizing - same fix as Connect tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Host field - expandable, not a tiny vertical rectangle
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host") },
                        modifier = Modifier.weight(2f), // Ensures Host gets proper width
                        singleLine = true,
                        enabled = !vehicleState.connected
                    )
                    
                    // Port field - readable, not cramped
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !vehicleState.connected
                    )
                    
                    // Connect button
                    Button(
                        onClick = {
                            if (vehicleState.connected) {
                                mavlinkParser.stopConnection()
                            } else {
                                val portInt = port.toIntOrNull() ?: 14550
                                mavlinkParser.startConnection(host, portInt)
                            }
                        },
                        modifier = Modifier.wrapContentWidth(),
                        colors = if (vehicleState.connected) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(if (vehicleState.connected) "Disc." else "Conn.")
                    }
                }
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