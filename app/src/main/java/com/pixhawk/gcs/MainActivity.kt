package com.pixhawk.gcs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixhawk.gcs.network.MavlinkParser
import com.pixhawk.gcs.permissions.rememberPermissionsManager
import com.pixhawk.gcs.ui.screens.*
import com.pixhawk.gcs.ui.theme.PixhawkGCSLiteTheme

class MainActivity : ComponentActivity() {
    private lateinit var mavlinkParser: MavlinkParser
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MAVLink parser
        mavlinkParser = MavlinkParser(this, lifecycleScope)
        
        setContent {
            PixhawkGCSLiteTheme {
                MainApp(mavlinkParser = mavlinkParser, activity = this)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mavlinkParser.stopConnection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(mavlinkParser: MavlinkParser, activity: ComponentActivity) {
    val navController = rememberNavController()
    
    // Initialize permissions manager
    val permissionsManager = rememberPermissionsManager(activity)
    
    // Define navigation items
    val items = listOf(
        NavigationItem("connect", "Connect", Icons.Default.Link),
        NavigationItem("fly", "Fly", Icons.Default.Flight),
        NavigationItem("missions", "Missions", Icons.Default.Map),
        NavigationItem("params", "Params", Icons.Default.Settings),
        NavigationItem("logs", "Logs", Icons.Default.List)
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination to avoid building up a large stack
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "connect",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("connect") { 
                ConnectScreen(
                    mavlinkParser = mavlinkParser,
                    permissionsManager = permissionsManager
                )
            }
            composable("fly") { 
                FlyScreen(
                    mavlinkParser = mavlinkParser,
                    permissionsManager = permissionsManager
                )
            }
            composable("missions") { 
                MissionsScreen()
            }
            composable("params") { 
                ParamsScreen()
            }
            composable("logs") { 
                LogsScreen()
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)