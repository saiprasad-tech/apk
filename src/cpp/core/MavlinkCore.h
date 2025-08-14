#pragma once

#include <string>
#include <functional>
#include <memory>

/**
 * @brief Core MAVLink interface for C++ backend
 * 
 * This class provides a C++ interface that can be used by both Qt (QML) 
 * and Android (via JNI) for MAVLink communication.
 * 
 * Based on the existing Kotlin implementation but extracted to C++ for
 * hybrid Android/Qt development.
 */
class MavlinkCore {
public:
    struct VehicleState {
        bool connected = false;
        bool armed = false;
        std::string mode = "UNKNOWN";
        int systemId = 0;
        int componentId = 0;
        double latitude = 0.0;
        double longitude = 0.0;
        float altitude = 0.0f;
        float heading = 0.0f;
        float roll = 0.0f;
        float pitch = 0.0f;
        float yaw = 0.0f;
        float groundSpeed = 0.0f;
        float airSpeed = 0.0f;
        float batteryVoltage = 0.0f;
        int batteryRemaining = 0;
        int gpsFixType = 0;
        int gpsNumSatellites = 0;
    };
    
    using StateUpdateCallback = std::function<void(const VehicleState&)>;
    
    MavlinkCore();
    ~MavlinkCore();
    
    // Connection management
    bool startConnection(const std::string& host, int port = 14550);
    void stopConnection();
    bool isConnected() const;
    
    // Vehicle commands
    void armDisarm(bool arm);
    void returnToLaunch();
    void takeoff(float altitude = 10.0f);
    void setMode(const std::string& mode);
    
    // State monitoring
    const VehicleState& getVehicleState() const;
    void setStateUpdateCallback(StateUpdateCallback callback);
    
    // Message processing
    void processMessage(const uint8_t* data, size_t length);
    
private:
    class Impl;
    std::unique_ptr<Impl> pImpl;
};

// C-style interface for JNI integration
extern "C" {
    // Create/destroy instance
    MavlinkCore* mavlink_core_create();
    void mavlink_core_destroy(MavlinkCore* core);
    
    // Connection functions
    int mavlink_core_start_connection(MavlinkCore* core, const char* host, int port);
    void mavlink_core_stop_connection(MavlinkCore* core);
    int mavlink_core_is_connected(MavlinkCore* core);
    
    // Commands
    void mavlink_core_arm_disarm(MavlinkCore* core, int arm);
    void mavlink_core_return_to_launch(MavlinkCore* core);
    void mavlink_core_takeoff(MavlinkCore* core, float altitude);
    
    // State access
    void mavlink_core_get_state(MavlinkCore* core, MavlinkCore::VehicleState* state);
}