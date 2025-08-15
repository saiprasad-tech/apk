#include "StatsCalculator.hpp"
#include <sstream>
void StatsCalculator::record(uint8_t){ total_.fetch_add(1); }
std::string StatsCalculator::json(double intervalSec) const { double rate = intervalSec>0 ? total_.load()/intervalSec : 0.0; std::ostringstream o; o << "{\"total\":"<<total_.load()<<",\"approxRate\":"<<rate <<"}"; return o.str(); }
