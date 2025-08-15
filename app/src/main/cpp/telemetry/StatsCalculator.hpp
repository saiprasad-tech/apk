#pragma once
#include <cstdint>
#include <atomic>
#include <string>
class StatsCalculator {
public:
    void record(uint8_t type);
    std::string json(double intervalSec) const;
private:
    std::atomic<uint64_t> total_{0};
};
