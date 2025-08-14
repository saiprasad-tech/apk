#include "MavlinkCore.h"
#include <thread>
#include <mutex>
#include <atomic>
#include <vector>
#include <cstring>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "MavlinkCore"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#include <iostream>
#define LOGD(...) printf(__VA_ARGS__); printf("\n")
#define LOGE(...) fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n")
#endif

// Simple MAVLink v1 constants (based on existing Kotlin implementation)
constexpr uint8_t MAV_STX = 0xFE;
constexpr uint8_t MAVLINK_MAX_PACKET_LEN = 263;

// MAVLink message IDs
constexpr uint8_t MAVLINK_MSG_ID_HEARTBEAT = 0;
constexpr uint8_t MAVLINK_MSG_ID_SYS_STATUS = 1;
constexpr uint8_t MAVLINK_MSG_ID_GPS_RAW_INT = 24;
constexpr uint8_t MAVLINK_MSG_ID_ATTITUDE = 30;
constexpr uint8_t MAVLINK_MSG_ID_GLOBAL_POSITION_INT = 33;

// MAVLink commands
constexpr uint16_t MAV_CMD_COMPONENT_ARM_DISARM = 400;
constexpr uint16_t MAV_CMD_NAV_TAKEOFF = 22;
constexpr uint16_t MAV_CMD_NAV_RETURN_TO_LAUNCH = 20;

class MavlinkCore::Impl {
public:
    VehicleState vehicleState;
    StateUpdateCallback stateCallback;
    std::mutex stateMutex;
    std::atomic<bool> shouldStop{false};
    std::thread networkThread;
    
    // Parsing state
    enum ParseState {
        WAITING_FOR_STX,
        GOT_STX,
        GOT_LENGTH,
        GOT_SEQ,
        GOT_SYSID,
        GOT_COMPID,
        GOT_MSGID,
        GOT_PAYLOAD,
        GOT_CHECKSUM1
    };
    
    ParseState parseState = WAITING_FOR_STX;
    uint8_t messageBuffer[MAVLINK_MAX_PACKET_LEN];
    size_t messageIndex = 0;
    uint8_t expectedLength = 0;
    
    void processByte(uint8_t byte);
    void processCompleteMessage();
    void updateState(const VehicleState& newState);
    void sendCommand(uint16_t command, float param1 = 0.0f, float param2 = 0.0f,
                     float param3 = 0.0f, float param4 = 0.0f, float param5 = 0.0f,
                     float param6 = 0.0f, float param7 = 0.0f);
};

MavlinkCore::MavlinkCore() : pImpl(std::make_unique<Impl>()) {
    LOGD("MavlinkCore created");
}

MavlinkCore::~MavlinkCore() {
    stopConnection();
    LOGD("MavlinkCore destroyed");
}

bool MavlinkCore::startConnection(const std::string& host, int port) {
    LOGD("Starting connection to %s:%d", host.c_str(), port);
    
    stopConnection(); // Stop any existing connection
    
    pImpl->shouldStop = false;
    
    // For now, just simulate connection
    // In a real implementation, this would start UDP socket connection
    {
        std::lock_guard<std::mutex> lock(pImpl->stateMutex);
        pImpl->vehicleState.connected = true;
        pImpl->updateState(pImpl->vehicleState);
    }
    
    return true;
}

void MavlinkCore::stopConnection() {
    if (pImpl->shouldStop) return;
    
    pImpl->shouldStop = true;
    
    if (pImpl->networkThread.joinable()) {
        pImpl->networkThread.join();
    }
    
    {
        std::lock_guard<std::mutex> lock(pImpl->stateMutex);
        pImpl->vehicleState.connected = false;
        pImpl->updateState(pImpl->vehicleState);
    }
    
    LOGD("Connection stopped");
}

bool MavlinkCore::isConnected() const {
    std::lock_guard<std::mutex> lock(pImpl->stateMutex);
    return pImpl->vehicleState.connected;
}

void MavlinkCore::armDisarm(bool arm) {
    LOGD("Arm/Disarm: %s", arm ? "ARM" : "DISARM");
    pImpl->sendCommand(MAV_CMD_COMPONENT_ARM_DISARM, arm ? 1.0f : 0.0f);
}

void MavlinkCore::returnToLaunch() {
    LOGD("Return to Launch");
    pImpl->sendCommand(MAV_CMD_NAV_RETURN_TO_LAUNCH);
}

