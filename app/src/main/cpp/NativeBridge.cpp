#include <jni.h>
#include <string>
#include "telemetry/TelemetryEngine.hpp"
static TelemetryEngine gEngine;
extern "C" JNIEXPORT void JNICALL Java_com_pixhawk_gcslab_NativeBridge_startTelemetry(JNIEnv*, jclass){ gEngine.start(); }
extern "C" JNIEXPORT void JNICALL Java_com_pixhawk_gcslab_NativeBridge_stopTelemetry(JNIEnv*, jclass){ gEngine.stop(); }
extern "C" JNIEXPORT jstring JNICALL Java_com_pixhawk_gcslab_NativeBridge_getStats(JNIEnv* env, jclass){ auto s = gEngine.statsJson(); return env->NewStringUTF(s.c_str()); }
extern "C" JNIEXPORT jstring JNICALL Java_com_pixhawk_gcslab_NativeBridge_getLatestBatch(JNIEnv* env, jclass, jint maxCount){ auto b = gEngine.latestBatchJson(maxCount); return env->NewStringUTF(b.c_str()); }
