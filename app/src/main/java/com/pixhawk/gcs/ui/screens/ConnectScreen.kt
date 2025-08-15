package com.pixhawk.gcs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pixhawk.gcs.transport.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    mavlinkParser: MavlinkParser,
    permissionsManager: PermissionsManager,
    modifier: Modifier = Modifier
) {
    var selectedTransport by remember { mutableStateOf(TransportType.UDP_LISTEN) }
    var host by remember { mutableStateOf("0.0.0.0") }
    var port by remember { mutableStateOf("14550") }
    var selectedBluetoothDevice by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
    
    val vehicleState by mavlinkParser.vehicleState.collectAsState()
    val transportManager = mavlinkParser.getTransportManager()
    val connectionState by transportManager.connectionState.collectAsState()
    val connectionInfo by transportManager.connectionInfo.collectAsState()
    val permissionState by permissionsManager.permissionState.collectAsState()
    
    val scope = rememberCoroutineScope()
    
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
        
        // Permissions Status Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (permissionState.allGranted) "All permissions granted" else "Some permissions missing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (permissionState.allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        if (!permissionState.allGranted) {
                            Text(
                                text = "Bluetooth: ${if (permissionState.bluetoothConnect && permissionState.bluetoothScan) "✓" else "✗"} • Location: ${if (permissionState.locationCoarse || permissionState.locationFine) "✓" else "✗"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (!permissionState.allGranted) {
                        Button(
                            onClick = { permissionsManager.requestPermissions() }
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }
        }
        
        // Transport Selection Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Transport Type",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Transport type selector
                val availableTransports = transportManager.getAvailableTransports()
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableTransports.size) { index ->
                        val transport = availableTransports[index]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTransport == transport,
                                onClick = { selectedTransport = transport },
                                enabled = connectionState != TransportState.CONNECTED
                            )
                            Text(
                                text = transportManager.getTransportDisplayName(transport),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Transport-specific configuration
                when (selectedTransport) {
                    TransportType.UDP_LISTEN, TransportType.UDP_CLIENT, TransportType.TCP_CLIENT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("Host") },
                                placeholder = { 
                                    Text(if (selectedTransport == TransportType.UDP_LISTEN) "0.0.0.0" else "192.168.1.100") 
                                },
                                modifier = Modifier.weight(2f),
                                singleLine = true,
                                enabled = connectionState != TransportState.CONNECTED
                            )
                            
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("Port") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = connectionState != TransportState.CONNECTED
                            )
                        }
                        
                        Text(
                            text = when (selectedTransport) {
                                TransportType.UDP_LISTEN -> "• Use 0.0.0.0 to listen for incoming MAVLink connections"
                                TransportType.UDP_CLIENT -> "• Connect directly to a MAVLink host (e.g., SITL or companion computer)"
                                TransportType.TCP_CLIENT -> "• TCP connection to MAVLink host (port usually 5760)"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    TransportType.BLUETOOTH_SPP -> {
                        if (permissionsManager.isBluetoothAvailable()) {
                            val bluetoothDevices = remember { transportManager.getBluetoothDevices() }
                            
                            if (bluetoothDevices.isNotEmpty()) {
                                var showDropdown by remember { mutableStateOf(false) }
                                
                                ExposedDropdownMenuBox(
                                    expanded = showDropdown,
                                    onExpandedChange = { showDropdown = it && connectionState != TransportState.CONNECTED }
                                ) {
                                    OutlinedTextField(
                                        value = selectedBluetoothDevice?.name ?: "Select Bluetooth Device",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        enabled = connectionState != TransportState.CONNECTED
                                    )
                                    
                                    ExposedDropdownMenu(
                                        expanded = showDropdown,
                                        onDismissRequest = { showDropdown = false }
                                    ) {
                                        bluetoothDevices.forEach { device ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(device.name)
                                                        Text(
                                                            device.address,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedBluetoothDevice = device
                                                    showDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No paired Bluetooth devices found. Please pair a device in Android Settings first.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                text = "Bluetooth permissions required for this transport type.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (connectionState) {
                                TransportState.DISCONNECTED -> "Disconnected"
                                TransportState.CONNECTING -> "Connecting..."
                                TransportState.CONNECTED -> "Connected"
                                TransportState.ERROR -> "Error"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = when (connectionState) {
                                TransportState.CONNECTED -> MaterialTheme.colorScheme.primary
                                TransportState.ERROR -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        if (connectionInfo.isNotEmpty()) {
                            Text(
                                text = connectionInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Connection Action Button
                    Button(
                        onClick = {
                            if (connectionState == TransportState.CONNECTED) {
                                scope.launch {
                                    transportManager.disconnect()
                                }
                            } else {
                                scope.launch {
                                    val params = when (selectedTransport) {
                                        TransportType.UDP_LISTEN, TransportType.UDP_CLIENT, TransportType.TCP_CLIENT -> {
                                            val portInt = port.toIntOrNull() ?: when (selectedTransport) {
                                                TransportType.TCP_CLIENT -> 5760
                                                else -> 14550
                                            }
                                            ConnectionParams.NetworkParams(host, portInt)
                                        }
                                        TransportType.BLUETOOTH_SPP -> {
                                            selectedBluetoothDevice?.let {
                                                ConnectionParams.BluetoothParams(it.address, it.name)
                                            } ?: return@launch
                                        }
                                    }
                                    transportManager.connect(selectedTransport, params)
                                }
                            }
                        },
                        enabled = when (selectedTransport) {
                            TransportType.BLUETOOTH_SPP -> permissionsManager.isBluetoothAvailable() && selectedBluetoothDevice != null
                            else -> true
                        } && connectionState != TransportState.CONNECTING,
                        colors = if (connectionState == TransportState.CONNECTED) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(
                            text = if (connectionState == TransportState.CONNECTED) "Disconnect" else "Connect",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        
        // Vehicle Information (when connected)
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
                        Text("System:", style = MaterialTheme.typography.bodyMedium)
                        Text("${vehicleState.systemId} • ${vehicleState.mode} • ${if (vehicleState.armed) "Armed" else "Disarmed"}", style = MaterialTheme.typography.bodyMedium)
                    }
                    
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