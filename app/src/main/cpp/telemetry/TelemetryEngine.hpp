#pragma once
#include <thread>
#include <atomic>
#include <random>
#include "TelemetryRingBuffer.hpp"
#include "StatsCalculator.hpp"
class TelemetryEngine {
public:
    TelemetryEngine();
    ~TelemetryEngine();
    void start();
    void stop();
    std::string statsJson();
    std::string latestBatchJson(int maxCount);
private:
    void run();
    std::thread th_;
    std::atomic<bool> running_{false};
    TelemetryRingBuffer buffer_;
    StatsCalculator stats_;
    std::mt19937 rng_;
    uint32_t seq_{0};
    std::chrono::steady_clock::time_point startTs_;
};
