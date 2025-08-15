#include "TelemetryMessage.hpp"
#include <sstream>
std::string TelemetryMessage::toJson() const {
    std::ostringstream o; o << "{\"seq\":"<<seq<<",\"type\":"<<(int)type<<",\"v1\":"<<v1<<",\"v2\":"<<v2<<",\"v3\":"<<v3<<"}"; return o.str(); }
