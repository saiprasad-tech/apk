#include "TelemetryEngine.hpp"
#include "TelemetryMessage.hpp"
#include <chrono>
#include <sstream>
TelemetryEngine::TelemetryEngine(): buffer_(10000), rng_(12345) {}
TelemetryEngine::~TelemetryEngine(){ stop(); }
void TelemetryEngine::start(){ if(running_.exchange(true)) return; startTs_=std::chrono::steady_clock::now(); th_=std::thread(&TelemetryEngine::run,this); }
void TelemetryEngine::stop(){ if(!running_.exchange(false)) return; if(th_.joinable()) th_.join(); }
void TelemetryEngine::run(){ std::uniform_real_distribution<float> dist(-100.f,100.f); while(running_){ TelemetryMessage m; m.seq=++seq_; m.type= static_cast<TelemetryMessage::Type>(m.seq % 4); m.monotonicNs = (uint64_t) std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::steady_clock::now().time_since_epoch()).count(); m.v1=dist(rng_); m.v2=dist(rng_); m.v3=dist(rng_); buffer_.push(m); stats_.record((uint8_t)m.type); std::this_thread::sleep_for(std::chrono::milliseconds(50)); } }
std::string TelemetryEngine::statsJson(){ auto now= std::chrono::steady_clock::now(); double secs = std::chrono::duration<double>(now-startTs_).count(); std::ostringstream o; o << "{\"engine\":{\"running\":"<<(running_?"true":"false")<<"},\"stats\":"<< stats_.json(secs) <<"}"; return o.str(); }
std::string TelemetryEngine::latestBatchJson(int maxCount){ auto list = buffer_.latest(maxCount); std::ostringstream o; o << "["; bool first=true; for(auto &m: list){ if(!first) o << ","; first=false; o<< m.toJson(); } o << "]"; return o.str(); }
