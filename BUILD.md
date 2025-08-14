# Build Instructions for Pixhawk_GCS Hybrid Project

This document explains how to build the Pixhawk_GCS hybrid Android/Qt project.

## Prerequisites

### Required
- Android Studio Arctic Fox or newer
- Android SDK 34
- Android NDK 25 or newer
- CMake 3.25+
- JDK 17

### Optional (for Qt features)
- Qt6 6.2+ with the following modules:
  - Qt6Core
  - Qt6Gui  
  - Qt6Quick
  - Qt6Qml
  - Qt6Network
  - Qt6Positioning

## Build Modes

### 1. Android-Only Build (Recommended for most users)

This builds the Android application with C++ backend but without Qt components.

```bash
# Open in Android Studio and build normally
# OR use command line:
./gradlew assembleDebug
```

The C++ components will be compiled via CMake automatically through the Android build system.

### 2. Hybrid Android + C++ Build

For advanced users who want to develop/debug the C++ components:

```bash
# Build C++ components separately
mkdir build-android
cd build-android
cmake .. \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DENABLE_QT_SUPPORT=OFF
make

# Then build Android app
cd ..
./gradlew assembleDebug
```

### 3. Desktop Qt Build (Development/Testing)

For cross-platform development and testing Qt components:

```bash
# Requires Qt6 installation
mkdir build-desktop
cd build-desktop
cmake .. -DENABLE_QT_SUPPORT=ON
make

# Run Qt application
./pixhawk_gcs_qt
```

## Configuration Options

### CMake Options

- `ENABLE_QT_SUPPORT`: Enable/disable Qt6 support (default: ON)
- `ANDROID_ABI`: Android ABI to build for (arm64-v8a, armeabi-v7a, x86, x86_64)
- `CMAKE_BUILD_TYPE`: Debug or Release

### Gradle Options

The Android build automatically:
- Compiles C++ code via CMake
- Links native libraries
- Packages everything into APK

## Troubleshooting

### Common Issues

1. **CMake not found**
   - Install CMake 3.25+ 
   - Add to PATH or set ANDROID_CMAKE_PATH

2. **NDK not found**
   - Install Android NDK via Android Studio
   - Set ANDROID_NDK_HOME environment variable

3. **Qt not found** (if using Qt features)
   - Install Qt6 with required modules
   - Set CMAKE_PREFIX_PATH to Qt installation

4. **JNI compilation errors**
   - Ensure NDK version compatibility
   - Check C++ standard version (requires C++17)

### Build Clean

```bash
# Clean Android build
./gradlew clean

# Clean CMake build
rm -rf build-android build-desktop

# Clean everything
git clean -fdx
```

## Architecture Notes

The hybrid build system works as follows:

1. **Gradle** manages the Android application build
2. **CMake** compiles the C++ backend and optional Qt components
3. **JNI** bridges Android (Kotlin) and native C++ code
4. **Qt** components are optional and only built when requested

This allows developers to:
- Use Android-only build for mobile deployment
- Use hybrid build for performance optimization
- Use Qt build for cross-platform development

## Development Workflow

1. **Android UI development**: Use Android Studio normally
2. **C++ backend development**: Edit C++ files, rebuild with cmake
3. **Qt components**: Develop QML files, test with desktop Qt build
4. **Integration testing**: Use hybrid demo screen in Android app

The hybrid demo screen shows backend performance comparison and validates the JNI bridge functionality.