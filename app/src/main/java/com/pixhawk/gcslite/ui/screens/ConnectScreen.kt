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
fun ConnectScreen(
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Connection Settings",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Status: ${if (vehicleState.connected) "Connected" else "Disconnected"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (vehicleState.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                if (vehicleState.connected) {
                    Text(
                        text = "System ID: ${vehicleState.systemId}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Mode: ${vehicleState.mode}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Armed: ${if (vehicleState.armed) "Yes" else "No"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (vehicleState.armed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Host/Port Configuration Card with proper layout
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Network Configuration",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Host and Port in a Row with proper weight distribution
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Host field - should take most of the available width
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host") },
                        placeholder = { Text("0.0.0.0 for listen mode") },
                        modifier = Modifier.weight(2f), // Takes 2/3 of available space
                        singleLine = true,
                        enabled = !vehicleState.connected
                    )
                    
                    // Port field - smaller but readable
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f), // Takes 1/3 of available space, minimum 120dp
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !vehicleState.connected
                    )
                }
                
                // Help text for host configuration
                Text(
                    text = "• Use 0.0.0.0 to listen for incoming connections\n• Use specific IP (e.g., 192.168.1.100) to connect directly\n• Default port is 14550 for MAVLink",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Connect/Disconnect Button
                Button(
                    onClick = {
                        if (vehicleState.connected) {
                            mavlinkParser.stopConnection()
                        } else {
                            val portInt = port.toIntOrNull() ?: 14550
                            mavlinkParser.startConnection(host, portInt)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (vehicleState.connected) "Disconnect" else "Connect",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        
        // Advanced Settings Card
        if (vehicleState.connected) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Vehicle Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Battery:", style = MaterialTheme.typography.bodyMedium)
                        Text("${vehicleState.batteryVoltage}V (${vehicleState.batteryRemaining}%)", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GPS:", style = MaterialTheme.typography.bodyMedium)
                        Text("Fix ${vehicleState.gpsFixType} (${vehicleState.gpsNumSatellites} sats)", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Position:", style = MaterialTheme.typography.bodyMedium)
                        Text("${String.format("%.6f", vehicleState.latitude)}, ${String.format("%.6f", vehicleState.longitude)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}