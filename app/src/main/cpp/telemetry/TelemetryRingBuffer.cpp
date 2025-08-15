#include "TelemetryRingBuffer.hpp"
TelemetryRingBuffer::TelemetryRingBuffer(size_t capacity): buf_(capacity), cap_(capacity) {}
void TelemetryRingBuffer::push(const TelemetryMessage& msg){ size_t h = head_.fetch_add(1); buf_[h % cap_] = msg; size_t c = count_.load(); if(c<cap_) count_.compare_exchange_strong(c,c+1); }
std::vector<TelemetryMessage> TelemetryRingBuffer::latest(size_t maxCount) const { std::vector<TelemetryMessage> out; size_t c = count_.load(); if(c==0) return out; size_t h = head_.load(); size_t n = std::min({maxCount,c}); out.reserve(n); for(size_t i=0;i<n;++i){ size_t idx = (h - 1 - i) % cap_; out.push_back(buf_[idx]); } return out; }
