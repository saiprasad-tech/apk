package com.pixhawk.gcs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixhawk.gcs.network.MavlinkParser
import com.pixhawk.gcs.jni.MavlinkJNI
import kotlinx.coroutines.delay

@Composable
fun HybridDemoScreen(
    modifier: Modifier = Modifier,
    mavlinkParser: MavlinkParser,
    mavlinkJNI: MavlinkJNI?
) {
    var testResults by remember { mutableStateOf("") }
    var isRunningTest by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Hybrid Android/C++ Demo",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Backend status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Backend Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kotlin Backend:")
                    Text("✅ Available", color = MaterialTheme.colorScheme.primary)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("C++ Backend:")
                    Text(
                        text = if (mavlinkJNI != null) "✅ Available" else "❌ Not Available",
                        color = if (mavlinkJNI != null) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                
                if (mavlinkJNI != null) {
                    Text(
                        text = "C++ MAVLink core loaded successfully via JNI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "C++ backend not available. Using Kotlin implementation only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Test controls
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Backend Comparison Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = {
                        isRunningTest = true
                        testResults = "Running comparison test...\n"
                    },
                    enabled = !isRunningTest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRunningTest) "Running..." else "Run Performance Test")
                }
                
                if (testResults.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = testResults,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        
        // Architecture info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Hybrid Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text("• Android UI: Jetpack Compose + Material 3")
                Text("• C++ Core: High-performance MAVLink processing")  
                Text("• JNI Bridge: Seamless Android ↔ C++ communication")
                Text("• Qt Support: Optional QML components (desktop)")
                Text("• CMake Build: Cross-platform native compilation")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "This hybrid approach provides the best of both worlds: " +
                          "native Android performance with cross-platform C++ logic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Run test effect
    LaunchedEffect(isRunningTest) {
        if (isRunningTest) {
            testResults += "Testing Kotlin backend...\n"
            delay(1000)
            
            // Test Kotlin backend
            val kotlinStartTime = System.nanoTime()
            // Simulate some work
            repeat(1000) {
                mavlinkParser.vehicleState.value
            }
            val kotlinTime = (System.nanoTime() - kotlinStartTime) / 1_000_000
            testResults += "Kotlin: ${kotlinTime}ms for 1000 operations\n"
            
            if (mavlinkJNI != null) {
                testResults += "Testing C++ backend...\n"
                delay(1000)
                
                // Test C++ backend
                val cppStartTime = System.nanoTime()
                repeat(1000) {
                    mavlinkJNI.getVehicleState()
                }
                val cppTime = (System.nanoTime() - cppStartTime) / 1_000_000
                testResults += "C++: ${cppTime}ms for 1000 operations\n"
                
                val improvement = if (cppTime > 0) ((kotlinTime - cppTime) * 100 / kotlinTime) else 0
                testResults += "\nResult: C++ is ${improvement}% faster\n"
            } else {
                testResults += "C++ backend not available for testing\n"
            }
            
            testResults += "Test complete!"
            isRunningTest = false
        }
    }
}