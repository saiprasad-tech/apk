#pragma once
#include <vector>
#include <atomic>
#include <mutex>
#include "TelemetryMessage.hpp"
class TelemetryRingBuffer {
public:
    explicit TelemetryRingBuffer(size_t capacity);
    void push(const TelemetryMessage& msg);
    std::vector<TelemetryMessage> latest(size_t maxCount) const;
    size_t size() const { return count_.load(); }
private:
    std::vector<TelemetryMessage> buf_;
    const size_t cap_;
    std::atomic<size_t> head_{0};
    std::atomic<size_t> count_{0};
};
