cmake_minimum_required(VERSION 3.25)

# Project configuration - based on BDA-Lab CMake structure but adapted for Android hybrid
project(PixhawkGCS
    VERSION 1.0.0
    DESCRIPTION "Pixhawk Ground Control Station - Android/Qt Hybrid"
    LANGUAGES C CXX
)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android-specific configuration
if(ANDROID)
    set(CMAKE_ANDROID_STL_TYPE c++_shared)
    set(CMAKE_ANDROID_ARCH_ABI ${ANDROID_ABI})
    set(CMAKE_ANDROID_NDK_VERSION ${ANDROID_NDK_VERSION})
endif()

# Qt6 Configuration (optional for hybrid mode)
option(ENABLE_QT_SUPPORT "Enable Qt6 support for hybrid development" ON)

if(ENABLE_QT_SUPPORT)
    find_package(Qt6 6.2.0 QUIET
        COMPONENTS
            Core
            Gui  
            Quick
            Qml
            Network
            Positioning
        OPTIONAL_COMPONENTS
            Widgets
            Multimedia
    )
    
    if(Qt6_FOUND)
        message(STATUS "Qt6 found - enabling Qt support")
        set(QT_ENABLED ON)
        
        qt_standard_project_setup()
        
        # Qt policies
        qt_policy(SET QTP0001 NEW)
        qt_policy(SET QTP0002 NEW)
    else()
        message(STATUS "Qt6 not found - disabling Qt support")
        set(QT_ENABLED OFF)
    endif()
else()
    set(QT_ENABLED OFF)
endif()

# Include directories
include_directories(
    ${CMAKE_SOURCE_DIR}/src/cpp
    ${CMAKE_SOURCE_DIR}/src/cpp/mavlink
    ${CMAKE_SOURCE_DIR}/src/cpp/core
)

# Source files for C++ backend
file(GLOB_RECURSE CPP_SOURCES
    "src/cpp/*.cpp"
    "src/cpp/*.h"
)

# Create static library for C++ backend
if(CPP_SOURCES)
    add_library(pixhawk_gcs_core STATIC ${CPP_SOURCES})
    
    target_include_directories(pixhawk_gcs_core PUBLIC
        ${CMAKE_SOURCE_DIR}/src/cpp
    )
    
    # Android-specific libraries
    if(ANDROID)
        target_link_libraries(pixhawk_gcs_core
            android
            log
        )
    endif()
endif()

# Qt resources and QML (if Qt is enabled)
if(QT_ENABLED)
    set(QML_RESOURCES
        ${CMAKE_SOURCE_DIR}/resources/qml.qrc
    )
    
    # Create Qt executable/library
    if(ANDROID)
        # For Android, create a shared library
        qt_add_library(pixhawk_gcs_qt SHARED)
    else()
        # For desktop, create executable
        qt_add_executable(pixhawk_gcs_qt)
    endif()
    
    # Add QML module
    qt_add_qml_module(pixhawk_gcs_qt
        URI PixhawkGCS
        VERSION 1.0
        RESOURCE_PREFIX "/qml"
    )
    
    if(TARGET pixhawk_gcs_core)
        target_link_libraries(pixhawk_gcs_qt PRIVATE pixhawk_gcs_core)
    endif()
    
    target_link_libraries(pixhawk_gcs_qt PRIVATE
        Qt6::Core
        Qt6::Gui
        Qt6::Quick
        Qt6::Qml
        Qt6::Network
        Qt6::Positioning
    )
endif()

# Install rules
if(NOT ANDROID)
    install(TARGETS pixhawk_gcs_core
        LIBRARY DESTINATION lib
        ARCHIVE DESTINATION lib
        RUNTIME DESTINATION bin
    )
    
    if(QT_ENABLED AND TARGET pixhawk_gcs_qt)
        install(TARGETS pixhawk_gcs_qt
            RUNTIME DESTINATION bin
        )
    endif()
endif()

# Build summary
message(STATUS "=== Pixhawk GCS Build Configuration ===")
message(STATUS "Project: ${CMAKE_PROJECT_NAME} v${PROJECT_VERSION}")
message(STATUS "Platform: ${CMAKE_SYSTEM_NAME}")
message(STATUS "Qt6 Support: ${QT_ENABLED}")
if(ANDROID)
    message(STATUS "Android ABI: ${ANDROID_ABI}")
    message(STATUS "Android API: ${ANDROID_PLATFORM}")
endif()
message(STATUS "=====================================")