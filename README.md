# Pixhawk GCS Lite

A simplified ground control station for Pixhawk-compatible autopilots, inspired by QGroundControl but with a modern Material 3 UI and streamlined feature set.

## Features

- **Modern Material 3 UI**: Clean, polished interface with dynamic color theming and dark/light mode support
- **MAVLink Communication**: UDP telemetry connection with configurable host/port
- **Real-time Telemetry**: Live vehicle status, GPS position, attitude, battery, and system health
- **Google Maps Integration**: Vehicle tracking with breadcrumb trail and home markers  
- **Flight Control**: Basic actions (Arm/Disarm, mode changes, RTL, Takeoff)
- **Mission Management**: Download, upload, and start missions
- **Parameter Editor**: Browse, search, and modify vehicle parameters
- **System Logs**: Real-time logging with filtering and export capabilities

## Requirements

- Android 8.0 (API 26) or higher
- Network connection for MAVLink telemetry
- Google Maps API key (optional, for map functionality)

## Setup

### 1. Clone and Open
1. Extract the project from the ZIP archive
2. Open in Android Studio Arctic Fox or newer
3. Sync project with Gradle files

### 2. Google Maps API Key (Optional)
To enable map functionality:
1. Get a Google Maps API key from the [Google Cloud Console](https://console.cloud.google.com/)
2. In your `local.properties` file, add:
   ```
   MAPS_API_KEY=your_api_key_here
   ```
3. Alternatively, set it in your build configuration:
   ```
   android {
       defaultConfig {
           manifestPlaceholders = [MAPS_API_KEY: "your_api_key_here"]
       }
   }
   ```

The app will work without a Maps API key, but the map view will show a placeholder.

### 3. Build and Run
1. Build the project (Build → Make Project)
2. Run on a device or emulator (Run → Run 'app')

## Usage

### Connection
1. Open the **Connect** tab
2. Configure host/port (default: 127.0.0.1:14550)
3. Tap "Connect" to establish UDP telemetry link

### Flight Monitoring
1. Switch to the **Fly** tab to view:
   - Real-time HUD with vehicle status
   - Map with vehicle position and trail
   - Quick action buttons (Arm, RTL, Takeoff)

### Mission Planning
1. Use the **Missions** tab to:
   - Download current mission from vehicle
   - Upload new missions (requires ≥2 waypoints + RTL)
   - Start/stop mission execution

### Parameter Management
1. The **Params** tab allows:
   - Browsing all vehicle parameters
   - Searching parameters by name
   - Editing parameter values with confirmation

### System Logs
1. View real-time logs in the **Logs** tab
2. Filter by source and severity
3. Export logs for troubleshooting

## Development

### Architecture
- **MVVM** pattern with Jetpack Compose
- **Repository** pattern for data management
- **Flow/StateFlow** for reactive UI updates
- **Coroutines** for asynchronous operations

### Key Dependencies
- Jetpack Compose with Material 3
- Navigation Compose
- Google Maps Compose
- MAVLink Java library (io.dronefleet:mavlink)
- Kotlin Coroutines

### Testing
Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## License

Open source - see repository for license details.

## Contributing

Contributions welcome! Please follow Material Design guidelines and maintain code quality standards.