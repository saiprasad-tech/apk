cmake_minimum_required(VERSION 3.28)
project(PixhawkGCSNative LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
add_library(pixhawkcore SHARED
    NativeBridge.cpp
    telemetry/TelemetryEngine.cpp
    telemetry/TelemetryRingBuffer.cpp
    telemetry/StatsCalculator.cpp
)

target_include_directories(pixhawkcore PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
find_library(log-lib log)
 target_link_libraries(pixhawkcore PRIVATE ${log-lib})
