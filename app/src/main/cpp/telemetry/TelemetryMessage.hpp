#pragma once
#include <cstdint>
#include <string>
#include <chrono>
struct TelemetryMessage {
    enum class Type : uint8_t { Heartbeat, Attitude, GPS, Battery } type;
    uint64_t monotonicNs; // timestamp
    uint32_t seq;
    float v1, v2, v3; // generic payload fields
    std::string toJson() const;
};
