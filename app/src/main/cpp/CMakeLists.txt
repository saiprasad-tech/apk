cmake_minimum_required(VERSION 3.22)

project(pixhawkcore LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
    add_compile_options(-Wall -Wextra -Wpedantic -Wconversion -Wno-unused-parameter)
endif()

option(PIXHAWKCORE_VERBOSE "Enable verbose native logging" ON)

add_library(pixhawkcore SHARED
    SystemBridge.cpp

    navigation/NavigationEngine.cpp
    navigation/AStar.cpp
    navigation/CostMap.cpp

    sensorsim/SensorSim.cpp

    sensorfusion/EkfAttitude.cpp
    sensorfusion/MathQuat.cpp

    geospatial/GeoidModel.cpp
    geospatial/MagneticModel.cpp
    geospatial/ElevationLookup.cpp
    geospatial/data/WmmCoeffs.inl
    geospatial/data/GeoidHeights.inl

    logparser/LogParser.cpp
)

target_include_directories(pixhawkcore
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/navigation
        ${CMAKE_CURRENT_SOURCE_DIR}/sensorsim
        ${CMAKE_CURRENT_SOURCE_DIR}/sensorfusion
        ${CMAKE_CURRENT_SOURCE_DIR}/geospatial
        ${CMAKE_CURRENT_SOURCE_DIR}/logparser
        ${CMAKE_CURRENT_SOURCE_DIR}/geospatial/data
)

if (PIXHAWKCORE_VERBOSE)
    target_compile_definitions(pixhawkcore PRIVATE PIXHAWKCORE_VERBOSE=1)
endif()

find_library(log-lib log)

target_link_libraries(pixhawkcore
    ${log-lib}
)

# Retain symbols to enlarge APK and aid debugging
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -g")