void MavlinkCore::takeoff(float altitude) {
    LOGD("Takeoff to altitude: %.1f", altitude);
    pImpl->sendCommand(MAV_CMD_NAV_TAKEOFF, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, altitude);
}

void MavlinkCore::setMode(const std::string& mode) {
    LOGD("Set mode: %s", mode.c_str());
    // Mode setting would require specific MAVLink command implementation
}

const MavlinkCore::VehicleState& MavlinkCore::getVehicleState() const {
    return pImpl->vehicleState;
}

void MavlinkCore::setStateUpdateCallback(StateUpdateCallback callback) {
    pImpl->stateCallback = std::move(callback);
}

void MavlinkCore::processMessage(const uint8_t* data, size_t length) {
    for (size_t i = 0; i < length; ++i) {
        pImpl->processByte(data[i]);
    }
}

void MavlinkCore::Impl::processByte(uint8_t byte) {
    switch (parseState) {
        case WAITING_FOR_STX:
            if (byte == MAV_STX) {
                messageBuffer[0] = byte;
                messageIndex = 1;
                parseState = GOT_STX;
            }
            break;
            
        case GOT_STX:
            expectedLength = byte;
            messageBuffer[messageIndex++] = byte;
            parseState = GOT_LENGTH;
            break;
            
        case GOT_LENGTH:
        case GOT_SEQ:
        case GOT_SYSID:
        case GOT_COMPID:
        case GOT_MSGID:
            messageBuffer[messageIndex++] = byte;
            parseState = static_cast<ParseState>(parseState + 1);
            if (parseState == GOT_PAYLOAD && expectedLength == 0) {
                // No payload, go straight to checksum
                parseState = GOT_CHECKSUM1;
            }
            break;
            
        case GOT_PAYLOAD:
            messageBuffer[messageIndex++] = byte;
            if (messageIndex >= (6 + expectedLength)) { // Header + payload
                parseState = GOT_CHECKSUM1;
            }
            break;
            
        case GOT_CHECKSUM1:
            messageBuffer[messageIndex++] = byte;
            processCompleteMessage();
            parseState = WAITING_FOR_STX;
            messageIndex = 0;
            break;
    }
}

void MavlinkCore::Impl::processCompleteMessage() {
    // Extract message ID
    uint8_t msgId = messageBuffer[5];
    
    // Simple message processing (placeholder implementation)
    switch (msgId) {
        case MAVLINK_MSG_ID_HEARTBEAT: {
            std::lock_guard<std::mutex> lock(stateMutex);
            vehicleState.connected = true;
            vehicleState.systemId = messageBuffer[3];
            vehicleState.componentId = messageBuffer[4];
            updateState(vehicleState);
            break;
        }
        
        // Add more message types as needed
        default:
            break;
    }
}

void MavlinkCore::Impl::updateState(const VehicleState& newState) {
    if (stateCallback) {
        stateCallback(newState);
    }
}

void MavlinkCore::Impl::sendCommand(uint16_t command, float param1, float param2,
                                   float param3, float param4, float param5,
                                   float param6, float param7) {
    // Placeholder for sending MAVLink commands
    // In real implementation, this would create and send COMMAND_LONG message
    LOGD("Sending command %d with params: %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f",
         command, param1, param2, param3, param4, param5, param6, param7);
}

// C-style interface implementation
extern "C" {
    MavlinkCore* mavlink_core_create() {
        return new MavlinkCore();
    }
    
    void mavlink_core_destroy(MavlinkCore* core) {
        delete core;
    }
    
    int mavlink_core_start_connection(MavlinkCore* core, const char* host, int port) {
        if (!core) return 0;
        return core->startConnection(std::string(host), port) ? 1 : 0;
    }
    
    void mavlink_core_stop_connection(MavlinkCore* core) {
        if (core) core->stopConnection();
    }
    
    int mavlink_core_is_connected(MavlinkCore* core) {
        return core ? (core->isConnected() ? 1 : 0) : 0;
    }
    
    void mavlink_core_arm_disarm(MavlinkCore* core, int arm) {
        if (core) core->armDisarm(arm != 0);
    }
    
    void mavlink_core_return_to_launch(MavlinkCore* core) {
        if (core) core->returnToLaunch();
    }
    
    void mavlink_core_takeoff(MavlinkCore* core, float altitude) {
        if (core) core->takeoff(altitude);
    }
    
    void mavlink_core_get_state(MavlinkCore* core, MavlinkCore::VehicleState* state) {
        if (core && state) {
            *state = core->getVehicleState();
        }
    }
}